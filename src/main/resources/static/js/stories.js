(function () {
    'use strict';

    // ═══════════════════════════════════════
    // Load Published Stories
    // ═══════════════════════════════════════
    function loadPublishedStories() {
        var container = document.getElementById('publicStoriesGrid');
        if (!container) return;

        container.innerHTML =
            '<div class="empty-state">' +
            '<div class="spinner"></div>' +
            '<div class="empty-state-text">Loading stories...</div>' +
            '</div>';

        fetch('/api/stories/published')
            .then(function (resp) { return resp.json(); })
            .then(function (stories) {
                if (!Array.isArray(stories) || stories.length === 0) {
                    container.innerHTML =
                        '<div class="empty-state">' +
                        '<div class="empty-state-icon">📚</div>' +
                        '<div class="empty-state-title">No stories published yet</div>' +
                        '<div class="empty-state-text">Be the first to share your story with the community!</div>' +
                        '</div>';
                    return;
                }

                var html = '';
                stories.forEach(function (story) {
                    var preview = (story.content || '').substring(0, 150);
                    if ((story.content || '').length > 150) preview += '...';
                    var date = story.updatedAt ? new Date(story.updatedAt).toLocaleDateString() : '';

                    html +=
                        '<a href="/story/' + story.id + '" class="story-card" style="text-decoration:none">' +
                        '<div class="story-card-title">' + esc(story.title || 'Untitled') + '</div>' +
                        '<div class="story-card-meta">' +
                        (story.genre ? '<span class="story-card-badge genre">' + esc(story.genre) + '</span>' : '') +
                        '</div>' +
                        '<div class="story-card-preview">' + esc(preview) + '</div>' +
                        '<div class="story-card-footer">' +
                        '<span class="story-card-author">' +
                        '✍️ ' + esc(story.authorName || 'Anonymous') +
                        '</span>' +
                        '<span class="story-card-date">' + date + '</span>' +
                        '</div>' +
                        '</a>';
                });

                container.innerHTML = html;
            })
            .catch(function () {
                container.innerHTML =
                    '<div class="empty-state">' +
                    '<div class="empty-state-icon">⚠️</div>' +
                    '<div class="empty-state-title">Failed to load stories</div>' +
                    '</div>';
            });
    }

    function esc(text) {
        var d = document.createElement('div');
        d.textContent = text;
        return d.innerHTML;
    }

    // ─── Init ───
    loadPublishedStories();
})();
