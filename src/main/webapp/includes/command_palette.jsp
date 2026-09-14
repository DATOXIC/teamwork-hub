<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ page import="com.teamwork.data.ProjectMemberDB" %>
<%@ page import="com.teamwork.business.Project" %>
<%@ page import="java.util.List" %>

<%
    // Tự động tải danh sách Dự án của người dùng hiện tại để phục vụ điều hướng nhanh
    if (session.getAttribute("currentUser") != null) {
        com.teamwork.business.User cpUser = (com.teamwork.business.User) session.getAttribute("currentUser");
        try {
            List<Project> cpProjects = ProjectMemberDB.selectProjectsByUserId(cpUser.getId());
            request.setAttribute("cpProjects", cpProjects);
        } catch (Exception ignored) {}
    }
%>

<!-- =========================================================================
     COMMAND PALETTE (CTRL + K) — HỘP ĐIỀU HƯỚNG LỆNH THÔNG MINH TOÀN HỆ THỐNG
     Phong cách: Linear / Raycast / Big Tech Glassmorphism
     ========================================================================= -->
<div id="commandPaletteOverlay" class="cp-overlay" aria-hidden="true" role="dialog" aria-modal="true" aria-label="Command Palette">
    <div class="cp-container" id="commandPaletteContainer">
        
        <!-- Header: Search Input & Shortcuts -->
        <div class="cp-header">
            <i class="bi bi-search cp-search-icon"></i>
            <input type="text" id="commandPaletteInput" class="cp-input" 
                   placeholder="Tìm kiếm công việc, dự án, tài liệu hoặc gõ lệnh..." 
                   autocomplete="off" spellcheck="false">
            <button type="button" class="cp-close-btn" id="commandPaletteCloseBtn" title="Đóng (Esc)" aria-label="Đóng Command Palette">
                <kbd class="cp-kbd">Esc</kbd>
            </button>
        </div>

        <!-- Scrollable Results Body -->
        <div class="cp-body" id="commandPaletteBody">

            <!-- Phân nhóm 1: NGỮ CẢNH DỰ ÁN HIỆN TẠI (Nếu đang ở trong 1 dự án) -->
            <c:if test="${not empty project}">
                <div class="cp-group" data-group="project-context">
                    <div class="cp-group-title">Phân Hệ: ${project.name} (#${project.projectCode})</div>
                    <ul class="cp-list">
                        <li class="cp-item" data-action="navigate" data-url="${pageContext.request.contextPath}/task?action=list&projectId=${project.id}" data-search="kanban cong viec bang nhiem vu ${project.name}">
                            <div class="cp-item-icon"><i class="bi bi-kanban"></i></div>
                            <div class="cp-item-content">
                                <div class="cp-item-title">Mở Bảng Kanban Công Việc</div>
                                <div class="cp-item-desc">Xem và kéo thả danh sách công việc của dự án này</div>
                            </div>
                            <span class="cp-item-badge">Kanban</span>
                            <span class="cp-item-action-key"><kbd class="cp-kbd">↵</kbd></span>
                        </li>

                        <li class="cp-item" data-action="navigate" data-url="${pageContext.request.contextPath}/doc?action=list&projectId=${project.id}" data-search="tai lieu wiki docs van ban huong dan ${project.name}">
                            <div class="cp-item-icon"><i class="bi bi-journal-text"></i></div>
                            <div class="cp-item-content">
                                <div class="cp-item-title">Mở Kho Tài Liệu & Wiki</div>
                                <div class="cp-item-desc">Đọc và soạn thảo tài liệu tri thức dự án</div>
                            </div>
                            <span class="cp-item-badge">Docs</span>
                            <span class="cp-item-action-key"><kbd class="cp-kbd">↵</kbd></span>
                        </li>

                        <li class="cp-item" data-action="navigate" data-url="${pageContext.request.contextPath}/chat?action=view&projectId=${project.id}" data-search="thao luan chat trao doi tin nhan ${project.name}">
                            <div class="cp-item-icon"><i class="bi bi-chat-dots"></i></div>
                            <div class="cp-item-content">
                                <div class="cp-item-title">Mở Kênh Thảo Luận Nhóm</div>
                                <div class="cp-item-desc">Trò chuyện thời gian thực cùng đồng đội</div>
                            </div>
                            <span class="cp-item-badge">Chat</span>
                            <span class="cp-item-action-key"><kbd class="cp-kbd">↵</kbd></span>
                        </li>

                        <li class="cp-item" data-action="navigate" data-url="${pageContext.request.contextPath}/report?action=view&projectId=${project.id}" data-search="bao cao tien do kpi danh gia report ${project.name}">
                            <div class="cp-item-icon"><i class="bi bi-file-earmark-bar-graph"></i></div>
                            <div class="cp-item-content">
                                <div class="cp-item-title">Xem Báo Cáo Tiến Độ Dự Án</div>
                                <div class="cp-item-desc">Xem chỉ số KPI, tỷ lệ hoàn thành và cảnh báo điểm nghẽn</div>
                            </div>
                            <span class="cp-item-badge">Report</span>
                            <span class="cp-item-action-key"><kbd class="cp-kbd">↵</kbd></span>
                        </li>

                        <li class="cp-item" data-action="print" data-search="in an print pdf xuat ban in bao cao ${project.name}">
                            <div class="cp-item-icon"><i class="bi bi-printer"></i></div>
                            <div class="cp-item-content">
                                <div class="cp-item-title">In Báo Cáo / Xuất File PDF (A4)</div>
                                <div class="cp-item-desc">Kích hoạt trình in ấn trình duyệt chuẩn khổ A4</div>
                            </div>
                            <span class="cp-item-badge">Action</span>
                            <span class="cp-item-action-key"><kbd class="cp-kbd">↵</kbd></span>
                        </li>

                        <li class="cp-item" data-action="navigate" data-url="${pageContext.request.contextPath}/task?action=exportCsv&projectId=${project.id}" data-search="xuat csv excel download tai ve export ${project.name}">
                            <div class="cp-item-icon"><i class="bi bi-file-earmark-spreadsheet"></i></div>
                            <div class="cp-item-content">
                                <div class="cp-item-title">Xuất Danh Sách Công Việc Ra File CSV</div>
                                <div class="cp-item-desc">Tải file Excel .csv chuẩn mã UTF-8 BOM</div>
                            </div>
                            <span class="cp-item-badge">Export</span>
                            <span class="cp-item-action-key"><kbd class="cp-kbd">↵</kbd></span>
                        </li>
                    </ul>
                </div>
            </c:if>

            <!-- Phân nhóm 2: DỰ ÁN CỦA BẠN (QUICK SWITCH PROJECTS) -->
            <c:if test="${not empty cpProjects}">
                <div class="cp-group" data-group="projects">
                    <div class="cp-group-title">Dự Án Của Bạn (${cpProjects.size()})</div>
                    <ul class="cp-list">
                        <c:forEach items="${cpProjects}" var="p">
                            <li class="cp-item" data-action="navigate" data-url="${pageContext.request.contextPath}/task?action=list&projectId=${p.id}" data-search="${p.projectCode} ${p.name} du an project">
                                <div class="cp-item-icon"><i class="bi bi-folder2-open"></i></div>
                                <div class="cp-item-content">
                                    <div class="cp-item-title">${p.name}</div>
                                    <div class="cp-item-desc">Mã: #${p.projectCode} &bull; ${p.doneTasks}/${p.totalTasks} công việc hoàn tất</div>
                                </div>
                                <span class="cp-item-badge">#${p.projectCode}</span>
                                <span class="cp-item-action-key"><kbd class="cp-kbd">↵</kbd></span>
                            </li>
                        </c:forEach>
                    </ul>
                </div>
            </c:if>

            <!-- Phân nhóm 3: HÀNH ĐỘNG HỆ THỐNG (SYSTEM ACTIONS) -->
            <div class="cp-group" data-group="system-actions">
                <div class="cp-group-title">Hành Động Nhanh</div>
                <ul class="cp-list">
                    <li class="cp-item" data-action="theme" data-search="doi giao dien sang toi dark light mode theme">
                        <div class="cp-item-icon"><i class="bi bi-moon-stars"></i></div>
                        <div class="cp-item-content">
                            <div class="cp-item-title">Chuyển Đổi Giao Diện (Sáng / Tối)</div>
                            <div class="cp-item-desc">Đổi chủ đề màu sắc toàn hệ thống giữa Light Mode và Dark Mode</div>
                        </div>
                        <span class="cp-item-badge">Giao diện</span>
                        <span class="cp-item-action-key"><kbd class="cp-kbd">↵</kbd></span>
                    </li>

                    <li class="cp-item" data-action="navigate" data-url="${pageContext.request.contextPath}/project?action=list" data-search="khong gian lam viec danh sach du an projects workspace dashboard">
                        <div class="cp-item-icon"><i class="bi bi-grid-1x2"></i></div>
                        <div class="cp-item-content">
                            <div class="cp-item-title">Vào Không Gian Làm Việc (Dashboard)</div>
                            <div class="cp-item-desc">Xem toàn bộ danh sách các dự án bạn đang tham gia</div>
                        </div>
                        <span class="cp-item-badge">Điều hướng</span>
                        <span class="cp-item-action-key"><kbd class="cp-kbd">↵</kbd></span>
                    </li>

                    <li class="cp-item" data-action="navigate" data-url="${pageContext.request.contextPath}/profile" data-search="ho so ca nhan profile account chuyen mon ky nang password">
                        <div class="cp-item-icon"><i class="bi bi-person-badge"></i></div>
                        <div class="cp-item-content">
                            <div class="cp-item-title">Hồ Sơ Chuyên Môn Của Tôi</div>
                            <div class="cp-item-desc">Cập nhật thông tin cá nhân, kỹ năng và đổi mật khẩu</div>
                        </div>
                        <span class="cp-item-badge">Tài khoản</span>
                        <span class="cp-item-action-key"><kbd class="cp-kbd">↵</kbd></span>
                    </li>

                    <li class="cp-item" data-action="navigate" data-url="${pageContext.request.contextPath}/auth?action=logout" data-search="dang xuat thoat logout sign out">
                        <div class="cp-item-icon text-danger"><i class="bi bi-box-arrow-right"></i></div>
                        <div class="cp-item-content">
                            <div class="cp-item-title text-danger">Đăng Xuất Khỏi TeamWork Hub</div>
                            <div class="cp-item-desc">Kết thúc phiên làm việc an toàn</div>
                        </div>
                        <span class="cp-item-badge">Bảo mật</span>
                        <span class="cp-item-action-key"><kbd class="cp-kbd">↵</kbd></span>
                    </li>
                </ul>
            </div>

            <!-- Empty State when query matches nothing -->
            <div id="commandPaletteEmpty" class="cp-empty d-none">
                <i class="bi bi-search cp-empty-icon d-block"></i>
                <div class="fw-bold fs-7">Không tìm thấy kết quả nào phù hợp</div>
                <div class="fs-8 mt-1">Thử gõ tên dự án, "kanban", "báo cáo", "tối", hoặc "hồ sơ"</div>
            </div>

        </div>

        <!-- Footer: Tips & Keybindings -->
        <div class="cp-footer">
            <div class="cp-footer-tips">
                <div class="cp-tip-item">
                    <kbd class="cp-kbd">↑</kbd>
                    <kbd class="cp-kbd">↓</kbd>
                    <span>di chuyển</span>
                </div>
                <div class="cp-tip-item">
                    <kbd class="cp-kbd">↵</kbd>
                    <span>thực thi</span>
                </div>
                <div class="cp-tip-item">
                    <kbd class="cp-kbd">esc</kbd>
                    <span>đóng</span>
                </div>
            </div>
            <div class="d-none d-sm-block text-muted">
                <strong>TeamWork Hub</strong> &bull; Command Palette
            </div>
        </div>

    </div>
</div>
