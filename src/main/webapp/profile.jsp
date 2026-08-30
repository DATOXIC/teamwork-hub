<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>

<c:set var="pageTitle" value="Hồ Sơ: ${profileUser.fullName} &bull; TeamWork Hub" scope="request" />
<jsp:include page="/includes/header.jsp" />
<jsp:include page="/includes/navbar.jsp" />

<div class="container py-4 my-auto">

    <!-- 1. Breadcrumb & Nút Quay lại -->
    <div class="d-flex align-items-center justify-content-between mb-4">
        <a href="${pageContext.request.contextPath}/project?action=list" 
           class="btn btn-outline-secondary btn-sm rounded-pill px-3 shadow-none d-flex align-items-center gap-1">
            <i class="bi bi-arrow-left"></i> Quay lại Không gian làm việc
        </a>
        <span class="badge bg-light text-secondary border rounded-pill px-3 py-1 fs-8">
            <i class="bi bi-shield-check text-success me-1"></i> Hồ sơ chuyên môn công khai
        </span>
    </div>

    <!-- 2. Thông báo Flash — UI-04: Floating Toast -->
    <jsp:include page="/includes/toast.jsp" />


    <!-- =========================================================================
         3. KHỐI 1: IDENTITY & CV HEADER (THÔNG TIN CHUYÊN MÔN)
         ========================================================================= -->
    <div class="card border-0 bg-white shadow-sm rounded-4 p-4 p-md-5 mb-4">
        <div class="row align-items-center g-4">
            
            <!-- Cột Trái: Avatar & Tên & Chuyên Môn -->
            <div class="col-12 col-md-8 d-flex flex-column flex-sm-row align-items-center align-items-sm-start gap-4 text-center text-sm-start">
                <div class="avatar-lg rounded-circle bg-primary text-white d-flex align-items-center justify-content-center fw-extrabold shadow-sm flex-shrink-0" 
                     style="width: 84px; height: 84px; font-size: 2.2rem; background: linear-gradient(135deg, #4f46e5 0%, #06b6d4 100%);">
                    <i class="bi bi-person-fill"></i>
                </div>
                
                <div class="flex-grow-1">
                    <div class="d-flex flex-wrap align-items-center justify-content-center justify-content-sm-start gap-2 mb-1">
                        <h3 class="fw-extrabold text-dark mb-0">${profileUser.fullName}</h3>
                        <span class="badge bg-primary text-white rounded-pill px-3 py-1 fs-8">
                            ${profileUser.role}
                        </span>
                    </div>
                    <p class="text-muted fs-8 mb-2">
                        @${profileUser.username} &bull; 
                        <c:choose>
                            <c:when test="${isOwner}">
                                <span class="text-dark fw-medium">${profileUser.email}</span>
                            </c:when>
                            <c:otherwise>
                                <span class="text-muted" title="Email liên lạc">${profileUser.email}</span>
                            </c:otherwise>
                        </c:choose>
                    </p>
                    
                    <!-- Bio giới thiệu bản thân -->
                    <p class="text-secondary fs-7 mb-3" style="max-width: 600px;">
                        ${not empty profileUser.bio ? profileUser.bio : 'Chưa có lời giới thiệu bản thân.'}
                    </p>

                    <!-- Dải Kỹ năng (Tech Stack) -->
                    <div class="d-flex flex-wrap align-items-center justify-content-center justify-content-sm-start gap-2 mb-3">
                        <c:choose>
                            <c:when test="${not empty profileUser.skillList}">
                                <c:forEach items="${profileUser.skillList}" var="sk">
                                    <span class="badge bg-primary-subtle text-primary border border-primary-subtle rounded-pill px-3 py-1 fs-8">
                                        <i class="bi bi-check-circle-fill me-1"></i> ${sk}
                                    </span>
                                </c:forEach>
                            </c:when>
                            <c:otherwise>
                                <span class="text-muted fs-8 fst-italic">Chưa cập nhật kỹ năng chuyên môn.</span>
                            </c:otherwise>
                        </c:choose>
                    </div>

                    <!-- Liên kết Mạng xã hội công việc (GitHub / LinkedIn) -->
                    <div class="d-flex align-items-center justify-content-center justify-content-sm-start gap-2">
                        <c:if test="${not empty profileUser.githubUrl}">
                            <a href="${profileUser.githubUrl}" target="_blank" class="btn btn-dark btn-sm rounded-pill px-3 fs-8 fw-semibold shadow-2xs">
                                <i class="bi bi-github me-1"></i> GitHub
                            </a>
                        </c:if>
                        <c:if test="${not empty profileUser.linkedinUrl}">
                            <a href="${profileUser.linkedinUrl}" target="_blank" class="btn btn-outline-primary btn-sm rounded-pill px-3 fs-8 fw-semibold shadow-2xs">
                                <i class="bi bi-linkedin me-1"></i> LinkedIn
                            </a>
                        </c:if>
                    </div>
                </div>
            </div>

            <!-- Cột Phải: Nút Hành Động (Chỉnh sửa hoặc Mời vào dự án) -->
            <div class="col-12 col-md-4 text-center text-md-end">
                <c:choose>
                    <%-- KỊCH BẢN 1: HỒ SƠ CHÍNH MÌNH ➔ NÚT CHỈNH SỬA --%>
                    <c:when test="${isOwner}">
                        <button type="button" class="btn btn-outline-primary px-4 py-2 rounded-pill fw-semibold shadow-sm fs-7" 
                                data-bs-toggle="modal" data-bs-target="#editProfileModal">
                            <i class="bi bi-pencil-square me-1"></i> Chỉnh sửa hồ sơ
                        </button>
                    </c:when>

                    <%-- KỊCH BẢN 2: HỒ SƠ NGƯỜI KHÁC ➔ NÚT MỜI NHANH (NẾU LÀ PM) --%>
                    <c:otherwise>
                        <c:if test="${not empty availableProjectsToInvite}">
                            <button type="button" class="btn btn-success px-4 py-2 rounded-pill fw-semibold shadow-sm fs-7" 
                                    data-bs-toggle="modal" data-bs-target="#quickInviteModal">
                                <i class="bi bi-person-plus-fill me-1"></i> + Mời vào Dự án của tôi
                            </button>
                        </c:if>
                    </c:otherwise>
                </c:choose>
            </div>

        </div>
    </div>

    <!-- =========================================================================
         4. KHỐI 2: BẢNG CHỈ SỐ NĂNG SUẤT REAL-TIME (DỮ LIỆU KHÁCH QUAN CSDL)
         ========================================================================= -->
    <div class="mb-4">
        <h5 class="fw-bold text-dark mb-3">
            <i class="bi bi-graph-up-arrow text-primary me-2"></i>Chỉ số Năng suất & Cống hiến trên Hệ thống
        </h5>

        <div class="row g-3">
            <!-- Chỉ số 1: Dự án tham gia -->
            <div class="col-6 col-md-3">
                <div class="stat-card-modern h-100 d-flex flex-column justify-content-between">
                    <div class="d-flex align-items-center justify-content-between mb-2">
                        <span class="fs-8 text-muted fw-semibold">Dự án tham gia</span>
                        <span class="p-1 bg-primary-subtle text-primary rounded-2 lh-1">
                            <i class="bi bi-folder2-open fs-7"></i>
                        </span>
                    </div>
                    <h3 class="fw-extrabold text-primary mb-0">${userProjects.size()}</h3>
                    <span class="fs-9 text-muted mt-1">không gian làm việc</span>
                </div>
            </div>

            <!-- Chỉ số 2: Task Lead -->
            <div class="col-6 col-md-3">
                <div class="stat-card-modern h-100 d-flex flex-column justify-content-between">
                    <div class="d-flex align-items-center justify-content-between mb-2">
                        <span class="fs-8 text-muted fw-semibold">Task Lead chủ trì</span>
                        <span class="p-1 bg-warning-subtle text-warning-emphasis rounded-2 lh-1">
                            <i class="bi bi-person-workspace fs-7"></i>
                        </span>
                    </div>
                    <h3 class="fw-extrabold text-warning mb-0">${leadTaskCount}</h3>
                    <span class="fs-9 text-muted mt-1">nhiệm vụ lớn</span>
                </div>
            </div>

            <!-- Chỉ số 3: Việc con hoàn thành -->
            <div class="col-6 col-md-3">
                <div class="stat-card-modern h-100 d-flex flex-column justify-content-between">
                    <div class="d-flex align-items-center justify-content-between mb-2">
                        <span class="fs-8 text-muted fw-semibold">Việc con đã xong</span>
                        <span class="p-1 bg-success-subtle text-success rounded-2 lh-1">
                            <i class="bi bi-check-all fs-7"></i>
                        </span>
                    </div>
                    <h3 class="fw-extrabold text-success mb-0">${completedSubTasks} / ${totalSubTasks}</h3>
                    <span class="fs-9 text-muted mt-1">việc hoàn tất</span>
                </div>
            </div>

            <!-- Chỉ số 4: Tỷ lệ hoàn thành -->
            <div class="col-6 col-md-3">
                <div class="stat-card-modern h-100 d-flex flex-column justify-content-between">
                    <div class="d-flex align-items-center justify-content-between mb-2">
                        <span class="fs-8 text-muted fw-semibold">Tỷ lệ hoàn thành</span>
                        <span class="p-1 bg-info-subtle text-info-emphasis rounded-2 lh-1">
                            <i class="bi bi-speedometer2 fs-7"></i>
                        </span>
                    </div>
                    <h3 class="fw-extrabold text-dark mb-0">${completionRate}%</h3>
                    <div class="project-progress-container mt-2">
                        <div class="project-progress-bar" data-progress="${completionRate}%" style="width: 0%;"></div>
                    </div>
                </div>
            </div>
        </div>
    </div>

    <!-- =========================================================================
         5. KHỐI 3: DANH SÁCH DỰ ÁN ĐÃ & ĐANG THAM GIA
         ========================================================================= -->
    <div class="mb-5">
        <h5 class="fw-bold text-dark mb-3">
            <i class="bi bi-kanban text-primary me-2"></i>Các Dự án đang tham gia (${userProjects.size()})
        </h5>

        <div class="row g-4">
            <c:forEach items="${userProjects}" var="p">
                <div class="col-12 col-md-6 col-lg-4">
                    <div class="project-card h-100 d-flex flex-column justify-content-between">
                        <div>
                            <div class="d-flex justify-content-between align-items-start mb-2">
                                <span class="badge bg-light text-secondary border rounded-pill px-2 py-1 fs-9 fw-bold">
                                    #${p.projectCode}
                                </span>
                                <span class="text-muted fs-9">${p.createdAt}</span>
                            </div>
                            <h6 class="fw-bold text-dark mb-1">${p.name}</h6>
                            <p class="text-secondary fs-8 mb-3 line-clamp-2">
                                ${not empty p.description ? p.description : 'Chưa có mô tả cho dự án này.'}
                            </p>
                        </div>
                        <div class="pt-2 border-top">
                            <div class="d-flex justify-content-between align-items-center fs-9 text-muted mb-1">
                                <span>Tiến độ dự án</span>
                                <span class="fw-bold text-dark">${p.progressPercentage}%</span>
                            </div>
                            <div class="project-progress-container mb-3">
                                <div class="project-progress-bar" data-progress="${p.progressPercentage}%" style="width: 0%;"></div>
                            </div>
                            <a href="${pageContext.request.contextPath}/task?action=list&projectId=${p.id}" 
                               class="btn btn-outline-primary btn-sm w-100 rounded-pill py-1 fs-8 fw-semibold">
                                Vào không gian dự án <i class="bi bi-arrow-right ms-1"></i>
                            </a>
                        </div>
                    </div>
                </div>
            </c:forEach>

            <c:if test="${empty userProjects}">
                <div class="col-12 text-center py-4 bg-white rounded-4 border">
                    <p class="text-muted fs-8 mb-0">Thành viên này hiện chưa tham gia dự án nào.</p>
                </div>
            </c:if>
        </div>
    </div>

