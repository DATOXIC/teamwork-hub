/*
 * home.js — Hiệu ứng trang chủ (index.jsp).
 * Được nạp bằng <script src> ở cuối index.jsp (tách từ script nhúng để dễ tìm / sửa).
 */
document.addEventListener('DOMContentLoaded', function () {

// 1. Tương tác chuyển đổi Tab Showcase trên Hero Studio Window
var tabBtns = document.querySelectorAll('.showcase-tab-btn');
var viewPanels = document.querySelectorAll('.viewport-panel');

tabBtns.forEach(function (btn) {
    btn.addEventListener('click', function () {
        var targetId = this.getAttribute('data-target');

        // Bật active cho button
        tabBtns.forEach(function (b) { b.classList.remove('active'); });
        this.classList.add('active');

        // Bật active cho màn hình hiển thị tương ứng
        viewPanels.forEach(function (panel) {
            if (panel.id === targetId) {
                panel.classList.add('active');
            } else {
                panel.classList.remove('active');
            }
        });
    });
});

// 2. Hiệu ứng viền sáng phát quang theo con trỏ chuột (Mouse-Tracking Spotlight)
var bentoCards = document.querySelectorAll('.bento-card');
bentoCards.forEach(function (card) {
    card.addEventListener('mousemove', function (e) {
        var rect = card.getBoundingClientRect();
        var x = e.clientX - rect.left;
        var y = e.clientY - rect.top;
        card.style.setProperty('--mouse-x', x + 'px');
        card.style.setProperty('--mouse-y', y + 'px');
    });
});

// 3. Hiệu ứng Scroll Reveal (Trồi lên so le khi cuộn chuột)
var revealElements = document.querySelectorAll('.reveal-up');
if ('IntersectionObserver' in window) {
    var revealObserver = new IntersectionObserver(function (entries) {
        entries.forEach(function (entry) {
            if (entry.isIntersecting) {
                entry.target.classList.add('is-visible');
                revealObserver.unobserve(entry.target);
            }
        });
    }, {
        threshold: 0.1,
        rootMargin: '0px 0px -40px 0px'
    });

    revealElements.forEach(function (el) {
        revealObserver.observe(el);
    });
} else {
    // Fallback nếu trình duyệt không hỗ trợ IntersectionObserver
    revealElements.forEach(function (el) {
        el.classList.add('is-visible');
    });
}

// 4. Tự động chuyển tab nhẹ sau 6 giây nếu người dùng không tương tác (Carousel Preview)
var autoTimer = null;
var currentTabIndex = 0;
function startAutoTabCycle() {
    autoTimer = setInterval(function () {
        currentTabIndex = (currentTabIndex + 1) % tabBtns.length;
        tabBtns[currentTabIndex].click();
    }, 6000);
}
function stopAutoTabCycle() {
    if (autoTimer) clearInterval(autoTimer);
}

startAutoTabCycle();
var showcaseContainer = document.querySelector('.showcase-container');
if (showcaseContainer) {
    showcaseContainer.addEventListener('mouseenter', stopAutoTabCycle);
    showcaseContainer.addEventListener('mouseleave', startAutoTabCycle);
}
});
