<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<%-- ◀ SERVLET: TaskBoardHandler → setAttribute("taskHealthMap") --%>
<c:set var="_h" value="${taskHealthMap[task.id]}" />
<c:if test="${not empty _h and _h.level != 'none'}">
    <span class="health-badge health-${_h.level}" title="<c:out value='${_h.reason}' />">
        <i class="bi ${_h.level == 'risk' ? 'bi-exclamation-octagon-fill' : _h.level == 'warn' ? 'bi-exclamation-triangle-fill' : 'bi-heart-pulse-fill'}"></i>
        ${_h.score}
    </span>
</c:if>
