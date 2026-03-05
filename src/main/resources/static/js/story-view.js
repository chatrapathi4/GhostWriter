(function () {
    'use strict';

    // ═══════════════════════════════════════
    // Load Single Story
    // ═══════════════════════════════════════
    var container = document.getElementById('storyContent');
    if (!container) return;

    // Extract story ID from URL: /story/{id}
    var pathParts = window.location.pathname.split('/');
    var storyId = pathParts[pathParts.length - 1];

    if (!storyId) {
        container.innerHTML = '<div class="empty-state"><div class="empty-state-title">Story not found</div></div>';
        return;
    }

    fetch('/api/stories/' + storyId)
        .then(function (resp) {
            if (!resp.ok) throw new Error('Not found');
            return resp.json();
        })
        .then(function (story) {
            var date = story.updatedAt ? new Date(story.updatedAt).toLocaleDateString('en-US', {
                year: 'numeric', month: 'long', day: 'numeric'
            }) : '';

            var html =
                '<div class="story-read">' +
                '<a href="/stories" class="story-back-link">← Back to Stories</a>' +
                '<div class="story-read-header">' +
                '<h1 class="story-read-title">' + esc(story.title || 'Untitled') + '</h1>' +
                '<div class="story-read-meta">' +
                '<span class="story-read-author">✍️ ' + esc(story.authorName || 'Anonymous') + '</span>' +
                (story.genre ? '<span class="story-card-badge genre">' + esc(story.genre) + '</span>' : '') +
                (story.tone ? '<span class="story-card-badge" style="background:rgba(100,210,255,0.15);color:var(--teal)">' + esc(story.tone) + '</span>' : '') +
                '<span class="story-card-date">' + date + '</span>' +
                '</div>' +
                '</div>' +
                '<div class="story-read-content">' + esc(story.content || '') + '</div>' +
                '</div>';

            container.innerHTML = html;
        })
        .catch(function () {
            container.innerHTML =
                '<div class="empty-state">' +
                '<div class="empty-state-icon">😔</div>' +
                '<div class="empty-state-title">Story not found</div>' +
                '<div class="empty-state-text">This story may have been removed or does not exist.</div>' +
                '<a href="/stories" class="story-back-link" style="margin-top:16px">← Back to Stories</a>' +
                '</div>';
        });

    function esc(text) {
        var d = document.createElement('div');
        d.textContent = text;
        return d.innerHTML;
    }
})();
