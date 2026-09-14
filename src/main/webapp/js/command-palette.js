/**
 * TEAMWORK HUB — COMMAND PALETTE CONTROLLER (CTRL + K)
 * Động cơ điều hướng và tìm kiếm thông minh toàn hệ thống
 */

(function () {
    'use strict';

    let overlay = null;
    let input = null;
    let items = [];
    let selectedIndex = -1;

    // Chuẩn hóa chuỗi tìm kiếm (Loại bỏ dấu tiếng Việt để tìm kiếm không dấu / có dấu)
    function normalizeString(str) {
        if (!str) return '';
        return str.toString().toLowerCase()
            .normalize('NFD')
            .replace(/[\u0300-\u036f]/g, '')
            .replace(/đ/g, 'd')
            .replace(/Đ/g, 'd')
            .trim();
    }

    function init() {
        overlay = document.getElementById('commandPaletteOverlay');
        if (!overlay) return;

        input = document.getElementById('commandPaletteInput');
        const container = document.getElementById('commandPaletteContainer');
        const closeBtn = document.getElementById('commandPaletteCloseBtn');

        // Bắt sự kiện phím tắt toàn cầu (Ctrl+K hoặc Cmd+K)
        window.addEventListener('keydown', function (e) {
            // Không can thiệp nếu người dùng đang ở trong modal khác (trừ khi là phím Escape trong CP)
            const isCtrlK = (e.ctrlKey || e.metaKey) && (e.key === 'k' || e.key === 'K');
            
            if (isCtrlK) {
                e.preventDefault();
                toggle();
                return;
            }

            if (isOpen()) {
                if (e.key === 'Escape') {
                    e.preventDefault();
                    close();
                } else if (e.key === 'ArrowDown') {
                    e.preventDefault();
                    navigate(1);
                } else if (e.key === 'ArrowUp') {
                    e.preventDefault();
                    navigate(-1);
                } else if (e.key === 'Enter') {
                    e.preventDefault();
                    executeSelected();
                }
            }
        });

        // Bắt sự kiện nhập từ khóa tìm kiếm
        if (input) {
            input.addEventListener('input', function () {
                filterItems(input.value);
            });
        }

        // Đóng khi click nút Close hoặc click ra ngoài vùng nền mờ
        if (closeBtn) {
            closeBtn.addEventListener('click', close);
        }

        overlay.addEventListener('click', function (e) {
            if (container && !container.contains(e.target)) {
                close();
            }
        });

        // Click trực tiếp vào một mục bất kỳ
        document.querySelectorAll('.cp-item').forEach(function (item) {
            item.addEventListener('click', function () {
                executeItem(item);
            });
            item.addEventListener('mouseenter', function () {
                setSelectedItem(item);
            });
        });

        // Tìm tất cả các nút trigger trên giao diện
        document.querySelectorAll('[data-cp-trigger]').forEach(function (btn) {
            btn.addEventListener('click', function (e) {
                e.preventDefault();
                open();
            });
        });
    }

    function isOpen() {
        return overlay && overlay.classList.contains('is-active');
    }

    function open() {
        if (!overlay) return;
        overlay.classList.add('is-active');
        overlay.setAttribute('aria-hidden', 'false');
        document.body.style.overflow = 'hidden'; // Khóa cuộn trang nền

        if (input) {
            input.value = '';
            filterItems('');
            setTimeout(function () {
                input.focus();
                input.select();
            }, 60);
        }
    }

    function close() {
        if (!overlay) return;
        overlay.classList.remove('is-active');
        overlay.setAttribute('aria-hidden', 'true');
        document.body.style.overflow = ''; // Phục hồi cuộn trang

        if (input) {
            input.blur();
        }
    }

    function toggle() {
        if (isOpen()) {
            close();
        } else {
            open();
        }
    }

    function getVisibleItems() {
        if (!overlay) return [];
        return Array.from(overlay.querySelectorAll('.cp-item')).filter(function (el) {
            return !el.classList.contains('d-none');
        });
    }

    function setSelectedItem(targetItem) {
        const visible = getVisibleItems();
        visible.forEach(function (el) {
            el.classList.remove('is-selected');
        });

        if (targetItem) {
            targetItem.classList.add('is-selected');
            selectedIndex = visible.indexOf(targetItem);
            scrollIntoView(targetItem);
        } else {
            selectedIndex = -1;
        }
    }

    function scrollIntoView(item) {
        if (!item) return;
        item.scrollIntoView({ block: 'nearest', behavior: 'smooth' });
    }

    function navigate(direction) {
        const visible = getVisibleItems();
        if (visible.length === 0) return;

        selectedIndex += direction;
        if (selectedIndex >= visible.length) {
            selectedIndex = 0; // Vòng lại đầu danh sách
        } else if (selectedIndex < 0) {
            selectedIndex = visible.length - 1; // Nhảy xuống cuối danh sách
        }

        setSelectedItem(visible[selectedIndex]);
    }

    function filterItems(query) {
        const normalizedQuery = normalizeString(query);
        const allItems = overlay ? overlay.querySelectorAll('.cp-item') : [];
        let visibleCount = 0;

        allItems.forEach(function (item) {
            const rawSearch = item.getAttribute('data-search') || '';
            const titleText = item.querySelector('.cp-item-title')?.textContent || '';
            const descText = item.querySelector('.cp-item-desc')?.textContent || '';
            
            const combined = normalizeString(rawSearch + ' ' + titleText + ' ' + descText);

            if (!normalizedQuery || combined.includes(normalizedQuery)) {
                item.classList.remove('d-none');
                visibleCount++;
            } else {
                item.classList.add('d-none');
            }
        });

        // Ẩn/Hiện nhóm cha nếu không có mục con nào khớp
        if (overlay) {
            overlay.querySelectorAll('.cp-group').forEach(function (group) {
                const hasVisible = group.querySelectorAll('.cp-item:not(.d-none)').length > 0;
                if (hasVisible) {
                    group.classList.remove('d-none');
                } else {
                    group.classList.add('d-none');
                }
            });

            // Empty state
            const emptyEl = document.getElementById('commandPaletteEmpty');
            if (emptyEl) {
                if (visibleCount === 0) {
                    emptyEl.classList.remove('d-none');
                } else {
                    emptyEl.classList.add('d-none');
                }
            }
        }

        // Tự động chọn mục đầu tiên
        const visible = getVisibleItems();
        if (visible.length > 0) {
            setSelectedItem(visible[0]);
        } else {
            selectedIndex = -1;
        }
    }

    function executeSelected() {
        const visible = getVisibleItems();
        if (selectedIndex >= 0 && selectedIndex < visible.length) {
            executeItem(visible[selectedIndex]);
        }
    }

    function executeItem(item) {
        if (!item) return;

        const action = item.getAttribute('data-action');
        const url = item.getAttribute('data-url');

        close();

        if (action === 'navigate' && url) {
            window.location.href = url;
        } else if (action === 'theme') {
            if (typeof window.toggleGlobalTheme === 'function') {
                window.toggleGlobalTheme();
            }
        } else if (action === 'print') {
            setTimeout(function () {
                window.print();
            }, 180);
        }
    }

    // Khởi tạo khi DOM sẵn sàng
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }

    // Xuất ra phạm vi toàn cục
    window.openCommandPalette = open;
    window.closeCommandPalette = close;
    window.toggleCommandPalette = toggle;

})();
