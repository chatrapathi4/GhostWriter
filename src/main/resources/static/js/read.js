(function () {
    'use strict';

    // ═══════════════════════════════════════
    // Parse URL
    // ═══════════════════════════════════════
    var pathParts = window.location.pathname.split('/');
    // /read/{storyId}/{chapterNumber}
    var storyId = pathParts[2] || null;
    var chapterNumber = parseInt(pathParts[3]) || 1;
    var totalChapters = 0;
    var storyTitle = '';

    if (!storyId) {
        document.getElementById('readerLoading').innerHTML =
            '<div style="text-align:center;color:#999">Story not found. <a href="/feed" style="color:var(--accent, #a78bfa)">← Back to Stories</a></div>';
        return;
    }

    // Set back link
    document.getElementById('backLink').href = '/story-view/' + storyId;

    // ═══════════════════════════════════════
    // Load Reader Preferences
    // ═══════════════════════════════════════
    var fontSelect = document.getElementById('fontSelect');
    var bgSelect = document.getElementById('bgSelect');

    var savedFont = localStorage.getItem('gw-reader-font') || 'sans-serif';
    var savedBg = localStorage.getItem('gw-reader-bg') || 'dark';

    fontSelect.value = savedFont;
    bgSelect.value = savedBg;

    applySettings();

    fontSelect.addEventListener('change', function () {
        localStorage.setItem('gw-reader-font', fontSelect.value);
        applySettings();
    });

    bgSelect.addEventListener('change', function () {
        localStorage.setItem('gw-reader-bg', bgSelect.value);
        applySettings();
    });

    function applySettings() {
        var body = document.body;

        // Remove old classes
        body.classList.remove('reader-font-serif', 'reader-font-sans-serif', 'reader-font-monospace');
        body.classList.remove('reader-bg-dark', 'reader-bg-sepia', 'reader-bg-light');

        // Apply new
        body.classList.add('reader-font-' + fontSelect.value);
        body.classList.add('reader-bg-' + bgSelect.value);
    }

    // ═══════════════════════════════════════
    // Load Chapter
    // ═══════════════════════════════════════
    function loadChapter() {
        Promise.all([
            fetch('/api/stories/' + storyId).then(function (r) { return r.json(); }),
            fetch('/api/chapters/story/' + storyId).then(function (r) { return r.json(); }).catch(function () { return []; })
        ]).then(function (results) {
            var story = results[0];
            var chapters = Array.isArray(results[1]) ? results[1] : [];

            if (story.error || !story.title) {
                showError('Story not found');
                return;
            }

            storyTitle = story.title;
            totalChapters = chapters.length;

            // Find the chapter by number
            var chapter = null;
            for (var i = 0; i < chapters.length; i++) {
                if (chapters[i].chapterNumber === chapterNumber) {
                    chapter = chapters[i];
                    break;
                }
            }

            if (!chapter) {
                // If chapters exist but number not found, try index-based
                if (chapterNumber <= chapters.length && chapterNumber > 0) {
                    chapter = chapters[chapterNumber - 1];
                }
            }

            if (!chapter) {
                showError('Chapter not found');
                return;
            }

            renderChapter(story, chapter);

        }).catch(function () {
            showError('Failed to load chapter');
        });
    }

    function renderChapter(story, chapter) {
        document.getElementById('readerLoading').style.display = 'none';
        document.getElementById('readerContent').style.display = 'block';

        // Update page title
        document.title = chapter.title + ' — ' + story.title + ' — Ghost Writer';

        // Chapter info
        document.getElementById('chapterInfo').textContent =
            story.title + ' · Chapter ' + chapterNumber + ' of ' + totalChapters;

        // Title and body
        document.getElementById('chapterTitle').textContent =
            chapter.title || 'Chapter ' + chapterNumber;
        document.getElementById('chapterBody').textContent = chapter.content || '';

        // Navigation
        document.getElementById('navInfo').textContent =
            'Chapter ' + chapterNumber + ' of ' + totalChapters;

        if (chapterNumber > 1) {
            var prevLink = document.getElementById('prevChapterLink');
            prevLink.href = '/read/' + storyId + '/' + (chapterNumber - 1);
            prevLink.style.display = 'inline-block';
        }

        if (chapterNumber < totalChapters) {
            var nextLink = document.getElementById('nextChapterLink');
            nextLink.href = '/read/' + storyId + '/' + (chapterNumber + 1);
            nextLink.style.display = 'inline-block';

            // Show next chapter section (Feature 7)
            var nextSection = document.getElementById('nextChapterSection');
            nextSection.style.display = 'block';
            document.getElementById('nextChapterLabel').textContent =
                'Next: Chapter ' + (chapterNumber + 1);

            document.getElementById('nextChapterArrow').addEventListener('click', function () {
                window.location.href = '/read/' + storyId + '/' + (chapterNumber + 1);
            });

            // Feature 7: Auto-load next chapter on scroll to bottom
            setupAutoScroll();
        }
    }

    // ═══════════════════════════════════════
    // Feature 7: Auto Next Chapter Scroll
    // ═══════════════════════════════════════
    function setupAutoScroll() {
        var nextSection = document.getElementById('nextChapterSection');
        if (!nextSection) return;

        var triggered = false;

        var observer = new IntersectionObserver(function (entries) {
            entries.forEach(function (entry) {
                if (entry.isIntersecting && !triggered) {
                    // Make arrow pulse faster to indicate action
                    var arrow = document.getElementById('nextChapterArrow');
                    if (arrow) {
                        arrow.style.animation = 'bounceArrow 0.5s ease-in-out infinite';
                        arrow.style.borderColor = '#50c878';
                        arrow.style.color = '#50c878';
                    }

                    // After a brief delay, navigate to next chapter
                    setTimeout(function () {
                        if (!triggered) {
                            triggered = true;
                            window.location.href = '/read/' + storyId + '/' + (chapterNumber + 1);
                        }
                    }, 1500);
                }
            });
        }, { threshold: 1.0 });

        observer.observe(nextSection);
    }

    function showError(msg) {
        document.getElementById('readerLoading').innerHTML =
            '<div style="text-align:center;color:#999">' + msg +
            '. <a href="/story-view/' + storyId + '" style="color:var(--accent, #a78bfa)">← Back to Story</a></div>';
    }

    // ─── Init ───
    loadChapter();
})();
