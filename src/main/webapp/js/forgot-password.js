/*
 * forgot-password.js — Các bước OTP quên mật khẩu (forgot-password.jsp).
 * Được nạp bằng <script src> ở cuối forgot-password.jsp (tách từ script nhúng để dễ tìm / sửa).
 */
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

function validateResetForm() {
    var pass = document.getElementById('fp-pass');
    var confirm = document.getElementById('fp-confirm');
    var passError = document.getElementById('fp-pass-error');
    var confirmError = document.getElementById('fp-confirm-error');
    passError.textContent = '';
    confirmError.textContent = '';

    if (pass.value.length < 6) {
        passError.textContent = 'Mật khẩu phải có ít nhất 6 ký tự';
        pass.focus();
        return false;
    }
    if (pass.value !== confirm.value) {
        confirmError.textContent = 'Mật khẩu xác nhận không khớp';
        confirm.focus();
        return false;
    }
    return true;
}
