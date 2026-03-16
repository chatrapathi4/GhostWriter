(function () {
    'use strict';

    var storyId = null;
    var chapters = [];
    var editingChapterIndex = -1;

    // Get story ID from URL
    var pathParts = window.location.pathname.split('/');
    if (pathParts.length >= 3 && pathParts[1] === 'edit') {
        storyId = pathParts[2];
    }

    if (!storyId) {
        showToast('No story ID found');
        return;
    }

    // ═══════════════════════════════════════
    // Load Story Data
    // ═══════════════════════════════════════
    function loadStory() {
        Promise.all([
            fetch('/api/stories/' + storyId).then(function (r) { return r.json(); }),
            fetch('/api/chapters/story/' + storyId).then(function (r) { return r.json(); }).catch(function () { return []; })
        ]).then(function (results) {
            var story = results[0];
            if (story.error) {
                showToast('Story not found');
                return;
            }

            chapters = (Array.isArray(results[1]) ? results[1] : []).map(function (ch) {
                return { id: ch.id, title: ch.title, content: ch.content, chapterNumber: ch.chapterNumber };
            });

            document.getElementById('editTitle').value = story.title || '';
            document.getElementById('editGenre').value = story.genre || '';
            document.getElementById('editTone').value = story.tone || '';
            document.getElementById('editSummary').value = story.summary || '';
            document.getElementById('editCoverImage').value = story.coverImage || '';

            if (story.coverImage) {
                var preview = document.getElementById('editCoverPreview');
                var previewImg = document.getElementById('editCoverPreviewImg');
                if (preview && previewImg) {
                    previewImg.src = story.coverImage;
                    preview.style.display = 'block';
                }
            }

            renderChapters();
        }).catch(function () {
            showToast('Failed to load story');
        });
    }

    // ═══════════════════════════════════════
    // Render Chapters
    // ═══════════════════════════════════════
    function renderChapters() {
        var container = document.getElementById('chaptersList');

        if (chapters.length === 0) {
            container.innerHTML =
                '<div class="write-chapters-empty">' +
                '<div class="write-chapters-empty-icon">📝</div>' +
                '<div class="write-chapters-empty-text">No chapters yet. Add your first chapter.</div>' +
                '</div>';
            return;
        }

        var html = '';
        chapters.forEach(function (ch, index) {
            var preview = (ch.content || '').substring(0, 80);
            if ((ch.content || '').length > 80) preview += '...';
            var wordCount = (ch.content || '').trim().split(/\s+/).filter(function (w) { return w; }).length;

            html += '<div class="write-chapter-card">';
            html += '<div class="write-chapter-number">' + (index + 1) + '</div>';
            html += '<div class="write-chapter-info">';
            html += '<div class="write-chapter-title">' + esc(ch.title || 'Chapter ' + (index + 1)) + '</div>';
            html += '<div class="write-chapter-preview">' + wordCount + ' words — ' + esc(preview) + '</div>';
            html += '</div>';
            html += '<div class="write-chapter-actions">';
            html += '<button class="write-ch-btn" onclick="window._editChapter(' + index + ')">Edit</button>';
            html += '<button class="write-ch-btn delete" onclick="window._deleteChapter(' + index + ')">Delete</button>';
            html += '</div>';
            html += '</div>';
        });

        container.innerHTML = html;
    }

    // ═══════════════════════════════════════
    // Chapter Editor
    // ═══════════════════════════════════════
    var chapterModal = document.getElementById('chapterEditorModal');
    var chapterClose = document.getElementById('chapterEditorClose');
    var chapterCancel = document.getElementById('chapterEditorCancel');
    var chapterSave = document.getElementById('chapterEditorSave');

    document.getElementById('addChapterBtn').addEventListener('click', function () {
        editingChapterIndex = -1;
        document.getElementById('chapterEditorTitle').textContent = 'Add Chapter';
        document.getElementById('chapterEditTitle').value = 'Chapter ' + (chapters.length + 1);
        document.getElementById('chapterEditContent').value = '';
        chapterModal.classList.add('show');
    });

    window._editChapter = function (index) {
        editingChapterIndex = index;
        var ch = chapters[index];
        document.getElementById('chapterEditorTitle').textContent = 'Edit Chapter ' + (index + 1);
        document.getElementById('chapterEditTitle').value = ch.title || '';
        document.getElementById('chapterEditContent').value = ch.content || '';
        chapterModal.classList.add('show');
    };

    window._deleteChapter = function (index) {
        if (!confirm('Delete this chapter?')) return;
        var ch = chapters[index];

        // If chapter has an ID, delete from server
        if (ch.id) {
            fetch('/api/chapters/' + ch.id, { method: 'DELETE' })
                .then(function () {
                    chapters.splice(index, 1);
                    renderChapters();
                    showToast('Chapter deleted');
                })
                .catch(function () { showToast('Failed to delete chapter'); });
        } else {
            chapters.splice(index, 1);
            renderChapters();
            showToast('Chapter removed');
        }
    };

    if (chapterClose) chapterClose.addEventListener('click', function () { chapterModal.classList.remove('show'); });
    if (chapterCancel) chapterCancel.addEventListener('click', function () { chapterModal.classList.remove('show'); });
    if (chapterModal) chapterModal.addEventListener('click', function (e) {
        if (e.target === chapterModal) chapterModal.classList.remove('show');
    });

    if (chapterSave) {
        chapterSave.addEventListener('click', function () {
            var title = document.getElementById('chapterEditTitle').value.trim();
            var content = document.getElementById('chapterEditContent').value.trim();
            if (!content) {
                showToast('Please write some chapter content');
                return;
            }

            if (editingChapterIndex >= 0) {
                var ch = chapters[editingChapterIndex];
                ch.title = title;
                ch.content = content;

                // Update on server if has ID
                if (ch.id) {
                    fetch('/api/chapters/' + ch.id, {
                        method: 'PUT',
                        headers: { 'Content-Type': 'application/json' },
                        body: JSON.stringify({ title: title, content: content })
                    }).catch(function () { });
                }
            } else {
                // Create new chapter on server
                fetch('/api/chapters', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({
                        storyId: storyId,
                        chapterNumber: chapters.length + 1,
                        title: title || 'Chapter ' + (chapters.length + 1),
                        content: content
                    })
                })
                    .then(function (r) { return r.json(); })
                    .then(function (saved) {
                        chapters.push({ id: saved.id, title: saved.title, content: saved.content, chapterNumber: saved.chapterNumber });
                        renderChapters();
                    })
                    .catch(function () { showToast('Failed to save chapter'); });

                chapterModal.classList.remove('show');
                showToast('Chapter added');
                return;
            }

            chapterModal.classList.remove('show');
            renderChapters();
            showToast('Chapter updated');
        });
    }

    // ═══════════════════════════════════════
    // Cover Image Upload
    // ═══════════════════════════════════════
    var coverFileInput = document.getElementById('editCoverFile');
    if (coverFileInput) {
        coverFileInput.addEventListener('change', function () {
            var file = coverFileInput.files[0];
            if (!file) return;

            var validTypes = ['image/jpeg', 'image/png', 'image/webp'];
            if (validTypes.indexOf(file.type) === -1) {
                showToast('Please select a JPG, PNG, or WebP image');
                coverFileInput.value = '';
                return;
            }

            var formData = new FormData();
            formData.append('file', file);

            fetch('/api/upload/cover', { method: 'POST', body: formData })
                .then(function (r) { return r.json(); })
                .then(function (data) {
                    if (data.error) { showToast(data.error); return; }
                    document.getElementById('editCoverImage').value = data.url;
                    var preview = document.getElementById('editCoverPreview');
                    var previewImg = document.getElementById('editCoverPreviewImg');
                    if (preview && previewImg) {
                        previewImg.src = data.url;
                        preview.style.display = 'block';
                    }
                    showToast('Cover image uploaded');
                })
                .catch(function () { showToast('Failed to upload cover image'); });
        });
    }

    // ═══════════════════════════════════════
    // Save Changes
    // ═══════════════════════════════════════
    document.getElementById('saveMetaBtn').addEventListener('click', function () {
        saveMetadata(false);
    });

    document.getElementById('sendReviewBtn').addEventListener('click', function () {
        saveMetadata(true);
    });

    function saveMetadata(sendToReview) {
        var title = document.getElementById('editTitle').value.trim();
        var genre = document.getElementById('editGenre').value.trim();
        var tone = document.getElementById('editTone').value.trim();
        var summary = document.getElementById('editSummary').value.trim();
        var coverImage = document.getElementById('editCoverImage').value.trim();

        if (!title) { showToast('Please enter a title'); return; }

        // Build combined content for legacy field
        var combinedContent = chapters.map(function (ch) {
            return (ch.title || '') + '\n\n' + (ch.content || '');
        }).join('\n\n---\n\n');

        fetch('/api/stories/' + storyId, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                title: title,
                content: combinedContent,
                genre: genre,
                tone: tone,
                status: 'draft'
            })
        })
            .then(function (r) { return r.json(); })
            .then(function (saved) {
                if (saved.error) { showToast(saved.error); return Promise.reject(new Error(saved.error)); }

                // Update extended fields
                return fetch('/api/stories/' + storyId + '/extended', {
                    method: 'PATCH',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ summary: summary, coverImage: coverImage })
                });
            })
            .then(function () {
                if (sendToReview) {
                    return fetch('/api/moderation/publish/' + storyId, { method: 'POST' })
                        .then(function (r) { return r.json(); })
                        .then(function (result) {
                            if (result.status === 'published') {
                                showToast('Story published!');
                            } else if (result.status === 'rejected') {
                                showToast('Story rejected: ' + (result.rejectionReason || ''));
                            } else {
                                showToast('Story sent for review!');
                            }
                        });
                } else {
                    showToast('Changes saved!');
                }
            })
            .catch(function (err) {
                if (err && err.message) return;
                showToast('Failed to save changes');
            });
    }

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

    document.addEventListener('keydown', function (e) {
        if (e.key === 'Escape' && chapterModal) chapterModal.classList.remove('show');
    });

    // Init
    loadStory();
})();
