package com.ghostwriter.interaction;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface LikeRepository extends MongoRepository<Like, String> {

    Optional<Like> findByStoryIdAndUserId(String storyId, String userId);

    long countByStoryId(String storyId);

    boolean existsByStoryIdAndUserId(String storyId, String userId);

    void deleteByStoryId(String storyId);
}
