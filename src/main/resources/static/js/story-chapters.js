(function () {
    'use strict';

    var container = document.getElementById('storyPageContent');
    if (!container) return;

    var pathParts = window.location.pathname.split('/');
    var storyId = pathParts[pathParts.length - 1];
    var allChapters = [];
    var currentChapterIndex = 0;

    if (!storyId) {
        container.innerHTML = '<div class="empty-state"><div class="empty-state-title">Story not found</div></div>';
        return;
    }

    // Load story data and chapters in parallel
    Promise.all([
        fetch('/api/stories/' + storyId).then(function (r) {
            if (!r.ok) throw new Error('Not found');
            return r.json();
        }),
        fetch('/api/chapters/story/' + storyId).then(function (r) { return r.json(); }).catch(function () { return []; }),
        fetch('/api/stories/' + storyId + '/interactions').then(function (r) { return r.json(); }).catch(function () { return {}; })
    ]).then(function (results) {
        var story = results[0];
        allChapters = Array.isArray(results[1]) ? results[1] : [];
        var interactions = results[2] || {};

        // Record view
        fetch('/api/stories/' + storyId + '/view', { method: 'POST' }).catch(function () { });

        renderStoryPage(story, allChapters, interactions);
    }).catch(function () {
        container.innerHTML =
            '<div class="empty-state">' +
            '<div class="empty-state-icon">😔</div>' +
            '<div class="empty-state-title">Story not found</div>' +
            '<div class="empty-state-text">This story may have been removed or does not exist.</div>' +
            '<a href="/feed" class="story-back-link" style="margin-top:16px">← Back to Stories</a>' +
            '</div>';
    });

    function renderStoryPage(story, chapters, interactions) {
        var date = story.updatedAt ? new Date(story.updatedAt).toLocaleDateString('en-US', {
            year: 'numeric', month: 'long', day: 'numeric'
        }) : '';

        var html = '';
        html += '<a href="/feed" class="story-back-link">← Back to Stories</a>';

        // Hero Cover
        html += '<div class="story-hero">';
        if (story.coverImage) {
            html += '<img class="story-hero-img" src="' + esc(story.coverImage) + '" alt="' + esc(story.title || '') + '">';
        } else {
            html += '<div class="story-hero-placeholder">📖</div>';
        }
        html += '</div>';

        // Header
        html += '<div class="story-page-header">';
        html += '<h1 class="story-page-title">' + esc(story.title || 'Untitled') + '</h1>';
        html += '<div class="story-page-meta">';
        html += '<span class="story-page-author">✍️ ' + esc(story.authorName || 'Anonymous') + '</span>';
        if (story.genre) html += '<span class="story-card-badge genre">' + esc(story.genre) + '</span>';
        if (story.tone) html += '<span class="story-card-badge" style="background:rgba(100,210,255,0.15);color:var(--teal)">' + esc(story.tone) + '</span>';
        html += '<span class="story-card-date">' + date + '</span>';
        html += '</div>';

        // Interactions
        html += '<div class="story-page-interactions">';
        var likedClass = interactions.userLiked ? ' liked' : '';
        html += '<button class="story-interaction-btn' + likedClass + '" id="likeBtn" onclick="window._toggleLike()">';
        html += '<span id="likeIcon">' + (interactions.userLiked ? '❤️' : '🤍') + '</span>';
        html += '<span id="likeCount">' + (interactions.likeCount || 0) + '</span>';
        html += '</button>';
        html += '<span class="story-interaction-count">👁️ <span id="viewCount">' + (interactions.viewCount || 0) + '</span> views</span>';
        html += '</div>';
        html += '</div>';

        // Summary
        if (story.summary) {
            html += '<div class="story-page-summary">';
            html += '<div class="story-page-summary-title">Summary</div>';
            html += '<div class="story-page-summary-text">' + esc(story.summary) + '</div>';
            html += '</div>';
        }

        // Chapters
        if (chapters.length > 0) {
            html += '<div class="story-chapters-section">';
            html += '<div class="story-chapters-title">📋 Chapters (' + chapters.length + ')</div>';
            chapters.forEach(function (ch, index) {
                html += '<div class="chapter-list-item" data-index="' + index + '" onclick="window._openChapter(' + index + ')">';
                html += '<div class="chapter-number">' + ch.chapterNumber + '</div>';
                html += '<div class="chapter-title">' + esc(ch.title || 'Chapter ' + ch.chapterNumber) + '</div>';
                html += '<div class="chapter-arrow">→</div>';
                html += '</div>';
            });
            html += '</div>';
        } else if (story.content) {
            // Legacy content (no chapters)
            html += '<div class="story-legacy-content">' + esc(story.content) + '</div>';
        }

        container.innerHTML = html;
    }

    // ═══════════════════════════════════════
    // Chapter Reader
    // ═══════════════════════════════════════
    window._openChapter = function (index) {
        currentChapterIndex = index;
        var ch = allChapters[index];
        if (!ch) return;

        document.getElementById('chapterReaderTitle').textContent =
            'Chapter ' + ch.chapterNumber + ': ' + (ch.title || 'Untitled');
        document.getElementById('chapterReaderBody').textContent = ch.content || '';
        document.getElementById('chapterNavInfo').textContent =
            'Chapter ' + (index + 1) + ' of ' + allChapters.length;

        document.getElementById('prevChapterBtn').style.display = index > 0 ? 'inline-block' : 'none';
        document.getElementById('nextChapterBtn').style.display = index < allChapters.length - 1 ? 'inline-block' : 'none';

        document.getElementById('chapterReader').classList.add('show');
    };

    // Navigation
    var prevBtn = document.getElementById('prevChapterBtn');
    var nextBtn = document.getElementById('nextChapterBtn');
    var readerClose = document.getElementById('chapterReaderClose');
    var readerOverlay = document.getElementById('chapterReader');

    if (prevBtn) prevBtn.addEventListener('click', function () {
        if (currentChapterIndex > 0) window._openChapter(currentChapterIndex - 1);
    });
    if (nextBtn) nextBtn.addEventListener('click', function () {
        if (currentChapterIndex < allChapters.length - 1) window._openChapter(currentChapterIndex + 1);
    });
    if (readerClose) readerClose.addEventListener('click', function () {
        readerOverlay.classList.remove('show');
    });
    if (readerOverlay) readerOverlay.addEventListener('click', function (e) {
        if (e.target === readerOverlay) readerOverlay.classList.remove('show');
    });

    // ═══════════════════════════════════════
    // Like Toggle
    // ═══════════════════════════════════════
    window._toggleLike = function () {
        fetch('/api/stories/' + storyId + '/like', { method: 'POST' })
            .then(function (r) { return r.json(); })
            .then(function (data) {
                if (data.error) return;
                var btn = document.getElementById('likeBtn');
                var icon = document.getElementById('likeIcon');
                var count = document.getElementById('likeCount');
                if (data.liked) {
                    btn.classList.add('liked');
                    icon.textContent = '❤️';
                } else {
                    btn.classList.remove('liked');
                    icon.textContent = '🤍';
                }
                count.textContent = data.likeCount;
            })
            .catch(function () { });
    };

    function esc(text) {
        var d = document.createElement('div');
        d.textContent = text;
        return d.innerHTML;
    }
})();
