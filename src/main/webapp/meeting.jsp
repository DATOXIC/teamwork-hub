<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>

<%-- meeting.jsp — Họp video nhóm (Jitsi Meet nhúng). Controller: MeetingServlet (/meeting)
     Model: ${project}, ${roomName}, ${sessionScope.currentUser} --%>

<jsp:include page="/includes/header.jsp" />
<jsp:include page="/includes/navbar.jsp" />

<div class="container-fluid px-lg-5 py-4">
    <!-- 2. THANH ĐIỀU HƯỚNG DỰ ÁN & CHUYỂN PHÂN HỆ (7 Phân hệ chuẩn hóa) -->
    <c:set var="activeSubNav" value="meeting" scope="request" />
    <jsp:include page="/includes/project_subnav.jsp" />

    <div class="d-flex flex-wrap align-items-center justify-content-between gap-3 mb-3">
        <div class="d-flex align-items-center gap-2">
            <span class="badge rounded-pill bg-success-subtle text-success px-3 py-1-5 fs-8 fw-semibold border border-success-subtle">
                <i class="bi bi-broadcast me-1"></i> Trực tuyến
            </span>
            <span class="text-muted fs-8">Phòng họp video bảo mật của nhóm</span>
        </div>
        <button type="button" id="meetCopyBtn" class="btn btn-outline-primary btn-sm rounded-pill px-3 shadow-none">
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
