package com.grid07.coreapi.repository;

import com.grid07.coreapi.model.PostLike;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostLikeRepository extends JpaRepository<PostLike, Long> {

    boolean existsByPost_IdAndUser_Id(Long postId, Long userId);
}
