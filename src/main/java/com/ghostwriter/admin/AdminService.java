package com.ghostwriter.admin;

import com.ghostwriter.chapter.ChapterRepository;
import com.ghostwriter.interaction.LikeRepository;
import com.ghostwriter.interaction.StoryViewRepository;
import com.ghostwriter.story.Story;
import com.ghostwriter.story.StoryRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class AdminService {

    private final StoryRepository storyRepository;
    private final ChapterRepository chapterRepository;
    private final LikeRepository likeRepository;
    private final StoryViewRepository viewRepository;

    public AdminService(StoryRepository storyRepository,
            ChapterRepository chapterRepository,
            LikeRepository likeRepository,
            StoryViewRepository viewRepository) {
        this.storyRepository = storyRepository;
        this.chapterRepository = chapterRepository;
        this.likeRepository = likeRepository;
        this.viewRepository = viewRepository;
    }

    public List<Story> getPendingStories() {
        return storyRepository.findByStatusOrderByUpdatedAtDesc("pending_review");
    }

    public List<Story> getPublishedStories() {
        return storyRepository.findByStatusOrderByUpdatedAtDesc("published");
    }

    public List<Story> getRejectedStories() {
        return storyRepository.findByStatusOrderByUpdatedAtDesc("rejected");
    }

    public Story approveStory(String storyId) {
        Optional<Story> opt = storyRepository.findById(storyId);
        if (opt.isEmpty()) {
            throw new RuntimeException("Story not found");
        }
        Story story = opt.get();
        story.setStatus("published");
        story.setRejectionReason(null);
        story.setUpdatedAt(Instant.now());
        return storyRepository.save(story);
    }

    public Story rejectStory(String storyId, String reason) {
        Optional<Story> opt = storyRepository.findById(storyId);
        if (opt.isEmpty()) {
            throw new RuntimeException("Story not found");
        }
        Story story = opt.get();
        story.setStatus("rejected");
        story.setRejectionReason(reason);
        story.setUpdatedAt(Instant.now());
        return storyRepository.save(story);
    }

    public void deleteStory(String storyId) {
        Optional<Story> opt = storyRepository.findById(storyId);
        if (opt.isEmpty()) {
            throw new RuntimeException("Story not found");
        }
        // Delete associated data
        chapterRepository.deleteByStoryId(storyId);
        likeRepository.deleteByStoryId(storyId);
        viewRepository.deleteByStoryId(storyId);
        storyRepository.deleteById(storyId);
    }
}
