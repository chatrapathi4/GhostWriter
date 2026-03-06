package com.ghostwriter.moderation;

import com.ghostwriter.story.Story;
import com.ghostwriter.story.StoryRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;

@Service
public class PublishingService {

    private final StoryRepository storyRepository;
    private final ModerationService moderationService;

    public PublishingService(StoryRepository storyRepository, ModerationService moderationService) {
        this.storyRepository = storyRepository;
        this.moderationService = moderationService;
    }

    /**
     * Submit a story for publishing.
     * Sets status to pending_review, runs moderation, then approves or rejects.
     */
    public Story submitForPublishing(String storyId, String userId) {
        Optional<Story> opt = storyRepository.findById(storyId);
        if (opt.isEmpty()) {
            throw new RuntimeException("Story not found");
        }
        Story story = opt.get();
        if (!story.getUserId().equals(userId)) {
            throw new RuntimeException("Not authorized to publish this story");
        }

        // Set to pending review
        story.setStatus("pending_review");
        story.setRejectionReason(null);
        story.setUpdatedAt(Instant.now());
        storyRepository.save(story);

        // Run moderation check
        String contentToCheck = (story.getTitle() != null ? story.getTitle() : "")
                + " " + (story.getContent() != null ? story.getContent() : "")
                + " " + (story.getSummary() != null ? story.getSummary() : "");

        ModerationResult result = moderationService.moderate(contentToCheck);

        if (result.isApproved()) {
            story.setStatus("published");
        } else {
            story.setStatus("rejected");
            story.setRejectionReason(result.getReason());
        }
        story.setUpdatedAt(Instant.now());
        return storyRepository.save(story);
    }
}
