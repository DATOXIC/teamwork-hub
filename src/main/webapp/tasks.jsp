<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>

<%-- =========================================================================
     MVC SKELETON & CONTRACT NOTE — tasks.jsp
     Controller: TaskServlet (/task)
     
     1. Luồng dữ liệu vào (Inbound Data / Model Attributes):
        - ${project}          : Project entity (id, name, ownerId)
        - ${projectMembers}   : List<ProjectMember> thành viên trong dự án
        - ${todoTasks}        : List<Task> danh sách việc cần làm (Cột 1)
        - ${inProgressTasks}  : List<Task> danh sách việc đang làm (Cột 2)
        - ${doneTasks}        : List<Task> danh sách việc đã hoàn thành (Cột 3)
        - ${taskProgressMap}  : Map<Integer, Integer> tỷ lệ % tiến độ theo Task ID
        - ${subTasksMap}      : Map<Integer, List<SubTask>> cây việc con theo Task ID
        - ${docs}             : List<Doc> tài liệu tham khảo đính kèm
        - ${userNotifications}: List<Notification> thông báo người dùng
        - ${isOwner}          : boolean quyền PM / Owner
     
     2. Luồng thao tác (Outbound Form Actions & Quality Gates):
        - POST /task?action=add              : Thêm công việc lớn (Task cha)
        - POST /task?action=updateStatus     : Cập nhật vị trí kéo thả HTML5 Drag-Drop
        - POST /task?action=addSubTask       : Thêm việc con (Sub-task)
        - POST /task?action=toggleSubTask    : Hoàn tất việc con [☑]
        - POST /task?action=submitParentTask : Nộp bàn giao Task lớn lên PM (Gate 2)
        - POST /task?action=pmApproveTask    : PM duyệt nghiệm thu Task lớn
        - POST /task?action=pmReviseTask     : PM yêu cầu cân chỉnh Task lớn
        - POST /task?action=pmRejectTask     : PM từ chối / trả về Task lớn
        - POST /task?action=sendTaskComment  : Gửi bình luận trao đổi Task
     ========================================================================= --%>

<c:set var="subtaskMode" value="${cookie.preferred_subtask_mode != null && cookie.preferred_subtask_mode.value == 'expanded' ? 'expanded' : 'collapsed'}" />

<!-- 1. NẠP HEADER & THANH ĐIỀU HƯỚNG CHUNG -->
<jsp:include page="/includes/header.jsp" />
<%-- Quy tắc dự án: chỉ Trưởng dự án (PM) được tạo / giao công việc và bật-tắt cổng duyệt (server cũng chặn) --%>
<c:set var="isPm" value="${project.ownerId == sessionScope.currentUser.id}" />

        <style>
            /* Khóa cứng Viewport SaaS: Triệt tiêu hoàn toàn thanh cuộn cấp độ trang web */
            html, body {
                overflow: hidden !important;
                height: 100vh !important;
                max-height: 100vh !important;
                width: 100vw !important;
                max-width: 100vw !important;
                margin: 0 !important;
                padding: 0 !important;
            }
        </style>

        <!-- =========================================================================
             CLICKUP 3.0 UNIFIED APP SHELL ARCHITECTURE (1 TRANG HỢP NHẤT)
             ========================================================================= -->
        <!-- =========================================================================
             UNIFIED MODERN SAAS WORKSPACE ARCHITECTURE
             ========================================================================= -->
        <div class="clickup-shell">

            <div class="clickup-islands-row">

                <%@ include file="/WEB-INF/jspf/tasks/sidebar.jspf" %>

                <!-- =========================================================================
                     2. MAIN WORKSPACE CANVAS
                     ========================================================================= -->
                <main class="clickup-main-panel">
                    <%@ include file="/WEB-INF/jspf/tasks/main-header.jspf" %>
                <!-- Workspace Content Area (Scrollable) -->
                <div class="clickup-workspace-body">
                    <!-- Toast thông báo -->
                    <jsp:include page="/includes/toast.jsp" />

                    <!-- Dải Banner Phản Hồi Khi Đang Lọc Task -->
                    <div id="activeFilterBanner" class="active-filter-banner d-none">
                        <div class="d-flex align-items-center gap-2">
                            <span class="avatar-circle-sm bg-primary text-white rounded-circle d-flex align-items-center justify-content-center" style="width: 22px; height: 22px; font-size: 0.65rem;">
                                <i class="bi bi-funnel-fill"></i>
                            </span>
                            <span id="activeFilterText">Đang lọc: <strong>Công việc của tôi</strong></span>
                        </div>
                        <button type="button" class="btn btn-sm btn-link text-primary fw-bold text-decoration-none p-0 fs-8" onclick="filterClickUpTasks('ALL')">
                            <i class="bi bi-x-circle me-1"></i> Xóa bộ lọc (Xem tất cả)
                        </button>
                    </div>

                    <!-- =========================================================================
                         TAB 1: TASKS CONTAINER (GỒM LIST VIEW VÀ BOARD VIEW)
                         ========================================================================= -->
                    <div id="clickup-view-tasks" class="clickup-view-pane ${currentView == 'tasks' ? '' : 'd-none'}">



                        <%@ include file="/WEB-INF/jspf/tasks/list-view.jspf" %>
                        <%@ include file="/WEB-INF/jspf/tasks/board-view.jspf" %>

            </div><!-- /clickup-view-tasks -->

            <%@ include file="/WEB-INF/jspf/tasks/workload-view.jspf" %>



            <%@ include file="/WEB-INF/jspf/tasks/activity-view.jspf" %>

        </div><!-- /clickup-workspace-body -->
    </main><!-- /clickup-main-panel -->
    </div><!-- /clickup-islands-row -->
