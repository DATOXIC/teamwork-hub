/*
 * meeting.js — Nhúng phòng họp Jitsi Meet + nút sao chép link (meeting.jsp).
 * Cấu hình từ server lấy từ đối tượng toàn cục khai báo ngay trước thẻ <script src> trong meeting.jsp.
 */
(function () {
var room = MEET_CONFIG.room;
var displayName = MEET_CONFIG.displayName;

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
