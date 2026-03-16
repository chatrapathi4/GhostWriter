package com.ghostwriter.admin;

import com.ghostwriter.chapter.ChapterRepository;
import com.ghostwriter.interaction.LikeRepository;
import com.ghostwriter.interaction.StoryViewRepository;
import com.ghostwriter.story.Story;
import com.ghostwriter.story.StoryRepository;
import com.ghostwriter.user.User;
import com.ghostwriter.user.UserRepository;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class AdminUserService {

    private final UserRepository userRepository;
    private final StoryRepository storyRepository;
    private final ChapterRepository chapterRepository;
    private final LikeRepository likeRepository;
    private final StoryViewRepository viewRepository;

    public AdminUserService(UserRepository userRepository,
            StoryRepository storyRepository,
            ChapterRepository chapterRepository,
            LikeRepository likeRepository,
            StoryViewRepository viewRepository) {
        this.userRepository = userRepository;
        this.storyRepository = storyRepository;
        this.chapterRepository = chapterRepository;
        this.likeRepository = likeRepository;
        this.viewRepository = viewRepository;
    }

    /**
     * Get all users with story statistics.
     */
    public List<Map<String, Object>> getAllUsersWithStats() {
        List<User> users = userRepository.findAll();
        List<Map<String, Object>> result = new ArrayList<>();

        for (User user : users) {
            Map<String, Object> dto = new LinkedHashMap<>();
            dto.put("id", user.getId());
            dto.put("githubId", user.getGithubId());
            dto.put("username", user.getUsername());
            dto.put("email", user.getEmail());
            dto.put("avatarUrl", user.getAvatarUrl());
            dto.put("createdAt", user.getCreatedAt());
            dto.put("storyCount", storyRepository.countByUserId(user.getId()));
            dto.put("publishedCount", storyRepository.countByUserIdAndStatus(user.getId(), "published"));
            result.add(dto);
        }

        return result;
    }

    /**
     * Get all stories for a specific user.
     */
    public List<Story> getUserStories(String userId) {
        return storyRepository.findByUserIdOrderByUpdatedAtDesc(userId);
    }

    /**
     * Delete a user and all their associated data.
     */
    public void deleteUser(String userId) {
        Optional<User> opt = userRepository.findById(userId);
        if (opt.isEmpty()) {
            throw new RuntimeException("User not found");
        }

        // Delete all user's stories and associated data
        List<Story> stories = storyRepository.findByUserIdOrderByUpdatedAtDesc(userId);
        for (Story story : stories) {
            chapterRepository.deleteByStoryId(story.getId());
            likeRepository.deleteByStoryId(story.getId());
            viewRepository.deleteByStoryId(story.getId());
            storyRepository.deleteById(story.getId());
        }

        // Delete the user
        userRepository.deleteById(userId);
    }
}
