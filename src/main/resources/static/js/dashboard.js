(function () {
    'use strict';

    var currentTab = 'all';
    var allStories = [];

    // ═══════════════════════════════════════
    // Load Stories
    // ═══════════════════════════════════════
    function loadStories() {
        fetch('/api/stories/mine')
            .then(function (resp) { return resp.json(); })
            .then(function (data) {
                if (Array.isArray(data)) {
                    allStories = data;
                    renderStories();
                }
            })
            .catch(function () {
                showToast('Failed to load stories');
            });
    }

    // ═══════════════════════════════════════
    // Render Stories
    // ═══════════════════════════════════════
    function renderStories() {
        var container = document.getElementById('storiesGrid');
        if (!container) return;

        var filtered = allStories;
        if (currentTab === 'drafts') {
            filtered = allStories.filter(function (s) { return s.status === 'draft'; });
        } else if (currentTab === 'published') {
            filtered = allStories.filter(function (s) { return s.status === 'published'; });
        } else if (currentTab === 'pending') {
            filtered = allStories.filter(function (s) { return s.status === 'pending_review'; });
        } else if (currentTab === 'rejected') {
            filtered = allStories.filter(function (s) { return s.status === 'rejected'; });
        }

        if (filtered.length === 0) {
            var emptyMsg = currentTab === 'drafts' ? 'No drafts yet' :
                currentTab === 'published' ? 'No published stories yet' :
                    currentTab === 'pending' ? 'No stories pending review' :
                        currentTab === 'rejected' ? 'No rejected stories' :
                            'No stories yet. Create your first one!';
            container.innerHTML =
                '<div class="empty-state">' +
                '<div class="empty-state-icon">📝</div>' +
                '<div class="empty-state-title">' + emptyMsg + '</div>' +
                '<div class="empty-state-text">Start writing to see your stories here.</div>' +
                '</div>';
            return;
        }

        var html = '';
        filtered.forEach(function (story) {
            var preview = (story.content || '').substring(0, 150);
            if ((story.content || '').length > 150) preview += '...';
            var date = story.updatedAt ? new Date(story.updatedAt).toLocaleDateString() : '';

            var statusClass, statusLabel;
            switch (story.status) {
                case 'published':
                    statusClass = 'status-published';
                    statusLabel = 'Published';
                    break;
                case 'pending_review':
                    statusClass = 'status-pending';
                    statusLabel = 'Pending Review';
                    break;
                case 'rejected':
                    statusClass = 'status-rejected';
                    statusLabel = 'Rejected';
                    break;
                default:
                    statusClass = 'status-draft';
                    statusLabel = 'Draft';
            }

            html +=
                '<div class="story-card" data-id="' + story.id + '">' +
                '<div class="story-card-title">' + esc(story.title || 'Untitled') + '</div>' +
                '<div class="story-card-meta">' +
                (story.genre ? '<span class="story-card-badge genre">' + esc(story.genre) + '</span>' : '') +
                '<span class="story-card-badge ' + statusClass + '">' + statusLabel + '</span>' +
                '</div>' +
                '<div class="story-card-preview">' + esc(preview) + '</div>' +
                '<div class="story-card-footer">' +
                '<span class="story-card-date">' + date + '</span>' +
                '<div class="story-card-actions">' +
                '<button class="story-action-btn edit-btn" data-id="' + story.id + '">Edit</button>' +
                (story.status === 'draft' || story.status === 'rejected' ?
                    '<button class="story-action-btn publish publish-btn" data-id="' + story.id + '">Send to Review</button>' : '') +
                (story.status === 'published' ?
                    '<button class="story-action-btn unpublish-btn" data-id="' + story.id + '">Unpublish</button>' : '') +
                '<button class="story-action-btn delete delete-btn" data-id="' + story.id + '">Delete</button>' +
                '</div>' +
                '</div>' +
                '</div>';
        });

        container.innerHTML = html;
        bindCardActions();
    }

    // ═══════════════════════════════════════
    // Card Action Handlers
    // ═══════════════════════════════════════
    function bindCardActions() {
        document.querySelectorAll('.edit-btn').forEach(function (btn) {
            btn.addEventListener('click', function (e) {
                e.stopPropagation();
                var id = btn.getAttribute('data-id');
                // Navigate to the chapter-based edit page
                window.location.href = '/edit/' + id;
            });
        });

        document.querySelectorAll('.publish-btn').forEach(function (btn) {
            btn.addEventListener('click', function (e) {
                e.stopPropagation();
                var id = btn.getAttribute('data-id');
                sendToReview(id);
            });
        });

        document.querySelectorAll('.unpublish-btn').forEach(function (btn) {
            btn.addEventListener('click', function (e) {
                e.stopPropagation();
                var id = btn.getAttribute('data-id');
                updateStoryStatus(id, 'draft');
            });
        });

        document.querySelectorAll('.delete-btn').forEach(function (btn) {
            btn.addEventListener('click', function (e) {
                e.stopPropagation();
                var id = btn.getAttribute('data-id');
                if (confirm('Are you sure you want to delete this story?')) {
                    deleteStory(id);
                }
            });
        });
    }

    function sendToReview(storyId) {
        fetch('/api/moderation/publish/' + storyId, { method: 'POST' })
            .then(function (resp) { return resp.json(); })
            .then(function (result) {
                if (result.error) {
                    showToast(result.error);
                } else if (result.status === 'published') {
                    showToast('Story published!');
                } else if (result.status === 'rejected') {
                    showToast('Story rejected: ' + (result.rejectionReason || 'Content flagged'));
                } else {
                    showToast('Story sent for review!');
                }
                loadStories();
            })
            .catch(function () {
                showToast('Failed to send story for review');
            });
    }

    function updateStoryStatus(storyId, newStatus) {
        var story = allStories.find(function (s) { return s.id === storyId; });
        if (!story) return;

        fetch('/api/stories/' + storyId, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                title: story.title,
                content: story.content,
                genre: story.genre,
                tone: story.tone,
                status: newStatus
            })
        })
            .then(function (resp) { return resp.json(); })
            .then(function () {
                showToast('Story moved to drafts');
                loadStories();
            })
            .catch(function () {
                showToast('Failed to update story');
            });
    }

    function deleteStory(storyId) {
        fetch('/api/stories/' + storyId, { method: 'DELETE' })
            .then(function (resp) { return resp.json(); })
            .then(function () {
                showToast('Story deleted');
                loadStories();
            })
            .catch(function () {
                showToast('Failed to delete story');
            });
    }

    // ═══════════════════════════════════════
    // Tabs
    // ═══════════════════════════════════════
    document.querySelectorAll('.tab-btn').forEach(function (tab) {
        tab.addEventListener('click', function () {
            document.querySelectorAll('.tab-btn').forEach(function (t) { t.classList.remove('active'); });
            tab.classList.add('active');
            currentTab = tab.getAttribute('data-tab');
            renderStories();
        });
    });

    // ═══════════════════════════════════════
    // Toast
    // ═══════════════════════════════════════
    function showToast(msg) {
        var toast = document.getElementById('toast');
        var toastMsg = document.getElementById('toastMsg');
        if (!toast || !toastMsg) return;
        toastMsg.textContent = msg;
        toast.classList.add('show');
        setTimeout(function () { toast.classList.remove('show'); }, 3000);
    }

    function esc(text) {
        var d = document.createElement('div');
        d.textContent = text;
        return d.innerHTML;
    }

    // ─── Init ───
    loadStories();
})();
