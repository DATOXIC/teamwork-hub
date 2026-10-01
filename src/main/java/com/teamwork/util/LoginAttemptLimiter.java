package com.teamwork.util;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Giới hạn số lần đăng nhập sai (chống dò mật khẩu).
 *
 * <p>Sai {@link #MAX_FAILS} lần liên tiếp với cùng một khóa (username + IP) → khóa {@link #LOCK_MS}.
 * Các lần sai cách nhau quá {@link #WINDOW_MS} thì không cộng dồn. Dữ liệu lưu trong bộ nhớ
 * server, nên khởi động lại server sẽ xóa trạng thái khóa.
 *
 * <p>Mọi hàm nhận thời gian {@code now} (ms) làm tham số để dễ kiểm thử.
 */
public class LoginAttemptLimiter {

    public static final int MAX_FAILS = 5;
    public static final long LOCK_MS = 5 * 60 * 1000L;
    public static final long WINDOW_MS = 15 * 60 * 1000L;

    private static final int PURGE_THRESHOLD = 10_000;

    private static final class Entry {
        int fails;
        long lastFailAt;
        long lockedUntil;
    }

    private final ConcurrentHashMap<String, Entry> entries = new ConcurrentHashMap<>();

    /** Khóa theo username (không phân biệt hoa thường) và địa chỉ IP. */
    public static String key(String username, String ip) {
        String u = username == null ? "" : username.trim().toLowerCase();
        return u + "|" + (ip == null ? "" : ip);
    }

    /** @return số giây còn bị khóa, hoặc 0 nếu được phép thử đăng nhập. */
    public long lockedSeconds(String key, long now) {
        Entry e = entries.get(key);
        if (e == null) {
            return 0;
        }
        synchronized (e) {
            long remainMs = e.lockedUntil - now;
            return remainMs > 0 ? (remainMs + 999) / 1000 : 0;
        }
    }

    /** Ghi nhận một lần đăng nhập sai. @return true nếu lần này làm khóa tài khoản. */
    public boolean recordFailure(String key, long now) {
        if (entries.size() > PURGE_THRESHOLD) {
            purge(now);
        }
        Entry e = entries.computeIfAbsent(key, k -> new Entry());
        synchronized (e) {
            if (now - e.lastFailAt > WINDOW_MS || (e.lockedUntil != 0 && now >= e.lockedUntil)) {
                e.fails = 0; // hết cửa sổ hoặc đã hết khóa → đếm lại
                e.lockedUntil = 0;
            }
            e.fails++;
            e.lastFailAt = now;
            if (e.fails >= MAX_FAILS) {
                e.lockedUntil = now + LOCK_MS;
                return true;
            }
            return false;
        }
    }

    /** Đăng nhập thành công → xóa bộ đếm. */
    public void reset(String key) {
        entries.remove(key);
    }

    private void purge(long now) {
        entries.entrySet().removeIf(en -> now - en.getValue().lastFailAt > WINDOW_MS
                && now >= en.getValue().lockedUntil);
    }
}
