(function () {
    'use strict';

    // ═══════════════════════════════════════
    // Check Admin Access
    // ═══════════════════════════════════════
    fetch('/api/auth/status')
        .then(function (resp) { return resp.json(); })
        .then(function (data) {
            if (data.authenticated && data.isAdmin) {
                document.getElementById('usersSection').style.display = 'block';
                document.getElementById('accessDenied').style.display = 'none';
                loadUsers();
            } else {
                document.getElementById('usersSection').style.display = 'none';
                document.getElementById('accessDenied').style.display = 'block';
            }
        })
        .catch(function () {
            document.getElementById('accessDenied').style.display = 'block';
        });

    // ═══════════════════════════════════════
    // Load Users
    // ═══════════════════════════════════════
    function loadUsers() {
        fetch('/api/admin/users')
            .then(function (r) { return r.json(); })
            .then(function (users) {
                if (!Array.isArray(users)) {
                    showToast('Failed to load users');
                    return;
                }

                var totalStories = 0;
                var totalPublished = 0;
                users.forEach(function (u) {
                    totalStories += (u.storyCount || 0);
                    totalPublished += (u.publishedCount || 0);
                });

                document.getElementById('totalUsers').textContent = users.length;
                document.getElementById('totalStories').textContent = totalStories;
                document.getElementById('totalPublished').textContent = totalPublished;

                renderUsers(users);
            })
            .catch(function () {
                showToast('Failed to load users');
            });
    }

    // ═══════════════════════════════════════
    // Render Users
    // ═══════════════════════════════════════
    function renderUsers(users) {
        var container = document.getElementById('usersList');

        if (users.length === 0) {
            container.innerHTML =
                '<div class="empty-state">' +
                '<div class="empty-state-icon">👥</div>' +
                '<div class="empty-state-title">No users found</div>' +
                '</div>';
            return;
        }

        var html = '';
        users.forEach(function (user) {
            var createdDate = user.createdAt ? new Date(user.createdAt).toLocaleDateString('en-US', {
                year: 'numeric', month: 'short', day: 'numeric'
            }) : 'N/A';

            html += '<div class="admin-story-card">';
            html += '<div class="admin-story-top">';
            html += '<div class="admin-story-info">';
            html += '<div class="admin-story-title" style="display:flex;align-items:center;gap:10px">';
            if (user.avatarUrl) {
                html += '<img src="' + esc(user.avatarUrl) + '" alt="avatar" style="width:32px;height:32px;border-radius:50%">';
            }
            html += esc(user.username || 'Unknown');
            html += '</div>';
            html += '<div class="admin-story-meta" style="margin-top:6px">';
            if (user.email) {
                html += '<span class="story-card-badge" style="background:rgba(100,210,255,0.15);color:var(--teal)">📧 ' + esc(user.email) + '</span>';
            }
            html += '<span class="story-card-badge genre">📖 ' + (user.storyCount || 0) + ' stories</span>';
            html += '<span class="story-card-badge" style="background:rgba(80,200,120,0.15);color:#50c878">✅ ' + (user.publishedCount || 0) + ' published</span>';
            html += '<span class="story-card-date">Joined: ' + createdDate + '</span>';
            html += '</div>';
            html += '</div>';
            html += '<div class="admin-story-actions">';
            html += '<button class="admin-action-btn approve" data-id="' + user.id + '" onclick="window._viewUserStories(this)">📖 View Stories</button>';
            html += '<button class="admin-action-btn delete" data-id="' + user.id + '" data-name="' + esc(user.username || '') + '" onclick="window._deleteUser(this)">🗑️ Delete</button>';
            html += '</div>';
            html += '</div>';
            html += '</div>';
        });

        container.innerHTML = html;
    }

    // ═══════════════════════════════════════
    // View User Stories
    // ═══════════════════════════════════════
    window._viewUserStories = function (btn) {
        var userId = btn.getAttribute('data-id');
        var modal = document.getElementById('userStoriesModal');
        var body = document.getElementById('userStoriesBody');

        document.getElementById('userStoriesTitle').textContent = 'User Stories';
        body.innerHTML = '<div class="empty-state"><div class="spinner"></div><div class="empty-state-text">Loading...</div></div>';
        modal.classList.add('show');

        fetch('/api/admin/users/' + userId + '/stories')
            .then(function (r) { return r.json(); })
            .then(function (stories) {
                if (!Array.isArray(stories) || stories.length === 0) {
                    body.innerHTML = '<div class="empty-state"><div class="empty-state-icon">📭</div><div class="empty-state-title">No stories</div></div>';
                    return;
                }

                var html = '';
                stories.forEach(function (story) {
                    var statusClass = story.status === 'published' ? 'background:rgba(80,200,120,0.15);color:#50c878' :
                        story.status === 'pending_review' ? 'background:rgba(255,180,50,0.15);color:#ffb432' :
                            story.status === 'rejected' ? 'background:rgba(255,80,80,0.15);color:#ff5050' :
                                'background:rgba(150,150,180,0.15);color:var(--text-secondary)';
                    var statusLabel = story.status === 'pending_review' ? 'Pending Review' :
                        story.status.charAt(0).toUpperCase() + story.status.slice(1);
                    var date = story.updatedAt ? new Date(story.updatedAt).toLocaleDateString() : '';

                    html += '<div style="padding:12px 0;border-bottom:1px solid var(--border)">';
                    html += '<div style="font-weight:600;color:var(--text-primary)">' + esc(story.title || 'Untitled') + '</div>';
                    html += '<div style="margin-top:4px;display:flex;gap:8px;align-items:center;flex-wrap:wrap">';
                    if (story.genre) html += '<span class="story-card-badge genre">' + esc(story.genre) + '</span>';
                    html += '<span class="story-card-badge" style="' + statusClass + '">' + statusLabel + '</span>';
                    html += '<span class="story-card-date">' + date + '</span>';
                    html += '</div>';
                    html += '</div>';
                });

                body.innerHTML = html;
            })
            .catch(function () {
                body.innerHTML = '<div class="empty-state"><div class="empty-state-title">Failed to load stories</div></div>';
            });
    };

    // ═══════════════════════════════════════
    // Delete User
    // ═══════════════════════════════════════
    window._deleteUser = function (btn) {
        var userId = btn.getAttribute('data-id');
        var userName = btn.getAttribute('data-name');
        if (!confirm('Are you sure you want to delete user "' + userName + '" and all their stories?')) return;

        fetch('/api/admin/users/' + userId, { method: 'DELETE' })
            .then(function (r) { return r.json(); })
            .then(function (data) {
                if (data.error) {
                    showToast(data.error);
                } else {
                    showToast('User deleted');
                    loadUsers();
                }
            })
            .catch(function () { showToast('Failed to delete user'); });
    };

    // ═══════════════════════════════════════
    // User Stories Modal
    // ═══════════════════════════════════════
    var storiesModal = document.getElementById('userStoriesModal');
    var storiesClose = document.getElementById('userStoriesClose');
    var storiesCloseBtn = document.getElementById('userStoriesCloseBtn');

    if (storiesClose) storiesClose.addEventListener('click', function () { storiesModal.classList.remove('show'); });
    if (storiesCloseBtn) storiesCloseBtn.addEventListener('click', function () { storiesModal.classList.remove('show'); });
    if (storiesModal) storiesModal.addEventListener('click', function (e) {
        if (e.target === storiesModal) storiesModal.classList.remove('show');
    });

    document.addEventListener('keydown', function (e) {
        if (e.key === 'Escape' && storiesModal) storiesModal.classList.remove('show');
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