</div><!-- /clickup-shell -->

<%@ include file="/WEB-INF/jspf/tasks/inbox-drawer.jspf" %>
<!-- ClickUp 3.0 Interactive Controller Scripts -->
<!-- TASK_PAGE: dữ liệu Servlet → JS. Mọi giá trị động của tasks.jsp mà JS cần đều nằm ở đây. -->
<jsp:useBean id="assetStamp" class="java.util.Date" />
<script>
    window.TASK_PAGE = {
        contextPath: "${pageContext.request.contextPath}",
        projectId: ${project.id},
        soloProject: ${project.soloProject},
        teamProject: ${project.teamProject},
        currentUserId: "${sessionScope.currentUser.id}",
        subtaskMode: "${subtaskMode}",
        <%-- ◀ SERVLET: SubTaskHandler, TaskBoardHandler, TaskCrudHandler … → setAttribute("toastSuccess") --%>
        <%-- Chuỗi trong JS: escapeXml chặn đóng thẻ/đóng nháy, replace thêm dấu \ để không phá chuỗi JS --%>
        toastSuccess: "${fn:replace(fn:escapeXml(toastSuccess), '\\', '\\\\')}",
        members: [
            <c:forEach items="${userList}" var="u" varStatus="loop">
            { id: ${u.id}, name: '${fn:replace(fn:escapeXml(u.fullName), '\\', '\\\\')}' }<c:if test="${!loop.last}">,</c:if>
            </c:forEach>
        ]
    };
</script>
<script src="${pageContext.request.contextPath}/js/tasks-board.js?v=${assetStamp.time}"></script>
        <!-- =========================================================================
             4. CLICKUP 3.0 UNIFIED TASK & SUBTASK SIDE-PEEK DRAWER
             ========================================================================= -->
        <div class="offcanvas offcanvas-end clickup-task-drawer shadow-lg" tabindex="-1" id="clickupTaskDrawer" aria-labelledby="clickupTaskDrawerLabel">
            <div class="offcanvas-body drawer-body p-0" id="clickupDrawerContent">
                <c:forEach items="${allProjectTasks}" var="task">
                    <%@ include file="/WEB-INF/jspf/tasks/drawer/task-pane.jspf" %>

                    <%@ include file="/WEB-INF/jspf/tasks/drawer/subtask-panes.jspf" %>
                </c:forEach>
            </div>
        </div>

        <%@ include file="/WEB-INF/jspf/tasks/modals/member-profile.jspf" %>

        <%@ include file="/WEB-INF/jspf/tasks/modals/add-task.jspf" %>

        <%@ include file="/WEB-INF/jspf/tasks/modals/project-team.jspf" %>
        <%@ include file="/WEB-INF/jspf/tasks/modals/invite-member.jspf" %>
        <%@ include file="/WEB-INF/jspf/tasks/modals/project-settings.jspf" %>
        <%@ include file="/WEB-INF/jspf/tasks/modals/create-project.jspf" %>
        <%@ include file="/WEB-INF/jspf/tasks/modals/join-project.jspf" %>
        <%@ include file="/WEB-INF/jspf/tasks/modals/shortcuts.jspf" %>
        <!-- 7. NẠP FILE JAVASCRIPT KÉO THẢ & LỌC TỨC THÌ (0.01 GIÂY) -->
        <script src="${pageContext.request.contextPath}/js/tasks.js?v=${assetStamp.time}"></script>

        <!-- Bootstrap 5.3.3 JS Bundle CDN -->
        <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js" integrity="sha384-YvpcrYf0tY3lHB60NNkmXc5s9fDVZLESaAA55NDzOxhy9GkcIdslK1eN7N6jIeHz" crossorigin="anonymous"></script>
        <!-- UI-04: Global App JS (Floating Toast System + Utilities) -->
        <script src="${pageContext.request.contextPath}/js/app.js?v=<%= System.currentTimeMillis() %>"></script>
    </body>
</html>
