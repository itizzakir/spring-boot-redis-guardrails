package com.grid07.coreapi.service;

import com.grid07.coreapi.dto.CommentResponse;
import com.grid07.coreapi.dto.CreateCommentRequest;
import com.grid07.coreapi.dto.CreatePostRequest;
import com.grid07.coreapi.dto.LikePostRequest;
import com.grid07.coreapi.dto.LikeResponse;
import com.grid07.coreapi.dto.PostResponse;
import com.grid07.coreapi.exception.BadRequestException;
import com.grid07.coreapi.exception.DuplicateActionException;
import com.grid07.coreapi.exception.GuardrailViolationException;
import com.grid07.coreapi.exception.ResourceNotFoundException;
import com.grid07.coreapi.model.ActorType;
import com.grid07.coreapi.model.AppUser;
import com.grid07.coreapi.model.Bot;
import com.grid07.coreapi.model.Comment;
import com.grid07.coreapi.model.Post;
import com.grid07.coreapi.model.PostLike;
import com.grid07.coreapi.repository.BotRepository;
import com.grid07.coreapi.repository.CommentRepository;
import com.grid07.coreapi.repository.PostLikeRepository;
import com.grid07.coreapi.repository.PostRepository;
import com.grid07.coreapi.repository.UserRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PostService {

    private static final int MAX_COMMENT_DEPTH = 20;

    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final PostLikeRepository postLikeRepository;
    private final UserRepository userRepository;
    private final BotRepository botRepository;
    private final ViralityService viralityService;
    private final BotGuardrailService botGuardrailService;
    private final NotificationService notificationService;

    public PostService(
            PostRepository postRepository,
            CommentRepository commentRepository,
            PostLikeRepository postLikeRepository,
            UserRepository userRepository,
            BotRepository botRepository,
            ViralityService viralityService,
            BotGuardrailService botGuardrailService,
            NotificationService notificationService
    ) {
        this.postRepository = postRepository;
        this.commentRepository = commentRepository;
        this.postLikeRepository = postLikeRepository;
        this.userRepository = userRepository;
        this.botRepository = botRepository;
        this.viralityService = viralityService;
        this.botGuardrailService = botGuardrailService;
        this.notificationService = notificationService;
    }

    @Transactional
    public PostResponse createPost(CreatePostRequest request) {
        ensureActorExists(request.authorType(), request.authorId());
        Post post = postRepository.saveAndFlush(new Post(request.authorType(), request.authorId(), request.content()));
        return PostResponse.from(post, viralityService.getScore(post.getId()));
    }

    @Transactional(readOnly = true)
    public PostResponse getPost(Long postId) {
        Post post = findPost(postId);
        return PostResponse.from(post, viralityService.getScore(postId));
    }

    @Transactional(readOnly = true)
    public List<CommentResponse> getComments(Long postId) {
        if (!postRepository.existsById(postId)) {
            throw new ResourceNotFoundException("Post not found: " + postId);
        }
        return commentRepository.findByPost_IdOrderByCreatedAtAsc(postId)
                .stream()
                .map(CommentResponse::from)
                .toList();
    }

    @Transactional
    public CommentResponse addComment(Long postId, CreateCommentRequest request) {
        Post post = findPost(postId);
        Comment parent = findParentComment(postId, request.parentCommentId());
        int depthLevel = parent == null ? 1 : parent.getDepthLevel() + 1;

        if (depthLevel > MAX_COMMENT_DEPTH) {
            throw new GuardrailViolationException("Vertical cap reached: comment threads cannot be deeper than 20 levels");
        }

        if (request.authorType() == ActorType.BOT) {
            return addBotComment(post, parent, request, depthLevel);
        }

        AppUser user = findUser(request.authorId());
        Comment comment = new Comment(post, parent, ActorType.USER, user.getId(), request.content(), depthLevel);
        Comment saved = commentRepository.saveAndFlush(comment);
        viralityService.incrementScore(postId, InteractionType.HUMAN_COMMENT);
        return CommentResponse.from(saved);
    }

    @Transactional
    public LikeResponse likePost(Long postId, LikePostRequest request) {
        Post post = findPost(postId);
        AppUser user = findUser(request.userId());

        if (postLikeRepository.existsByPost_IdAndUser_Id(postId, user.getId())) {
            throw new DuplicateActionException("User already liked this post");
        }

        postLikeRepository.saveAndFlush(new PostLike(post, user));
        long score = viralityService.incrementScore(postId, InteractionType.HUMAN_LIKE);
        return new LikeResponse(postId, user.getId(), score, "Post liked");
    }

    public long getViralityScore(Long postId) {
        if (!postRepository.existsById(postId)) {
            throw new ResourceNotFoundException("Post not found: " + postId);
        }
        return viralityService.getScore(postId);
    }

    private CommentResponse addBotComment(Post post, Comment parent, CreateCommentRequest request, int depthLevel) {
        Bot bot = findBot(request.authorId());
        Long humanId = humanTargetForCooldown(post, parent);
        BotReplyLock lock = botGuardrailService.reserveBotReply(post.getId(), bot.getId(), humanId);

        try {
            Comment comment = new Comment(post, parent, ActorType.BOT, bot.getId(), request.content(), depthLevel);
            Comment saved = commentRepository.saveAndFlush(comment);
            viralityService.incrementScore(post.getId(), InteractionType.BOT_REPLY);

            if (post.getAuthorType() == ActorType.USER) {
                notificationService.handleBotInteractionOnUserPost(post.getAuthorId(), bot.getName());
            }

            return CommentResponse.from(saved);
        } catch (RuntimeException ex) {
            botGuardrailService.release(lock);
            throw ex;
        }
    }

    private Long humanTargetForCooldown(Post post, Comment parent) {
        if (parent != null && parent.getAuthorType() == ActorType.USER) {
            return parent.getAuthorId();
        }
        if (post.getAuthorType() == ActorType.USER) {
            return post.getAuthorId();
        }
        return null;
    }

    private Comment findParentComment(Long postId, Long parentCommentId) {
        if (parentCommentId == null) {
            return null;
        }

        Comment parent = commentRepository.findById(parentCommentId)
                .orElseThrow(() -> new ResourceNotFoundException("Parent comment not found: " + parentCommentId));
        if (!parent.getPost().getId().equals(postId)) {
            throw new BadRequestException("Parent comment does not belong to post " + postId);
        }
        return parent;
    }

    private void ensureActorExists(ActorType actorType, Long actorId) {
        if (actorType == ActorType.USER) {
            findUser(actorId);
            return;
        }
        findBot(actorId);
    }

    private Post findPost(Long postId) {
        return postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found: " + postId));
    }

    private AppUser findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
    }

    private Bot findBot(Long botId) {
        return botRepository.findById(botId)
                .orElseThrow(() -> new ResourceNotFoundException("Bot not found: " + botId));
    }
}
