(function () {
    'use strict';

    var chapters = [];
    var editingChapterIndex = -1;
    var storyId = null;
    var isEditMode = false;

    // Check if editing an existing story
    var pathParts = window.location.pathname.split('/');
    if (pathParts.length >= 3 && pathParts[1] === 'write' && pathParts[2]) {
        storyId = pathParts[2];
        isEditMode = true;
    }

    // ═══════════════════════════════════════
    // Init
    // ═══════════════════════════════════════
    if (isEditMode) {
        document.getElementById('writePageTitle').textContent = '✏️ Edit Story';
        loadExistingStory();
    } else {
        renderChapters();
    }

    function loadExistingStory() {
        Promise.all([
            fetch('/api/stories/' + storyId).then(function (r) { return r.json(); }),
            fetch('/api/chapters/story/' + storyId).then(function (r) { return r.json(); }).catch(function () { return []; })
        ]).then(function (results) {
            var story = results[0];
            chapters = (Array.isArray(results[1]) ? results[1] : []).map(function (ch) {
                return { title: ch.title, content: ch.content, id: ch.id };
            });

            document.getElementById('writeTitle').value = story.title || '';
            document.getElementById('writeGenre').value = story.genre || '';
            document.getElementById('writeTone').value = story.tone || '';
            document.getElementById('writeSummary').value = story.summary || '';
            document.getElementById('writeCoverImage').value = story.coverImage || '';

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
                '<div class="write-chapters-empty-text">No chapters yet. Add your first chapter or import a TXT file.</div>' +
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
        if (confirm('Delete this chapter?')) {
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
                chapters[editingChapterIndex].title = title;
                chapters[editingChapterIndex].content = content;
            } else {
                chapters.push({ title: title || 'Chapter ' + (chapters.length + 1), content: content });
            }

            chapterModal.classList.remove('show');
            renderChapters();
            showToast(editingChapterIndex >= 0 ? 'Chapter updated' : 'Chapter added');
        });
    }

    // ═══════════════════════════════════════
    // TXT Import
    // ═══════════════════════════════════════
    var importModal = document.getElementById('importModal');
    var importClose = document.getElementById('importModalClose');
    var importCancel = document.getElementById('importCancel');
    var importConfirm = document.getElementById('importConfirm');

    document.getElementById('importTxtBtn').addEventListener('click', function () {
        document.getElementById('importFile').value = '';
        importModal.classList.add('show');
    });

    if (importClose) importClose.addEventListener('click', function () { importModal.classList.remove('show'); });
    if (importCancel) importCancel.addEventListener('click', function () { importModal.classList.remove('show'); });
    if (importModal) importModal.addEventListener('click', function (e) {
        if (e.target === importModal) importModal.classList.remove('show');
    });

    if (importConfirm) {
        importConfirm.addEventListener('click', function () {
            var fileInput = document.getElementById('importFile');
            var file = fileInput.files[0];
            if (!file) {
                showToast('Please select a TXT file');
                return;
            }

            var reader = new FileReader();
            reader.onload = function (e) {
                var text = e.target.result;
                // Preview split via API
                fetch('/api/import/txt/preview', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ text: text })
                })
                    .then(function (r) { return r.json(); })
                    .then(function (data) {
                        if (data.chapters && data.chapters.length > 0) {
                            chapters = data.chapters.map(function (ch) {
                                return { title: ch.title, content: ch.content };
                            });
                            renderChapters();
                            importModal.classList.remove('show');
                            showToast('Imported ' + data.chapterCount + ' chapters! You can edit titles.');
                        } else {
                            showToast('No chapters detected in file');
                        }
                    })
                    .catch(function () { showToast('Failed to parse file'); });
            };
            reader.readAsText(file);
        });
    }

    // ═══════════════════════════════════════
    // Save / Publish
    // ═══════════════════════════════════════
    document.getElementById('saveDraftBtn').addEventListener('click', function () {
        saveStory('draft');
    });

    document.getElementById('publishBtn').addEventListener('click', function () {
        saveStory('publish');
    });

    function saveStory(action) {
        var title = document.getElementById('writeTitle').value.trim();
        var genre = document.getElementById('writeGenre').value.trim();
        var tone = document.getElementById('writeTone').value.trim();
        var summary = document.getElementById('writeSummary').value.trim();
        var coverImage = document.getElementById('writeCoverImage').value.trim();

        if (!title) {
            showToast('Please enter a story title');
            return;
        }
        if (chapters.length === 0) {
            showToast('Please add at least one chapter');
            return;
        }

        // Build combined content for legacy compatibility
        var combinedContent = chapters.map(function (ch) {
            return ch.title + '\n\n' + ch.content;
        }).join('\n\n---\n\n');

        var storyPayload = {
            title: title,
            content: combinedContent,
            genre: genre,
            tone: tone,
            status: 'draft'
        };

        var url = isEditMode ? '/api/stories/' + storyId : '/api/stories';
        var method = isEditMode ? 'PUT' : 'POST';

        fetch(url, {
            method: method,
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(storyPayload)
        })
            .then(function (r) { return r.json(); })
            .then(function (savedStory) {
                if (savedStory.error) {
                    showToast(savedStory.error);
                    return Promise.reject(new Error(savedStory.error));
                }

                var sid = savedStory.id;
                storyId = sid;
                isEditMode = true;

                // Update extended fields via PATCH
                return fetch('/api/stories/' + sid + '/extended', {
                    method: 'PATCH',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({
                        summary: summary,
                        coverImage: coverImage
                    })
                }).then(function () { return sid; });
            })
            .then(function (sid) {
                if (!sid) return;

                // Delete existing chapters first if editing
                var deleteFirst = fetch('/api/chapters/story/' + sid)
                    .then(function (r) { return r.json(); })
                    .then(function (existing) {
                        if (!Array.isArray(existing) || existing.length === 0) return;
                        return Promise.all(existing.map(function (ch) {
                            return fetch('/api/chapters/' + ch.id, { method: 'DELETE' });
                        }));
                    })
                    .catch(function () { });

                return deleteFirst.then(function () {
                    // Save new chapters
                    return Promise.all(chapters.map(function (ch, i) {
                        return fetch('/api/chapters', {
                            method: 'POST',
                            headers: { 'Content-Type': 'application/json' },
                            body: JSON.stringify({
                                storyId: sid,
                                chapterNumber: i + 1,
                                title: ch.title,
                                content: ch.content
                            })
                        });
                    }));
                }).then(function () { return sid; });
            })
            .then(function (sid) {
                if (!sid) return;

                if (action === 'publish') {
                    // Submit for moderation
                    return fetch('/api/moderation/publish/' + sid, { method: 'POST' })
                        .then(function (r) { return r.json(); })
                        .then(function (result) {
                            if (result.status === 'published') {
                                showToast('Story published successfully!');
                            } else if (result.status === 'rejected') {
                                showToast('Story rejected: ' + (result.rejectionReason || 'Content flagged'));
                            } else {
                                showToast('Story saved and submitted for review');
                            }
                        });
                } else {
                    showToast('Story saved as draft!');
                }
            })
            .catch(function (err) {
                if (err && err.message) return; // Already handled
                showToast('Failed to save story');
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

    // Escape key closes modals
    document.addEventListener('keydown', function (e) {
        if (e.key === 'Escape') {
            if (chapterModal) chapterModal.classList.remove('show');
            if (importModal) importModal.classList.remove('show');
        }
    });
})();
