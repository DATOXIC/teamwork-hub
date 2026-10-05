/*
 * project_report.js — Biểu đồ + tương tác trang Báo cáo chất lượng dự án (project_report.jsp).
 * Được nạp bằng <script src> ở cuối project_report.jsp (tách từ script nhúng để dễ tìm / sửa).
 */
// 1. Quản lý trạng thái bộ lọc Task (Interactive Task Filter & Search)
let currentStatusFilter = 'ALL';
let currentSearchQuery = '';

function filterTasks(filterType, btnElement) {
currentStatusFilter = filterType;

// Cập nhật trạng thái active của tab buttons
const buttons = document.querySelectorAll('#taskFilterTabs .filter-tab-btn');
buttons.forEach(b => b.classList.remove('active'));
if (btnElement) {
    btnElement.classList.add('active');
} else {
    // Tự tìm button theo filterType nếu được gọi từ KPI card
    buttons.forEach(b => {
        const onclickAttr = b.getAttribute('onclick') || '';
        if (onclickAttr.includes("'" + filterType + "'")) {
            b.classList.add('active');
        }
    });
}

syncKpiCardState(filterType);
applyTaskFilters();
}

function handleKpiCardClick(filterType) {
filterTasks(filterType);
scrollToTasks();
}

function syncKpiCardState(filterType) {
const kpiCards = document.querySelectorAll('.report-stat-card');
kpiCards.forEach(c => c.classList.remove('active'));

const map = {
    'ALL': 'kpiCardAll',
    'DONE': 'kpiCardDone',
    'IN_PROGRESS': 'kpiCardInProgress',
    'PENDING_REVIEW': 'kpiCardPending',
    'OVERDUE': 'kpiCardOverdue'
};

if (map[filterType]) {
    const card = document.getElementById(map[filterType]);
    if (card) card.classList.add('active');
}
}

function scrollToTasks() {
const tableSection = document.getElementById('tasksInventorySection');
if (tableSection) {
    tableSection.scrollIntoView({ behavior: 'smooth', block: 'start' });
}
}

function handleTaskSearch() {
const input = document.getElementById('taskSearchInput');
currentSearchQuery = input ? input.value.trim().toLowerCase() : '';
applyTaskFilters();
}

function applyTaskFilters() {
const rows = document.querySelectorAll('.task-inventory-row');
let visibleCount = 0;

rows.forEach(row => {
    const status = row.getAttribute('data-status') || '';
    const isOverdue = row.getAttribute('data-overdue') === 'true';
    const searchTarget = (row.getAttribute('data-search') || '').toLowerCase();

    // Khớp trạng thái (Status match)
    let matchesStatus = false;
    if (currentStatusFilter === 'ALL') {
        matchesStatus = true;
    } else if (currentStatusFilter === 'TODO') {
        matchesStatus = (status === 'TODO' || status === 'PLANNING_REJECTED' || status === 'CREATED' || status === '');
    } else if (currentStatusFilter === 'IN_PROGRESS') {
        matchesStatus = (status === 'IN_PROGRESS');
    } else if (currentStatusFilter === 'PENDING_REVIEW') {
        matchesStatus = (status === 'SUBMITTED' || status === 'PLANNING');
    } else if (currentStatusFilter === 'REVISE_OR_REJECT') {
        matchesStatus = (status === 'REVISE' || status === 'REJECTED');
    } else if (currentStatusFilter === 'DONE') {
        matchesStatus = (status === 'DONE' || status === 'APPROVED');
    } else if (currentStatusFilter === 'OVERDUE') {
        matchesStatus = isOverdue;
    }

    // Khớp từ khóa tìm kiếm (Search match)
    let matchesSearch = true;
    if (currentSearchQuery.length > 0) {
        matchesSearch = searchTarget.includes(currentSearchQuery);
    }

    if (matchesStatus && matchesSearch) {
        row.classList.remove('d-none');
        visibleCount++;
    } else {
        row.classList.add('d-none');
    }
});

// Cập nhật số lượng công việc hiển thị
const countBadge = document.getElementById('tasksVisibleCount');
if (countBadge) {
    countBadge.innerText = 'Hiển thị: ' + visibleCount + ' / ' + rows.length + ' công việc';
}

// Hiển thị dòng empty state nếu không tìm thấy kết quả
const noMatchRow = document.getElementById('noMatchingTasksRow');
if (noMatchRow) {
    if (visibleCount === 0 && rows.length > 0) {
        noMatchRow.classList.remove('d-none');
    } else {
        noMatchRow.classList.add('d-none');
    }
}
}

// 2. Theme Management (Light / Dark Mode Switcher)
function toggleReportTheme() {
if (typeof toggleGlobalTheme === 'function') {
    toggleGlobalTheme();
} else {
    var current = document.documentElement.getAttribute('data-theme') || 'light';
    var next = current === 'dark' ? 'light' : 'dark';
    document.documentElement.setAttribute('data-theme', next);
    document.documentElement.setAttribute('data-bs-theme', next);
    try {
        localStorage.setItem('teamwork_theme', next);
        localStorage.setItem('teamwork_report_theme', next);
    } catch (e) {}
}
}

function initReportTheme() {
if (typeof applyGlobalTheme === 'function') {
    var current = document.documentElement.getAttribute('data-theme') || 'light';
    applyGlobalTheme(current);
}
}

// Khởi tạo trạng thái giao diện khi trang sẵn sàng
document.addEventListener('DOMContentLoaded', function() {
initReportTheme();
});
