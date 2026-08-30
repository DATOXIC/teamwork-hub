<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%--
    UI-04: Floating Toast Data Carrier
    Nhúng dữ liệu thông báo Flash vào DOM để app.js hiển thị Floating Toast tự động.
--%>
<c:set var="tSuccess" value="${not empty requestScope.toastSuccess ? requestScope.toastSuccess : sessionScope.toastSuccess}" />
<c:set var="tError" value="${not empty requestScope.toastError ? requestScope.toastError : sessionScope.toastError}" />

<div id="toastData" class="d-none"
     data-success="<c:out value='${tSuccess}' />"
     data-error="<c:out value='${tError}' />">
</div>

<%
    // Xóa Flash Message khỏi Session sau khi đã nhúng vào HTML để không bị lặp lại khi người dùng refresh F5
    if (session.getAttribute("toastSuccess") != null) {
        session.removeAttribute("toastSuccess");
    }
    if (session.getAttribute("toastError") != null) {
        session.removeAttribute("toastError");
    }
%>

