package com.ghostwriter.interaction;

import com.ghostwriter.story.Story;
import com.ghostwriter.story.StoryRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class LikeService {

    private final LikeRepository likeRepository;
    private final StoryRepository storyRepository;

    public LikeService(LikeRepository likeRepository, StoryRepository storyRepository) {
        this.likeRepository = likeRepository;
        this.storyRepository = storyRepository;
    }

    /**
     * Toggle like: if already liked, unlike; otherwise, like.
     * Returns true if liked, false if unliked.
     */
    public boolean toggleLike(String storyId, String userId) {
        Optional<Like> existing = likeRepository.findByStoryIdAndUserId(storyId, userId);
        Optional<Story> storyOpt = storyRepository.findById(storyId);

        if (existing.isPresent()) {
            likeRepository.delete(existing.get());
            storyOpt.ifPresent(story -> {
                story.setLikeCount(Math.max(0, story.getLikeCount() - 1));
                storyRepository.save(story);
            });
            return false;
        } else {
            likeRepository.save(new Like(storyId, userId));
            storyOpt.ifPresent(story -> {
                story.setLikeCount(story.getLikeCount() + 1);
                storyRepository.save(story);
            });
            return true;
        }
    }

    public long getLikeCount(String storyId) {
        return likeRepository.countByStoryId(storyId);
    }

    public boolean hasUserLiked(String storyId, String userId) {
        return likeRepository.existsByStoryIdAndUserId(storyId, userId);
    }
}
