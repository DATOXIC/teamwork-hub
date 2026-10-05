/*
 * login.js — Chuyển tab Đăng nhập/Đăng ký, kiểm tra form (login.jsp).
 * Được nạp bằng <script src> ở cuối login.jsp (tách từ script nhúng để dễ tìm / sửa).
 */
/* ── Hàm 1: Chuyển tab Đăng nhập / Đăng ký (Tab Switcher) ── */
function switchTab(tab) {
    document.getElementById('tab-btn-login').classList.toggle('active', tab === 'login');
    document.getElementById('tab-btn-register').classList.toggle('active', tab === 'register');
    document.getElementById('pane-login').classList.toggle('active', tab === 'login');
    document.getElementById('pane-register').classList.toggle('active', tab === 'register');

    var header = document.querySelector('.login-card-header h3');
    var subtitle = document.querySelector('.login-card-header p');
    if (tab === 'register') {
        header.textContent = 'Đăng Ký';
        subtitle.textContent = 'Tạo tài khoản mới để bắt đầu';
    } else {
        header.textContent = 'Đăng Nhập';
        subtitle.textContent = 'Nhập tài khoản để truy cập hệ thống';
    }
}

/* ── Hàm 3: Toggle hiện/ẩn mật khẩu (Password Visibility Toggle) ── */
function togglePassword(inputId, btn) {
    var input = document.getElementById(inputId);
    var icon = btn.querySelector('i');
    if (input.type === 'password') {
        input.type = 'text';
        icon.classList.remove('bi-eye');
        icon.classList.add('bi-eye-slash');
    } else {
        input.type = 'password';
        icon.classList.remove('bi-eye-slash');
        icon.classList.add('bi-eye');
    }
}

/* ── Hàm 4: Client-side validation form đăng ký ── */
function validateRegisterForm() {
    var pass        = document.getElementById('reg-pass');
    var confirm     = document.getElementById('reg-confirmpass');
    var passError   = document.getElementById('reg-pass-error');
    var confirmError= document.getElementById('reg-confirmpass-error');
    var valid = true;

    // Reset lỗi về trạng thái sạch
    passError.textContent = '';
    confirmError.textContent = '';
    pass.classList.remove('input-error');
    confirm.classList.remove('input-error');

    // Kiểm tra độ dài mật khẩu tối thiểu 6 ký tự
    if (pass.value.length < 6) {
        passError.textContent = 'Mật khẩu phải có ít nhất 6 ký tự';
        pass.classList.add('input-error');
        pass.focus();
        valid = false;
    }

    // Kiểm tra mật khẩu xác nhận có khớp không
    if (valid && pass.value !== confirm.value) {
        confirmError.textContent = 'Mật khẩu xác nhận không khớp';
        confirm.classList.add('input-error');
        confirm.focus();
        valid = false;
    }

    return valid;
}
