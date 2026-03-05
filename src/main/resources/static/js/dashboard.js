(function () {
    'use strict';

    var currentTab = 'all';
    var allStories = [];
    var editingStoryId = null;

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
        }

        if (filtered.length === 0) {
            var emptyMsg = currentTab === 'drafts' ? 'No drafts yet' :
                currentTab === 'published' ? 'No published stories yet' :
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
            var statusClass = story.status === 'published' ? 'status-published' : 'status-draft';
            var statusLabel = story.status === 'published' ? 'Published' : 'Draft';

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
                (story.status === 'draft' ?
                    '<button class="story-action-btn publish publish-btn" data-id="' + story.id + '">Publish</button>' :
                    '<button class="story-action-btn unpublish-btn" data-id="' + story.id + '">Unpublish</button>') +
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
                openEditModal(id);
            });
        });

        document.querySelectorAll('.publish-btn').forEach(function (btn) {
            btn.addEventListener('click', function (e) {
                e.stopPropagation();
                var id = btn.getAttribute('data-id');
                updateStoryStatus(id, 'published');
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

    function openEditModal(storyId) {
        var story = allStories.find(function (s) { return s.id === storyId; });
        if (!story) return;

        editingStoryId = storyId;
        document.getElementById('modalTitle').textContent = 'Edit Story';
        document.getElementById('storyTitle').value = story.title || '';
        document.getElementById('storyGenre').value = story.genre || '';
        document.getElementById('storyTone').value = story.tone || '';
        document.getElementById('storyContent').value = story.content || '';
        document.getElementById('storyModal').classList.add('show');
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
                showToast(newStatus === 'published' ? 'Story published!' : 'Story moved to drafts');
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
    // Create / Edit Modal
    // ═══════════════════════════════════════
    var createBtn = document.getElementById('createStoryBtn');
    var modal = document.getElementById('storyModal');
    var modalClose = document.getElementById('modalCloseBtn');

    if (createBtn) {
        createBtn.addEventListener('click', function () {
            editingStoryId = null;
            document.getElementById('modalTitle').textContent = 'Create New Story';
            document.getElementById('storyTitle').value = '';
            document.getElementById('storyGenre').value = '';
            document.getElementById('storyTone').value = '';
            document.getElementById('storyContent').value = '';
            modal.classList.add('show');
        });
    }

    if (modalClose) {
        modalClose.addEventListener('click', function () {
            modal.classList.remove('show');
        });
    }

    if (modal) {
        modal.addEventListener('click', function (e) {
            if (e.target === modal) modal.classList.remove('show');
        });
    }

    // Save as Draft
    var saveDraftBtn = document.getElementById('saveDraftBtn');
    if (saveDraftBtn) {
        saveDraftBtn.addEventListener('click', function () {
            saveStory('draft');
        });
    }

    // Save & Publish
    var publishBtn = document.getElementById('publishStoryBtn');
    if (publishBtn) {
        publishBtn.addEventListener('click', function () {
            saveStory('published');
        });
    }

    function saveStory(status) {
        var title = document.getElementById('storyTitle').value.trim();
        var genre = document.getElementById('storyGenre').value.trim();
        var tone = document.getElementById('storyTone').value.trim();
        var content = document.getElementById('storyContent').value.trim();

        if (!title) {
            showToast('Please enter a title');
            return;
        }
        if (!content) {
            showToast('Please write some content');
            return;
        }

        var payload = {
            title: title,
            content: content,
            genre: genre,
            tone: tone,
            status: status
        };

        var url = editingStoryId ? '/api/stories/' + editingStoryId : '/api/stories';
        var method = editingStoryId ? 'PUT' : 'POST';

        fetch(url, {
            method: method,
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        })
            .then(function (resp) { return resp.json(); })
            .then(function (data) {
                if (data.error) {
                    showToast(data.error);
                } else {
                    showToast(editingStoryId ? 'Story updated!' : 'Story created!');
                    modal.classList.remove('show');
                    loadStories();
                }
            })
            .catch(function () {
                showToast('Failed to save story');
            });
    }

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

    // Escape key closes modal
    document.addEventListener('keydown', function (e) {
        if (e.key === 'Escape' && modal) {
            modal.classList.remove('show');
        }
    });

    // ─── Init ───
    loadStories();
})();
