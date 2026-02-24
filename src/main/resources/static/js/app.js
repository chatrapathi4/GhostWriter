(function () {
    'use strict';

    var editor = document.getElementById('storyEditor');
    var fullCtx = document.getElementById('fullContext');
    var memory = document.getElementById('shortMemory');
    var btn = document.getElementById('btnAnalyze');
    var btnLabel = document.getElementById('btnLabel');
    var wordEl = document.getElementById('wordCount');
    var charEl = document.getElementById('charCount');
    var fileInput = document.getElementById('fileUpload');
    var uploadArea = document.getElementById('uploadArea');
    var uploadStatus = document.getElementById('uploadStatus');

    // ═══════════════════════════════════════
    // Theme Toggle
    // ═══════════════════════════════════════
    var themeToggle = document.getElementById('themeToggle');
    var themeIcon = document.getElementById('themeIcon');
    var html = document.documentElement;

    // Load saved theme
    var savedTheme = localStorage.getItem('gw-theme') || 'dark';
    html.setAttribute('data-theme', savedTheme);
    themeIcon.textContent = savedTheme === 'dark' ? '☀️' : '🌙';

    themeToggle.addEventListener('click', function () {
        var current = html.getAttribute('data-theme');
        var next = current === 'dark' ? 'light' : 'dark';
        html.setAttribute('data-theme', next);
        themeIcon.textContent = next === 'dark' ? '☀️' : '🌙';
        localStorage.setItem('gw-theme', next);
    });

    // ═══════════════════════════════════════
    // Scroll Reveal Animations
    // ═══════════════════════════════════════
    var animElements = document.querySelectorAll('.anim-fade');

    var observer = new IntersectionObserver(function (entries) {
        entries.forEach(function (entry) {
            if (entry.isIntersecting) {
                var delay = parseInt(entry.target.getAttribute('data-delay') || '0', 10);
                setTimeout(function () {
                    entry.target.classList.add('anim-visible');
                }, delay * 100);
                observer.unobserve(entry.target);
            }
        });
    }, { threshold: 0.1, rootMargin: '0px 0px -40px 0px' });

    animElements.forEach(function (el) { observer.observe(el); });

    // ═══════════════════════════════════════
    // Word Count
    // ═══════════════════════════════════════
    function updateCounts() {
        var text = editor.value.trim();
        wordEl.textContent = text.length === 0 ? 0 : text.split(/\s+/).length;
        charEl.textContent = editor.value.length;
    }
    editor.addEventListener('input', updateCounts);
    updateCounts();

    // ═══════════════════════════════════════
    // File Upload
    // ═══════════════════════════════════════
    fileInput.addEventListener('change', async function () {
        var file = fileInput.files[0];
        if (!file) return;

        var ext = file.name.split('.').pop().toLowerCase();
        if (ext !== 'pdf' && ext !== 'txt') {
            showToast('Please upload a .pdf or .txt file');
            return;
        }

        uploadStatus.textContent = 'Uploading ' + file.name + '...';
        uploadArea.classList.add('uploading');

        try {
            var formData = new FormData();
            formData.append('file', file);

            var resp = await fetch('/api/upload', { method: 'POST', body: formData });
            var data = await resp.json();

            if (data.error) {
                showToast(data.error);
                uploadStatus.textContent = 'Drag & drop or click to browse';
            } else {
                editor.value = data.text;
                updateCounts();
                uploadStatus.textContent = '✓ Loaded: ' + data.filename;
                uploadArea.classList.add('uploaded');
                showToast('Story loaded from ' + data.filename);
            }
        } catch (err) {
            showToast('Upload failed');
            uploadStatus.textContent = 'Drag & drop or click to browse';
        } finally {
            uploadArea.classList.remove('uploading');
        }
    });

    // Drag & drop
    uploadArea.addEventListener('dragover', function (e) {
        e.preventDefault();
        uploadArea.classList.add('dragover');
    });
    uploadArea.addEventListener('dragleave', function () {
        uploadArea.classList.remove('dragover');
    });
    uploadArea.addEventListener('drop', function (e) {
        e.preventDefault();
        uploadArea.classList.remove('dragover');
        if (e.dataTransfer.files.length > 0) {
            fileInput.files = e.dataTransfer.files;
            fileInput.dispatchEvent(new Event('change'));
        }
    });

    // ═══════════════════════════════════════
    // Analyze
    // ═══════════════════════════════════════
    btn.addEventListener('click', async function () {
        var lastParagraph = editor.value.trim();
        if (lastParagraph.length < 10) {
            showToast('Write at least a few sentences first.');
            return;
        }

        btn.disabled = true;
        btnLabel.textContent = 'Analyzing...';
        document.getElementById('results').classList.remove('show');
        document.getElementById('loading').classList.add('show');

        try {
            var payload = {
                fullContext: fullCtx.value.trim() || lastParagraph,
                shortMemory: memory.value.trim() || '',
                lastParagraph: lastParagraph
            };

            var resp = await fetch('/api/analyze', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });

            if (!resp.ok) throw new Error('Server error ' + resp.status);
            var data = await resp.json();
            renderResults(data);
        } catch (err) {
            showToast('Failed to reach the ghost. Is the server running?');
            console.error(err);
        } finally {
            btn.disabled = false;
            btnLabel.textContent = 'Summon Ghost';
            document.getElementById('loading').classList.remove('show');
        }
    });

    // ═══════════════════════════════════════
    // Render Results
    // ═══════════════════════════════════════
    function renderResults(data) {
        var badgesHtml = '';
        badgesHtml += '<div class="badge badge-genre" style="animation-delay:0s"><span class="badge-label">Genre</span> ' + esc(data.genre_detected || 'Unknown') + '</div>';
        badgesHtml += '<div class="badge badge-tone" style="animation-delay:0.1s"><span class="badge-label">Tone</span> ' + esc(data.tone_detected || 'Neutral') + '</div>';
        if (data.source === 'ai') {
            badgesHtml += '<div class="badge badge-ai" style="animation-delay:0.2s"><span class="badge-label">Engine</span> ✨ Gemini AI</div>';
        } else {
            badgesHtml += '<div class="badge badge-template" style="animation-delay:0.2s"><span class="badge-label">Engine</span> ⚙️ Templates</div>';
        }
        document.getElementById('badges').innerHTML = badgesHtml;

        var entHtml = '';
        (data.key_entities || []).forEach(function (e, i) {
            entHtml += '<span class="entity" style="animation: badgePop 0.3s ease ' + (i * 0.05) + 's backwards">' + esc(e) + '</span>';
        });
        document.getElementById('entities').innerHTML = entHtml;

        var bridge = data.narrative_bridge || '';
        document.getElementById('bridge').textContent = bridge;
        document.getElementById('bridge').style.display = bridge ? 'block' : 'none';

        var dirHtml = '';
        (data.directions || []).forEach(function (dir, idx) {
            var name = (typeof dir === 'object') ? (dir.name || 'Path ' + (idx + 1)) : 'Path ' + (idx + 1);
            var desc = (typeof dir === 'object') ? (dir.description || '') : String(dir);

            dirHtml += '<div class="direction" data-name="' + esc(name) + '" data-desc="' + esc(desc) + '" style="animation: fadeIn 0.4s ease ' + (0.1 + idx * 0.12) + 's backwards">' +
                '<div class="direction-name"><span class="num">' + (idx + 1) + '</span>' + esc(name) + '</div>' +
                '<div class="direction-desc">' + esc(desc) + '</div>' +
                '<div class="direction-hint">Tap to preview this path</div>' +
                '</div>';
        });
        document.getElementById('directions').innerHTML = dirHtml;

        document.querySelectorAll('.direction').forEach(function (card) {
            card.addEventListener('click', function () {
                var pathName = card.getAttribute('data-name');
                var pathDesc = card.getAttribute('data-desc');
                showPathPreview(pathName, pathDesc);
            });
        });

        document.getElementById('results').classList.add('show');
        document.getElementById('results').scrollIntoView({ behavior: 'smooth', block: 'nearest' });
    }

    // ═══════════════════════════════════════
    // Path Preview
    // ═══════════════════════════════════════
    function showPathPreview(pathName, pathDesc) {
        var overlay = document.getElementById('previewOverlay');
        var titleEl = document.getElementById('previewTitle');
        var bodyEl = document.getElementById('previewBody');

        titleEl.textContent = pathName;
        bodyEl.innerHTML = '<div class="spinner"></div><p>Generating preview...</p>';
        overlay.classList.add('show');

        var storyContext = (fullCtx.value.trim() || editor.value.trim());

        fetch('/api/expand', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                storyContext: storyContext,
                pathName: pathName,
                pathDescription: pathDesc
            })
        })
            .then(function (resp) { return resp.json(); })
            .then(function (data) {
                bodyEl.innerHTML = '<p class="preview-text">' + esc(data.preview || 'No preview available.') + '</p>';
            })
            .catch(function () {
                bodyEl.innerHTML = '<p class="preview-text">Failed to generate preview.</p>';
            });
    }

    // Close preview
    document.getElementById('previewClose').addEventListener('click', function () {
        document.getElementById('previewOverlay').classList.remove('show');
    });
    document.getElementById('previewOverlay').addEventListener('click', function (e) {
        if (e.target === this) this.classList.remove('show');
    });

    // Escape key closes preview
    document.addEventListener('keydown', function (e) {
        if (e.key === 'Escape') {
            document.getElementById('previewOverlay').classList.remove('show');
        }
    });

    // ═══════════════════════════════════════
    // Toast
    // ═══════════════════════════════════════
    function showToast(msg) {
        var toast = document.getElementById('toast');
        document.getElementById('toastMsg').textContent = msg;
        toast.classList.add('show');
        setTimeout(function () { toast.classList.remove('show'); }, 3000);
    }

    function esc(text) {
        var d = document.createElement('div');
        d.textContent = text;
        return d.innerHTML;
    }

    // Ctrl+Enter shortcut
    editor.addEventListener('keydown', function (e) {
        if ((e.ctrlKey || e.metaKey) && e.key === 'Enter') {
            e.preventDefault();
            btn.click();
        }
    });
})();
