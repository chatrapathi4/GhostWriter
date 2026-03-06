package com.ghostwriter.chapter;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class ChapterService {

    private final ChapterRepository chapterRepository;

    public ChapterService(ChapterRepository chapterRepository) {
        this.chapterRepository = chapterRepository;
    }

    public Chapter createChapter(String storyId, int chapterNumber, String title, String content) {
        Chapter chapter = new Chapter(storyId, chapterNumber, title, content);
        return chapterRepository.save(chapter);
    }

    public Chapter updateChapter(String chapterId, String title, String content) {
        Optional<Chapter> opt = chapterRepository.findById(chapterId);
        if (opt.isEmpty()) {
            throw new RuntimeException("Chapter not found");
        }
        Chapter chapter = opt.get();
        chapter.setTitle(title);
        chapter.setContent(content);
        chapter.setUpdatedAt(Instant.now());
        return chapterRepository.save(chapter);
    }

    public void deleteChapter(String chapterId) {
        chapterRepository.deleteById(chapterId);
    }

    public void deleteAllByStoryId(String storyId) {
        chapterRepository.deleteByStoryId(storyId);
    }

    public List<Chapter> getChaptersByStoryId(String storyId) {
        return chapterRepository.findByStoryIdOrderByChapterNumberAsc(storyId);
    }

    public Optional<Chapter> getChapterById(String chapterId) {
        return chapterRepository.findById(chapterId);
    }

    public long getChapterCount(String storyId) {
        return chapterRepository.countByStoryId(storyId);
    }

    /**
     * Save a batch of chapters for a story, replacing all existing ones.
     */
    public List<Chapter> saveAllChapters(String storyId, List<Chapter> chapters) {
        chapterRepository.deleteByStoryId(storyId);
        for (int i = 0; i < chapters.size(); i++) {
            Chapter ch = chapters.get(i);
            ch.setStoryId(storyId);
            ch.setChapterNumber(i + 1);
            ch.setCreatedAt(Instant.now());
            ch.setUpdatedAt(Instant.now());
        }
        return chapterRepository.saveAll(chapters);
    }
}