</div>

<!-- =========================================================================
     6. MODAL 1: CHỈNH SỬA HỒ SƠ CÁ NHÂN (DÀNH RIÊNG CHO CHÍNH CHỦ)
     ========================================================================= -->
<c:if test="${isOwner}">
    <div class="modal fade" id="editProfileModal" tabindex="-1" aria-labelledby="editProfileModalLabel" aria-hidden="true">
        <div class="modal-dialog modal-dialog-centered modal-lg">
            <div class="modal-content border-0 shadow-lg rounded-4 p-2">
                <div class="modal-header border-0 pb-0">
                    <h5 class="modal-title fw-bold text-dark" id="editProfileModalLabel">
                        <i class="bi bi-pencil-square text-primary me-2"></i>Chỉnh sửa hồ sơ cá nhân
                    </h5>
                    <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                </div>

                <form action="${pageContext.request.contextPath}/profile" method="post">
                    <input type="hidden" name="action" value="update">
                    <input type="hidden" name="userId" value="${profileUser.id}">

                    <div class="modal-body py-3">
                        <div class="row g-3 mb-3">
                            <div class="col-12 col-md-6">
                                <label for="inputFullName" class="form-label fw-semibold fs-7 text-dark">Họ và tên <span class="text-danger">*</span></label>
                                <input type="text" class="form-control fs-7 rounded-3" id="inputFullName" name="fullName" value="${profileUser.fullName}" required>
                            </div>
                            <div class="col-12 col-md-6">
                                <label for="inputRole" class="form-label fw-semibold fs-7 text-dark">Chuyên môn / Chức danh</label>
                                <input type="text" class="form-control fs-7 rounded-3" id="inputRole" name="role" value="${profileUser.role}" placeholder="Ví dụ: Senior Backend Developer">
                            </div>
                        </div>

                        <div class="mb-3">
                            <label for="inputBio" class="form-label fw-semibold fs-7 text-dark d-flex justify-content-between align-items-center">
                                <span>Lời giới thiệu bản thân (Bio ngắn)</span>
                                <span class="text-muted fs-9 fw-normal">Tối đa 250 ký tự</span>
                            </label>
                            <textarea class="form-control fs-7 rounded-3" id="inputBio" name="bio" rows="3" maxlength="250" 
                                      placeholder="Mô tả ngắn gọn đam mê, chuyên môn và phong cách làm việc của bạn...">${profileUser.bio}</textarea>
                        </div>

                        <div class="mb-3">
                            <label for="inputSkills" class="form-label fw-semibold fs-7 text-dark">
                                Kỹ năng chuyên môn (Tech Stack)
                            </label>
                            <input type="text" class="form-control fs-7 rounded-3" id="inputSkills" name="skills" value="${profileUser.skills}" 
                                   placeholder="Cách nhau bằng dấu phẩy, ví dụ: Java, Jakarta EE, MySQL, Docker, RESTful API">
                            <div class="form-text fs-9 text-muted mt-1">
                                Gợi ý kỹ năng: <span class="badge bg-light text-dark border me-1 cursor-pointer" onclick="addSkill('Java')">+ Java</span>
                                <span class="badge bg-light text-dark border me-1 cursor-pointer" onclick="addSkill('MySQL')">+ MySQL</span>
                                <span class="badge bg-light text-dark border me-1 cursor-pointer" onclick="addSkill('Docker')">+ Docker</span>
                                <span class="badge bg-light text-dark border me-1 cursor-pointer" onclick="addSkill('UI/UX')">+ UI/UX</span>
                                <span class="badge bg-light text-dark border me-1 cursor-pointer" onclick="addSkill('Spring Boot')">+ Spring Boot</span>
                            </div>
                        </div>

                        <div class="row g-3">
                            <div class="col-12 col-md-6">
                                <label for="inputGithub" class="form-label fw-semibold fs-7 text-dark">Liên kết GitHub</label>
                                <input type="url" class="form-control fs-7 rounded-3" id="inputGithub" name="githubUrl" value="${profileUser.githubUrl}" placeholder="https://github.com/username">
                            </div>
                            <div class="col-12 col-md-6">
                                <label for="inputLinkedin" class="form-label fw-semibold fs-7 text-dark">Liên kết LinkedIn</label>
                                <input type="url" class="form-control fs-7 rounded-3" id="inputLinkedin" name="linkedinUrl" value="${profileUser.linkedinUrl}" placeholder="https://linkedin.com/in/username">
                            </div>
                        </div>
                    </div>

                    <div class="modal-footer border-0 pt-0">
                        <button type="button" class="btn btn-light rounded-pill px-4 fs-7 fw-semibold" data-bs-dismiss="modal">Hủy</button>
                        <button type="submit" class="btn btn-primary-custom rounded-pill px-4 fs-7 fw-semibold shadow-sm">
                            <i class="bi bi-check-lg me-1"></i> Lưu thay đổi
                        </button>
                    </div>
                </form>
            </div>
        </div>
    </div>

    <script>
        function addSkill(skillName) {
            var input = document.getElementById('inputSkills');
            if (input.value.trim() === '') {
                input.value = skillName;
            } else {
                if (!input.value.includes(skillName)) {
                    input.value = input.value.trim() + ', ' + skillName;
                }
            }
        }
    </script>
