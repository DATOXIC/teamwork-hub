package com.teamwork.util;

/**
 * Tiện ích đọc tham số request dùng chung cho servlet ({@code BaseServlet}) và các handler tĩnh của /task.
 */
public final class RequestUtil {

    private RequestUtil() {}

    /** Chuyển chuỗi thành số nguyên; null, rỗng hoặc sai định dạng thì trả {@code defaultValue}. */
    public static int parseInt(String value, int defaultValue) {
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
