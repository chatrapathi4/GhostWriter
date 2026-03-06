package com.ghostwriter.interaction;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface StoryViewRepository extends MongoRepository<StoryView, String> {

    long countByStoryId(String storyId);

    boolean existsByStoryIdAndUserId(String storyId, String userId);

    void deleteByStoryId(String storyId);
}
