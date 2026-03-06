(function () {
    'use strict';

    var currentTab = 'pending';
    var pendingStories = [];
    var publishedStories = [];
    var rejectedStories = [];
    var rejectingStoryId = null;

    // ═══════════════════════════════════════
    // Check Admin Access
    // ═══════════════════════════════════════
    fetch('/api/auth/status')
        .then(function (resp) { return resp.json(); })
        .then(function (data) {
            if (data.authenticated && data.isAdmin) {
                document.getElementById('adminDashboard').style.display = 'block';
                document.getElementById('accessDenied').style.display = 'none';
                loadAllStories();
            } else {
                document.getElementById('adminDashboard').style.display = 'none';
                document.getElementById('accessDenied').style.display = 'block';
            }
        })
        .catch(function () {
            document.getElementById('accessDenied').style.display = 'block';
        });

    // ═══════════════════════════════════════
    // Load Stories
    // ═══════════════════════════════════════
    function loadAllStories() {
        Promise.all([
            fetch('/api/admin/stories/pending').then(function (r) { return r.json(); }),
            fetch('/api/admin/stories/published').then(function (r) { return r.json(); }),
            fetch('/api/admin/stories/rejected').then(function (r) { return r.json(); })
        ]).then(function (results) {
            pendingStories = Array.isArray(results[0]) ? results[0] : [];
            publishedStories = Array.isArray(results[1]) ? results[1] : [];
            rejectedStories = Array.isArray(results[2]) ? results[2] : [];

            document.getElementById('pendingCount').textContent = pendingStories.length;
            document.getElementById('publishedCount').textContent = publishedStories.length;
            document.getElementById('rejectedCount').textContent = rejectedStories.length;

            renderStories();
        }).catch(function () {
            showToast('Failed to load stories');
        });
    }

    // ═══════════════════════════════════════
    // Render Stories
    // ═══════════════════════════════════════
    function renderStories() {
        var container = document.getElementById('adminStoriesList');
        var stories = currentTab === 'pending' ? pendingStories
            : currentTab === 'published' ? publishedStories
                : rejectedStories;

        if (stories.length === 0) {
            var msg = currentTab === 'pending' ? 'No stories pending review'
                : currentTab === 'published' ? 'No published stories'
                    : 'No rejected stories';
            container.innerHTML =
                '<div class="empty-state">' +
                '<div class="empty-state-icon">📭</div>' +
                '<div class="empty-state-title">' + msg + '</div>' +
                '</div>';
            return;
        }

        var html = '';
        stories.forEach(function (story) {
            var preview = (story.content || story.summary || '').substring(0, 200);
            if (preview.length >= 200) preview += '...';
            var date = story.updatedAt ? new Date(story.updatedAt).toLocaleDateString() : '';

            html += '<div class="admin-story-card">';
            html += '<div class="admin-story-top">';
            html += '<div class="admin-story-info">';
            html += '<div class="admin-story-title">' + esc(story.title || 'Untitled') + '</div>';
            html += '<div class="admin-story-meta">';
            if (story.genre) html += '<span class="story-card-badge genre">' + esc(story.genre) + '</span>';
            if (story.tone) html += '<span class="story-card-badge" style="background:rgba(100,210,255,0.15);color:var(--teal)">' + esc(story.tone) + '</span>';
            html += '<span class="story-card-date">' + date + '</span>';
            html += '</div>';
            html += '<div class="admin-story-preview">' + esc(preview) + '</div>';
            html += '<div class="admin-story-author">✍️ ' + esc(story.authorName || 'Anonymous') + '</div>';
            html += '</div>';

            html += '<div class="admin-story-actions">';
            if (currentTab === 'pending') {
                html += '<button class="admin-action-btn approve" data-id="' + story.id + '" onclick="window._adminApprove(this)">✅ Approve</button>';
                html += '<button class="admin-action-btn reject" data-id="' + story.id + '" onclick="window._adminReject(this)">❌ Reject</button>';
            }
            if (currentTab === 'rejected') {
                html += '<button class="admin-action-btn approve" data-id="' + story.id + '" onclick="window._adminApprove(this)">✅ Approve</button>';
            }
            html += '<button class="admin-action-btn delete" data-id="' + story.id + '" onclick="window._adminDelete(this)">🗑️ Delete</button>';
            html += '</div>';

            html += '</div>';

            if (story.rejectionReason && currentTab === 'rejected') {
                html += '<div class="admin-story-rejection"><strong>Reason:</strong> ' + esc(story.rejectionReason) + '</div>';
            }

            html += '</div>';
        });

        container.innerHTML = html;
    }

    // ═══════════════════════════════════════
    // Actions
    // ═══════════════════════════════════════
    window._adminApprove = function (btn) {
        var id = btn.getAttribute('data-id');
        fetch('/api/admin/stories/' + id + '/approve', { method: 'POST' })
            .then(function (r) { return r.json(); })
            .then(function () {
                showToast('Story approved!');
                loadAllStories();
            })
            .catch(function () { showToast('Failed to approve story'); });
    };

    window._adminReject = function (btn) {
        rejectingStoryId = btn.getAttribute('data-id');
        document.getElementById('rejectReason').value = '';
        document.getElementById('rejectModal').classList.add('show');
    };

    window._adminDelete = function (btn) {
        var id = btn.getAttribute('data-id');
        if (confirm('Are you sure you want to permanently delete this story?')) {
            fetch('/api/admin/stories/' + id, { method: 'DELETE' })
                .then(function (r) { return r.json(); })
                .then(function () {
                    showToast('Story deleted');
                    loadAllStories();
                })
                .catch(function () { showToast('Failed to delete story'); });
        }
    };

    // Reject Modal
    var rejectModal = document.getElementById('rejectModal');
    var rejectClose = document.getElementById('rejectModalClose');
    var rejectCancel = document.getElementById('rejectCancelBtn');
    var rejectConfirm = document.getElementById('rejectConfirmBtn');

    if (rejectClose) {
        rejectClose.addEventListener('click', function () { rejectModal.classList.remove('show'); });
    }
    if (rejectCancel) {
        rejectCancel.addEventListener('click', function () { rejectModal.classList.remove('show'); });
    }
    if (rejectModal) {
        rejectModal.addEventListener('click', function (e) {
            if (e.target === rejectModal) rejectModal.classList.remove('show');
        });
    }
    if (rejectConfirm) {
        rejectConfirm.addEventListener('click', function () {
            var reason = document.getElementById('rejectReason').value.trim();
            if (!reason) {
                showToast('Please provide a rejection reason');
                return;
            }
            fetch('/api/admin/stories/' + rejectingStoryId + '/reject', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ reason: reason })
            })
                .then(function (r) { return r.json(); })
                .then(function () {
                    showToast('Story rejected');
                    rejectModal.classList.remove('show');
                    loadAllStories();
                })
                .catch(function () { showToast('Failed to reject story'); });
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
    // Helpers
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
})();
