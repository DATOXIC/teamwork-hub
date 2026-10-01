<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>

<%-- meeting.jsp — Họp video nhóm (Jitsi Meet nhúng). Controller: MeetingServlet (/meeting)
     Model: ${project}, ${roomName}, ${sessionScope.currentUser} --%>

<jsp:include page="/includes/header.jsp" />
<jsp:include page="/includes/navbar.jsp" />

<div class="container-fluid px-lg-5 py-4">
    <div class="d-flex flex-wrap align-items-center justify-content-between gap-3 mb-3 bg-white p-3 rounded-4 shadow-sm">
        <div class="d-flex flex-wrap align-items-center gap-3">
            <a href="${pageContext.request.contextPath}/project?action=list"
               class="btn btn-outline-secondary btn-sm rounded-pill px-3 shadow-none">
                <i class="bi bi-arrow-left me-1"></i> Danh sách dự án
            </a>
            <div class="border-start ps-3">
                <h4 class="fw-extrabold text-dark mb-0 tracking-tight"><c:out value="${project.name}" /></h4>
                <span class="fs-8 text-muted">Phòng họp video của nhóm</span>
            </div>
            <div class="d-none d-md-flex align-items-center gap-2 bg-light p-1 rounded-pill border ms-2">
                <a href="${pageContext.request.contextPath}/task?action=list&projectId=${project.id}"
                   class="btn btn-sm text-secondary rounded-pill px-3 py-1 fw-medium fs-8"><i class="bi bi-kanban me-1"></i> Kanban</a>
                <a href="${pageContext.request.contextPath}/whiteboard?projectId=${project.id}"
                   class="btn btn-sm text-secondary rounded-pill px-3 py-1 fw-medium fs-8"><i class="bi bi-easel me-1"></i> Bảng vẽ</a>
                <a href="${pageContext.request.contextPath}/meeting?projectId=${project.id}"
                   class="btn btn-sm btn-white bg-white text-primary shadow-2xs rounded-pill px-3 py-1 fw-bold fs-8"><i class="bi bi-camera-video me-1"></i> Họp video</a>
                <a href="${pageContext.request.contextPath}/doc?action=list&projectId=${project.id}"
                   class="btn btn-sm text-secondary rounded-pill px-3 py-1 fw-medium fs-8"><i class="bi bi-journal-text me-1"></i> Tài liệu</a>
                <a href="${pageContext.request.contextPath}/chat?action=view&projectId=${project.id}"
                   class="btn btn-sm text-secondary rounded-pill px-3 py-1 fw-medium fs-8"><i class="bi bi-chat-dots me-1"></i> Thảo luận</a>
            </div>
        </div>
        <button type="button" id="meetCopyBtn" class="btn btn-outline-primary btn-sm rounded-pill px-3">
            <i class="bi bi-link-45deg me-1"></i> Sao chép link mời
        </button>
    </div>

    <div id="meetRoot" class="meet-frame"></div>
</div>

<style>
    .meet-frame { height: calc(100vh - 230px); min-height: 480px; border-radius: 16px; overflow: hidden; background: #111827; }
</style>

<script src="https://meet.jit.si/external_api.js"></script>
<script>
(function () {
    var room = '<c:out value="${roomName}" />';
    var displayName = '<c:out value="${sessionScope.currentUser.fullName}" />';

    if (typeof JitsiMeetExternalAPI === 'undefined') {
        document.getElementById('meetRoot').innerHTML =
            '<div class="text-white p-4">Không tải được Jitsi Meet. Kiểm tra kết nối internet rồi tải lại trang.</div>';
        return;
    }

    new JitsiMeetExternalAPI('meet.jit.si', {
        roomName: room,
        parentNode: document.getElementById('meetRoot'),
        width: '100%',
        height: '100%',
        lang: 'vi',
        userInfo: { displayName: displayName },
        configOverwrite: { prejoinPageEnabled: true, disableDeepLinking: true },
        interfaceConfigOverwrite: { SHOW_JITSI_WATERMARK: false }
    });

    document.getElementById('meetCopyBtn').addEventListener('click', function () {
        var link = 'https://meet.jit.si/' + room;
        var btn = this;
        function done() { btn.innerHTML = '<i class="bi bi-check2 me-1"></i> Đã sao chép'; }
        if (navigator.clipboard) navigator.clipboard.writeText(link).then(done);
        else { window.prompt('Link mời:', link); }
    });
})();
</script>

<jsp:include page="/includes/footer.jsp" />
