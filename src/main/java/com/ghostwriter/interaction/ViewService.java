package com.ghostwriter.interaction;

import com.ghostwriter.story.Story;
import com.ghostwriter.story.StoryRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class ViewService {

    private final StoryViewRepository viewRepository;
    private final StoryRepository storyRepository;

    public ViewService(StoryViewRepository viewRepository, StoryRepository storyRepository) {
        this.viewRepository = viewRepository;
        this.storyRepository = storyRepository;
    }

    /**
     * Record a view. Only one view per user per story.
     */
    public void recordView(String storyId, String userId) {
        if (userId != null && !userId.isBlank()) {
            if (viewRepository.existsByStoryIdAndUserId(storyId, userId)) {
                return; // Already viewed
            }
            viewRepository.save(new StoryView(storyId, userId));
        } else {
            // Anonymous view — record with null userId
            viewRepository.save(new StoryView(storyId, null));
        }

        Optional<Story> storyOpt = storyRepository.findById(storyId);
        storyOpt.ifPresent(story -> {
            story.setViewCount(story.getViewCount() + 1);
            storyRepository.save(story);
        });
    }

    public long getViewCount(String storyId) {
        return viewRepository.countByStoryId(storyId);
    }
}
