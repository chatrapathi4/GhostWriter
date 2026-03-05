package com.ghostwriter.story;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface StoryRepository extends MongoRepository<Story, String> {

    List<Story> findByUserIdOrderByUpdatedAtDesc(String userId);

    List<Story> findByStatusOrderByUpdatedAtDesc(String status);

    List<Story> findByUserIdAndStatusOrderByUpdatedAtDesc(String userId, String status);
}
