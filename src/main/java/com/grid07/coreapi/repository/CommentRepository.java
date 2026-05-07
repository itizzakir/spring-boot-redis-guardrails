package com.grid07.coreapi.repository;

import com.grid07.coreapi.model.Comment;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    List<Comment> findByPost_IdOrderByCreatedAtAsc(Long postId);
}
