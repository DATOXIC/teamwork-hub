package com.teamwork.controllers;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Điểm xử lý lỗi 500 chung (khai báo trong web.xml {@code <error-page>}).
 *
 * <p>Mỗi lỗi được cấp một MÃ LỖI ngắn (ví dụ {@code E-7K2QX9}): mã ghi vào log máy chủ cùng stack trace,
 * và hiện cho người dùng. Người dùng báo mã → nhóm tìm đúng dòng log, mà không lộ stack trace ra trình duyệt.</p>
 *
 * <ul>
 *   <li>Request AJAX (fetch / XMLHttpRequest / Accept JSON) → trả JSON {@code {success:false, ok:false, message, errorId}}.</li>
 *   <li>Request thường → hiển thị 500.jsp kèm mã lỗi.</li>
 * </ul>
 */
@WebServlet("/error")
public class ErrorServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(ErrorServlet.class.getName());
    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; // bỏ 0/O, 1/I cho dễ đọc
    private static final SecureRandom RANDOM = new SecureRandom();

    static String newErrorId() {
        StringBuilder sb = new StringBuilder("E-");
        for (int i = 0; i < 6; i++) {
            sb.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }

    static boolean wantsJson(HttpServletRequest request) {
        String xrw = request.getHeader("X-Requested-With");
        String accept = request.getHeader("Accept");
        return "XMLHttpRequest".equalsIgnoreCase(xrw)
                || (accept != null && accept.contains("application/json"))
                || "true".equals(request.getParameter("ajax"))
                || "true".equals(request.getParameter("isAjax"));
    }

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String errorId = newErrorId();
        Throwable error = (Throwable) request.getAttribute(RequestDispatcher.ERROR_EXCEPTION);
        Object uri = request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);

        if (status != null) { // gõ thẳng /error trên trình duyệt thì không có lỗi nào để ghi
            LOGGER.log(Level.SEVERE, "[" + errorId + "] Lỗi " + status + " tại " + request.getMethod() + " " + uri, error);
        }

        if (response.isCommitted()) {
            return;
        }
        response.resetBuffer();
        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);

        if (wantsJson(request)) {
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write("{\"success\":false,\"ok\":false,\"errorId\":\"" + errorId
                    + "\",\"message\":\"Máy chủ gặp lỗi (mã " + errorId + "). Vui lòng thử lại hoặc báo mã này cho nhóm.\"}");
            return;
        }

        // ▶ JSP: 500.jsp đọc bằng ${errorId}
        request.setAttribute("errorId", errorId);
        request.getRequestDispatcher("/500.jsp").forward(request, response);
    }
}
