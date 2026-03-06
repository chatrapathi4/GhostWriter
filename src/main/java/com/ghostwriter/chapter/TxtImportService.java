package com.ghostwriter.chapter;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class TxtImportService {

    private static final Pattern CHAPTER_MARKER = Pattern.compile(
            "(?i)^\\s*(chapter\\s+\\d+[.:;\\-—]*\\s*.*?)$",
            Pattern.MULTILINE);

    private static final int MIN_WORDS_PER_CHAPTER = 1000;
    private static final int MAX_WORDS_PER_CHAPTER = 1500;

    /**
     * Parse raw text into chapters.
     * Strategy 1: Detect "Chapter X" markers
     * Strategy 2: Split by word count (1000-1500 words per chapter)
     */
    public List<Chapter> parseTextIntoChapters(String text) {
        if (text == null || text.isBlank()) {
            return new ArrayList<>();
        }

        // Strategy 1: Try to detect chapter markers
        List<Chapter> chapters = splitByChapterMarkers(text);
        if (chapters.size() > 1) {
            return chapters;
        }

        // Strategy 2: Split by word count
        return splitByWordCount(text);
    }

    private List<Chapter> splitByChapterMarkers(String text) {
        List<Chapter> chapters = new ArrayList<>();
        Matcher matcher = CHAPTER_MARKER.matcher(text);

        List<Integer> markerPositions = new ArrayList<>();
        List<String> markerTitles = new ArrayList<>();

        while (matcher.find()) {
            markerPositions.add(matcher.start());
            markerTitles.add(matcher.group(1).trim());
        }

        if (markerPositions.isEmpty()) {
            return chapters;
        }

        for (int i = 0; i < markerPositions.size(); i++) {
            int start = markerPositions.get(i);
            int end = (i + 1 < markerPositions.size())
                    ? markerPositions.get(i + 1)
                    : text.length();

            String chapterContent = text.substring(start, end).trim();
            // Remove the chapter title line from content
            String title = markerTitles.get(i);
            String content = chapterContent;
            int titleEnd = chapterContent.indexOf('\n');
            if (titleEnd > 0) {
                content = chapterContent.substring(titleEnd).trim();
            }

            Chapter chapter = new Chapter();
            chapter.setChapterNumber(i + 1);
            chapter.setTitle(title);
            chapter.setContent(content);
            chapters.add(chapter);
        }

        return chapters;
    }

    private List<Chapter> splitByWordCount(String text) {
        List<Chapter> chapters = new ArrayList<>();
        String[] words = text.trim().split("\\s+");

        if (words.length <= MAX_WORDS_PER_CHAPTER) {
            Chapter chapter = new Chapter();
            chapter.setChapterNumber(1);
            chapter.setTitle("Chapter 1");
            chapter.setContent(text.trim());
            chapters.add(chapter);
            return chapters;
        }

        int chapterNum = 1;
        int wordIndex = 0;

        while (wordIndex < words.length) {
            int targetEnd = Math.min(wordIndex + MAX_WORDS_PER_CHAPTER, words.length);

            // Try to find a paragraph/sentence break near the target
            int breakPoint = targetEnd;
            if (targetEnd < words.length) {
                // Look for sentence-ending punctuation near the target
                for (int j = targetEnd; j >= wordIndex + MIN_WORDS_PER_CHAPTER; j--) {
                    if (words[j - 1].matches(".*[.!?]$")) {
                        breakPoint = j;
                        break;
                    }
                }
            }

            StringBuilder sb = new StringBuilder();
            for (int j = wordIndex; j < breakPoint; j++) {
                if (j > wordIndex)
                    sb.append(' ');
                sb.append(words[j]);
            }

            Chapter chapter = new Chapter();
            chapter.setChapterNumber(chapterNum);
            chapter.setTitle("Chapter " + chapterNum);
            chapter.setContent(sb.toString());
            chapters.add(chapter);

            chapterNum++;
            wordIndex = breakPoint;
        }

        return chapters;
    }
}