</c:if>

<!-- =========================================================================
     7. MODAL 2: MỜI NHANH VÀO DỰ ÁN CỦA TÔI (DÀNH CHO PM KHI XEM HỒ SƠ NGƯỜI KHÁC)
     ========================================================================= -->
<c:if test="${not isOwner && not empty availableProjectsToInvite}">
    <div class="modal fade" id="quickInviteModal" tabindex="-1" aria-labelledby="quickInviteModalLabel" aria-hidden="true">
        <div class="modal-dialog modal-dialog-centered">
            <div class="modal-content border-0 shadow-lg rounded-4 p-2">
                <div class="modal-header border-0 pb-0">
                    <h5 class="modal-title fw-bold text-dark" id="quickInviteModalLabel">
                        <i class="bi bi-person-plus-fill text-success me-2"></i>Mời ${profileUser.fullName} vào Dự án
                    </h5>
                    <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                </div>

                <form action="${pageContext.request.contextPath}/invite" method="post">
                    <input type="hidden" name="action" value="sendInvite">
                    <input type="hidden" name="usernameOrEmail" value="${profileUser.username}">

                    <div class="modal-body py-3">
                        <p class="text-muted fs-8 mb-3">
                            Chọn dự án bạn muốn mời <strong>${profileUser.fullName}</strong> tham gia. Lời mời sẽ có hiệu lực trong <strong>7 ngày</strong>.
                        </p>

                        <div class="mb-3">
                            <label for="selectProject" class="form-label fw-semibold fs-7 text-dark">
                                Chọn dự án của bạn <span class="text-danger">*</span>
                            </label>
                            <select class="form-select fs-7 rounded-3" id="selectProject" name="projectId" required>
                                <c:forEach items="${availableProjectsToInvite}" var="ap">
                                    <option value="${ap.id}">[${ap.projectCode}] ${ap.name}</option>
                                </c:forEach>
                            </select>
                        </div>
                    </div>

                    <div class="modal-footer border-0 pt-0">
                        <button type="button" class="btn btn-light rounded-pill px-4 fs-7 fw-semibold" data-bs-dismiss="modal">Hủy</button>
                        <button type="submit" class="btn btn-success rounded-pill px-4 fs-7 fw-semibold shadow-sm">
                            <i class="bi bi-send-fill me-1"></i> Gửi lời mời ngay
                        </button>
                    </div>
                </form>
            </div>
        </div>
    </div>
</c:if>

<jsp:include page="/includes/footer.jsp" />
