(function () {
    'use strict';

    // ═══════════════════════════════════════
    // Auth Status — runs on every page
    // ═══════════════════════════════════════
    var loginBtn = document.getElementById('loginBtn');
    var userMenu = document.getElementById('userMenu');

    if (!loginBtn || !userMenu) return;

    fetch('/api/auth/status')
        .then(function (resp) { return resp.json(); })
        .then(function (data) {
            if (data.authenticated) {
                loginBtn.style.display = 'none';
                userMenu.style.display = 'block';

                var avatarImg = document.getElementById('userAvatar');
                var userName = document.getElementById('dropdownName');
                var userSub = document.getElementById('dropdownSub');

                if (avatarImg) avatarImg.src = data.avatarUrl || '';
                if (userName) userName.textContent = data.username || 'User';
                if (userSub) userSub.textContent = 'Signed in via GitHub';
            } else {
                loginBtn.style.display = 'inline-flex';
                userMenu.style.display = 'none';
            }
        })
        .catch(function () {
            loginBtn.style.display = 'inline-flex';
            userMenu.style.display = 'none';
        });

    // ─── Dropdown Toggle ───
    var avatarBtn = document.getElementById('avatarBtn');
    var dropdown = document.getElementById('navDropdown');

    if (avatarBtn && dropdown) {
        avatarBtn.addEventListener('click', function (e) {
            e.stopPropagation();
            dropdown.classList.toggle('show');
        });

        document.addEventListener('click', function (e) {
            if (!dropdown.contains(e.target) && e.target !== avatarBtn) {
                dropdown.classList.remove('show');
            }
        });

        document.addEventListener('keydown', function (e) {
            if (e.key === 'Escape') {
                dropdown.classList.remove('show');
            }
        });
    }
})();
