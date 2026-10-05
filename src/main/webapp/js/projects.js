/*
 * projects.js — Tương tác trang Danh sách dự án (projects.jsp): lọc, tìm kiếm, modal tạo dự án.
 * Được nạp bằng <script src> ở cuối projects.jsp (tách từ script nhúng để dễ tìm / sửa).
 */
        function copyProjectCode(code) {
            if (!code) return;
            if (navigator.clipboard && window.isSecureContext) {
                navigator.clipboard.writeText(code).then(showToast).catch(fallback);
            } else {
                fallback();
            }

            function fallback() {
                var temp = document.createElement('textarea');
                temp.value = code;
                document.body.appendChild(temp);
                temp.select();
                try {
                    document.execCommand('copy');
                    showToast();
                } catch (err) {
                    console.error('Không thể copy', err);
                }
                document.body.removeChild(temp);
            }

            function showToast() {
                var textEl = document.getElementById('copiedCodeText');
                if (textEl) textEl.textContent = code;
                var toastEl = document.getElementById('copyToast');
                if (toastEl) {
                    var toast = new bootstrap.Toast(toastEl, { delay: 2500 });
                    toast.show();
                }
            }
        }

        // =========================================================================
        // LỌC DỰ ÁN & TÌM KIẾM TỨC THÌ (LIVE SEARCH & FILTER PILLS)
        // =========================================================================
        var currentFilterRole = 'all';
        var currentSearchKeyword = '';

        function filterProjects(role, btnEl) {
            currentFilterRole = role;
            // Đổi active state của nút
            var buttons = document.querySelectorAll('.filter-pill-btn');
            buttons.forEach(function(b) { b.classList.remove('active'); });
            if (btnEl) btnEl.classList.add('active');
            applyProjectFilters();
        }

        function searchProjectsLive(keyword) {
            currentSearchKeyword = (keyword || '').trim().toLowerCase();
            var clearBtn = document.getElementById('clearSearchBtn');
            if (clearBtn) {
                if (currentSearchKeyword.length > 0) {
                    clearBtn.classList.remove('d-none');
                } else {
                    clearBtn.classList.add('d-none');
                }
            }
            applyProjectFilters();
        }

        function clearProjectSearch() {
            var searchInput = document.getElementById('projectSearchInput');
            if (searchInput) searchInput.value = '';
            searchProjectsLive('');
        }

        function applyProjectFilters() {
            var items = document.querySelectorAll('#myProjectsGrid .project-item');
            var visibleCount = 0;

            items.forEach(function(item) {
                var itemRole = item.getAttribute('data-role');
                var itemName = item.getAttribute('data-name') || '';
                var itemCode = item.getAttribute('data-code') || '';

                var matchesRole = (currentFilterRole === 'all') || (itemRole === currentFilterRole);
                var matchesSearch = !currentSearchKeyword || (itemName.indexOf(currentSearchKeyword) !== -1 || itemCode.indexOf(currentSearchKeyword) !== -1);

                if (matchesRole && matchesSearch) {
                    item.classList.remove('d-none');
                    visibleCount++;
                } else {
                    item.classList.add('d-none');
                }
            });

            var countBadge = document.getElementById('myProjectsCountBadge');
            if (countBadge) countBadge.textContent = visibleCount;

            var noResultsAlert = document.getElementById('noSearchResultsAlert');
            if (noResultsAlert) {
                if (visibleCount === 0 && items.length > 0) {
                    noResultsAlert.classList.remove('d-none');
                } else {
                    noResultsAlert.classList.add('d-none');
                }
            }
        }
