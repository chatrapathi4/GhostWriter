(function () {
    'use strict';

    // ═══════════════════════════════════════
    // Load Published Stories for Visual Feed
    // ═══════════════════════════════════════
    function loadFeed() {
        var container = document.getElementById('feedGrid');
        if (!container) return;

        fetch('/api/stories/published')
            .then(function (resp) { return resp.json(); })
            .then(function (stories) {
                if (!Array.isArray(stories) || stories.length === 0) {
                    container.innerHTML =
                        '<div class="empty-state">' +
                        '<div class="empty-state-icon">📚</div>' +
                        '<div class="empty-state-title">No stories published yet</div>' +
                        '<div class="empty-state-text">Be the first to share your story!</div>' +
                        '</div>';
                    return;
                }

                var html = '';
                stories.forEach(function (story) {
                    var coverHtml;
                    if (story.coverImage) {
                        coverHtml = '<img class="feed-card-cover" src="' + esc(story.coverImage) + '" alt="' + esc(story.title || '') + '">';
                    } else {
                        // Generate a consistent gradient placeholder with genre emoji
                        var emoji = getGenreEmoji(story.genre);
                        coverHtml = '<div class="feed-card-cover-placeholder">' + emoji + '</div>';
                    }

                    html += '<a href="/story-view/' + story.id + '" class="feed-card">';
                    html += coverHtml;
                    html += '<div class="feed-card-body">';
                    html += '<div class="feed-card-title">' + esc(story.title || 'Untitled') + '</div>';
                    html += '<div class="feed-card-badges">';
                    if (story.genre) html += '<span class="story-card-badge genre">' + esc(story.genre) + '</span>';
                    if (story.tone) html += '<span class="story-card-badge" style="background:rgba(100,210,255,0.15);color:var(--teal)">' + esc(story.tone) + '</span>';
                    html += '</div>';
                    html += '<div class="feed-card-stats">';
                    html += '<span class="feed-card-stat">👁️ ' + (story.viewCount || 0) + '</span>';
                    html += '<span class="feed-card-stat">❤️ ' + (story.likeCount || 0) + '</span>';
                    html += '</div>';
                    html += '<div class="feed-card-author">✍️ ' + esc(story.authorName || 'Anonymous') + '</div>';
                    html += '</div>';
                    html += '</a>';
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

    function getGenreEmoji(genre) {
        if (!genre) return '📖';
        var g = genre.toLowerCase();
        if (g.includes('fantasy')) return '🧙';
        if (g.includes('sci-fi') || g.includes('science')) return '🚀';
        if (g.includes('horror')) return '👻';
        if (g.includes('romance')) return '💕';
        if (g.includes('mystery') || g.includes('thriller')) return '🔍';
        if (g.includes('adventure')) return '⚔️';
        if (g.includes('comedy') || g.includes('humor')) return '😂';
        if (g.includes('drama')) return '🎭';
        if (g.includes('historical')) return '🏛️';
        return '📖';
    }

    function esc(text) {
        var d = document.createElement('div');
        d.textContent = text;
        return d.innerHTML;
    }

    loadFeed();
})();
