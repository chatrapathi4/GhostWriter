package com.ghostwriter.chapter;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ChapterRepository extends MongoRepository<Chapter, String> {

    List<Chapter> findByStoryIdOrderByChapterNumberAsc(String storyId);

    void deleteByStoryId(String storyId);

    long countByStoryId(String storyId);

    java.util.Optional<Chapter> findByStoryIdAndChapterNumber(String storyId, int chapterNumber);
}
