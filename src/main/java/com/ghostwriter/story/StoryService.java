package com.ghostwriter.story;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class StoryService {

    private final StoryRepository storyRepository;

    public StoryService(StoryRepository storyRepository) {
        this.storyRepository = storyRepository;
    }

    public Story createStory(String userId, String authorName, String title,
            String content, String genre, String tone, String status) {
        Story story = new Story(userId, authorName, title, content, genre, tone, status);
        return storyRepository.save(story);
    }

    public Story updateStory(String storyId, String userId, String title,
            String content, String genre, String tone, String status) {
        Optional<Story> opt = storyRepository.findById(storyId);
        if (opt.isEmpty()) {
            throw new RuntimeException("Story not found");
        }
        Story story = opt.get();
        if (!story.getUserId().equals(userId)) {
            throw new RuntimeException("Not authorized to edit this story");
        }
        story.setTitle(title);
        story.setContent(content);
        story.setGenre(genre);
        story.setTone(tone);
        story.setStatus(status);
        story.setUpdatedAt(Instant.now());
        return storyRepository.save(story);
    }

    public void deleteStory(String storyId, String userId) {
        Optional<Story> opt = storyRepository.findById(storyId);
        if (opt.isEmpty()) {
            throw new RuntimeException("Story not found");
        }
        Story story = opt.get();
        if (!story.getUserId().equals(userId)) {
            throw new RuntimeException("Not authorized to delete this story");
        }
        storyRepository.delete(story);
    }

    public Optional<Story> getStoryById(String storyId) {
        return storyRepository.findById(storyId);
    }

    public List<Story> getUserStories(String userId) {
        return storyRepository.findByUserIdOrderByUpdatedAtDesc(userId);
    }

    public List<Story> getUserDrafts(String userId) {
        return storyRepository.findByUserIdAndStatusOrderByUpdatedAtDesc(userId, "draft");
    }

    public List<Story> getUserPublished(String userId) {
        return storyRepository.findByUserIdAndStatusOrderByUpdatedAtDesc(userId, "published");
    }

    public List<Story> getAllPublished() {
        return storyRepository.findByStatusOrderByUpdatedAtDesc("published");
    }
}
