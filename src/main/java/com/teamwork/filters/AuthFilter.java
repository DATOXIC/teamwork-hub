package com.teamwork.filters;

import com.teamwork.business.User;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;

/**
 * Filter Bảo Mật Trung Tâm (Authentication Filter / Security Middleware)
 * 
 * NHIỆM VỤ CỐT LÕI:
 * Đứng gác tại cổng vào duy nhất của toàn bộ ứng dụng (/*).
 * - Nếu người dùng vào các trang công khai (Đăng nhập, Đăng ký, CSS, JS, Trang chủ) -> Cho qua.
 * - Nếu người dùng vào các trang nội bộ (/project, /task, /doc, /chat) mà CHƯA ĐĂNG NHẬP -> Chặn lại và chuyển hướng về trang Đăng nhập.
 * 
 * LỢI ÍCH: Tất cả các Servlet (ProjectServlet, TaskServlet...) không cần phải viết code kiểm tra đăng nhập lặp đi lặp lại nữa!
 */
public class AuthFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) throws ServletException 
    {
        // Khởi tạo Filter khi Server khởi động (nếu cần cấu hình ban đầu)
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        // 1. Ép kiểu ServletRequest/ServletResponse sang chuẩn HttpServletRequest/HttpServletResponse
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // 2. Thiết lập bảng mã UTF-8 cho toàn bộ request/response đi qua cổng
        httpRequest.setCharacterEncoding("UTF-8");
        httpResponse.setContentType("text/html;charset=UTF-8");

        // 3. Lấy đường dẫn URL tương đối mà người dùng đang truy cập
        // Ví dụ: URL đầy đủ là "/teamwork-hub/project" -> path nhận được sẽ là "/project"
        String uri = httpRequest.getRequestURI();
        String contextPath = httpRequest.getContextPath();
        String path = uri.substring(contextPath.length());

        // 4. DANH SÁCH CÁC ĐƯỜNG DẪN CÔNG KHAI (WHITELIST) - AI CŨNG ĐƯỢC PHÉP TRUY CẬP
        boolean isPublicPath = false;

        if (path.equals("") || path.equals("/") || path.equals("/index.jsp")) {
            isPublicPath = true; // Trang chủ giới thiệu
        } else if (path.startsWith("/auth")) {
            isPublicPath = true; // Trang đăng nhập, đăng ký, đăng xuất (AuthServlet)
        } else if (path.equals("/login.jsp")) {
            isPublicPath = true; // Giao diện form đăng nhập
        } else if (path.startsWith("/styles/") || path.startsWith("/js/") || path.startsWith("/images/") || path.startsWith("/includes/")) {
            isPublicPath = true; // Các tài nguyên tĩnh (CSS, JavaScript, Logo, Footer/Header fragment)
        } else if (path.equals("/404.jsp") || path.equals("/500.jsp")) {
            isPublicPath = true; // Các trang lỗi tùy chỉnh
        }

        // 5. NẾU LÀ ĐƯỜNG DẪN CÔNG KHAI -> MỞ CỔNG CHO ĐI TIẾP
        if (isPublicPath) {
            chain.doFilter(request, response);
            return;
        }

        // 6. NẾU LÀ ĐƯỜNG DẪN BẢO MẬT (/project, /task, /doc, /chat...) -> KIỂM TRA ĐĂNG NHẬP
        HttpSession session = httpRequest.getSession(false);
        User currentUser = null;

        if (session != null) {
            currentUser = (User) session.getAttribute("currentUser");
        }

        // 7. KIỂM TRA KẾT QUẢ ĐĂNG NHẬP
        if (currentUser != null) {
            // Đã đăng nhập hợp lệ -> Cho phép đi tiếp vào các Servlet / JSP nội bộ
            chain.doFilter(request, response);
        } 
        else 
        {
            // Chưa đăng nhập -> Chặn lại và chuyển hướng về trang Đăng nhập
            httpResponse.sendRedirect(contextPath + "/auth?action=viewLogin");
        }
    }

    @Override
    public void destroy() 
    {
        // Hủy Filter khi Server tắt
    }
}
