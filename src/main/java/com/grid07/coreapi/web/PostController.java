package com.grid07.coreapi.web;

import com.grid07.coreapi.dto.CommentResponse;
import com.grid07.coreapi.dto.CreateCommentRequest;
import com.grid07.coreapi.dto.CreatePostRequest;
import com.grid07.coreapi.dto.LikePostRequest;
import com.grid07.coreapi.dto.LikeResponse;
import com.grid07.coreapi.dto.PostResponse;
import com.grid07.coreapi.service.PostService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/posts")
public class PostController {

    private final PostService postService;

    public PostController(PostService postService) {
        this.postService = postService;
    }

    @PostMapping
    public ResponseEntity<PostResponse> createPost(@Valid @RequestBody CreatePostRequest request) {
        PostResponse post = postService.createPost(request);
        return ResponseEntity.created(URI.create("/api/posts/" + post.id())).body(post);
    }

    @GetMapping("/{postId}")
    public PostResponse getPost(@PathVariable Long postId) {
        return postService.getPost(postId);
    }

    @GetMapping("/{postId}/comments")
    public List<CommentResponse> getComments(@PathVariable Long postId) {
        return postService.getComments(postId);
    }

    @PostMapping("/{postId}/comments")
    public ResponseEntity<CommentResponse> addComment(
            @PathVariable Long postId,
            @Valid @RequestBody CreateCommentRequest request
    ) {
        CommentResponse comment = postService.addComment(postId, request);
        return ResponseEntity.created(URI.create("/api/posts/" + postId + "/comments/" + comment.id())).body(comment);
    }

    @PostMapping("/{postId}/like")
    public LikeResponse likePost(@PathVariable Long postId, @Valid @RequestBody LikePostRequest request) {
        return postService.likePost(postId, request);
    }

    @GetMapping("/{postId}/virality")
    public Map<String, Long> getVirality(@PathVariable Long postId) {
        return Map.of("postId", postId, "viralityScore", postService.getViralityScore(postId));
    }
}
