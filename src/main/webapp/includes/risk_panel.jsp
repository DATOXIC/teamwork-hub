<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<c:set var="riskCount" value="0" />
<%-- ◀ SERVLET: TaskBoardHandler → setAttribute("todoTasks") --%>
<%-- ◀ SERVLET: TaskBoardHandler → setAttribute("taskHealthMap") --%>
<c:forEach items="${todoTasks}" var="t"><c:if test="${taskHealthMap[t.id].level == 'risk'}"><c:set var="riskCount" value="${riskCount + 1}" /></c:if></c:forEach>
<%-- ◀ SERVLET: TaskBoardHandler → setAttribute("inProgressTasks") --%>
<c:forEach items="${inProgressTasks}" var="t"><c:if test="${taskHealthMap[t.id].level == 'risk'}"><c:set var="riskCount" value="${riskCount + 1}" /></c:if></c:forEach>
<c:if test="${riskCount > 0}">
    <div class="risk-panel" id="riskPanel">
        <div class="risk-panel-head">
            <i class="bi bi-exclamation-octagon-fill"></i>
            <strong>Đang có nguy cơ trễ</strong>
            <span class="risk-panel-count">${riskCount}</span>
            <span class="risk-panel-hint">Bấm vào task để xử lý ngay</span>
        </div>
        <div class="risk-panel-list">
            <c:forEach items="${todoTasks}" var="t">
                <c:if test="${taskHealthMap[t.id].level == 'risk'}">
                    <a href="#task-${t.id}" class="risk-item" onclick="openClickUpTask(${t.id}); return false;">
                        <span class="risk-item-title"><c:out value="${t.title}" /></span>
                        <span class="risk-item-meta"><c:out value="${t.assigneeName}" /> · <c:out value="${taskHealthMap[t.id].reason}" /></span>
                        <span class="health-badge health-risk">${taskHealthMap[t.id].score}</span>
                    </a>
                </c:if>
            </c:forEach>
            <c:forEach items="${inProgressTasks}" var="t">
                <c:if test="${taskHealthMap[t.id].level == 'risk'}">
                    <a href="#task-${t.id}" class="risk-item" onclick="openClickUpTask(${t.id}); return false;">
                        <span class="risk-item-title"><c:out value="${t.title}" /></span>
                        <span class="risk-item-meta"><c:out value="${t.assigneeName}" /> · <c:out value="${taskHealthMap[t.id].reason}" /></span>
                        <span class="health-badge health-risk">${taskHealthMap[t.id].score}</span>
                    </a>
                </c:if>
            </c:forEach>
        </div>
    </div>
</c:if>
