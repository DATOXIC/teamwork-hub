<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%-- Thiết lập tiêu đề trang cho header.jsp --%>
<c:set var="pageTitle" value="Đăng nhập &bull; TeamWork Hub" />
<jsp:include page="/includes/header.jsp" />
<jsp:include page="/includes/navbar.jsp" />

<div class="container py-5 my-auto">
    <div class="row justify-content-center">
        <div class="col-12 col-md-8 col-lg-5">

            <!-- Card Xác thực (Phong cách Basecamp / Notion) -->
            <div class="card auth-card bg-white p-4 p-md-5">
                
                <!-- Header Card -->
                <div class="text-center mb-4">
                    <div class="brand-icon mx-auto mb-3 d-flex align-items-center justify-content-center" style="width: 48px; height: 48px; font-size: 1.5rem;">
                        <i class="bi bi-grid-1x2-fill"></i>
                    </div>
                    <h3 class="fw-bold text-dark mb-1">Chào mừng bạn!</h3>
                    <p class="text-muted fs-7">Không gian làm việc nhóm tập trung và hiệu quả</p>
                </div>

                <!-- Thông báo thành công (sau khi Đăng ký) -->
                <div class="alert alert-success alert-dismissible fade show fs-7 py-2 ${not empty successMessage ? '' : 'd-none'}" role="alert">
                    <i class="bi bi-check-circle-fill me-1"></i> ${successMessage}
                </div>

                <!-- Tab chuyển đổi: ĐĂNG NHẬP / ĐĂNG KÝ -->
                <ul class="nav nav-pills nav-fill bg-light p-1 rounded-pill mb-4" id="authTab" role="tablist">
                    <li class="nav-item" role="presentation">
                        <button class="nav-link rounded-pill py-2 fs-7 ${activeTab != 'register' ? 'active' : ''}" 
                                id="login-tab" data-bs-toggle="pill" data-bs-target="#login-pane" type="button" role="tab">
                            <i class="bi bi-box-arrow-in-right me-1"></i> Đăng nhập
                        </button>
                    </li>
                    <li class="nav-item" role="presentation">
                        <button class="nav-link rounded-pill py-2 fs-7 ${activeTab == 'register' ? 'active' : ''}" 
                                id="register-tab" data-bs-toggle="pill" data-bs-target="#register-pane" type="button" role="tab">
                            <i class="bi bi-person-plus me-1"></i> Đăng ký
                        </button>
                    </li>
                </ul>

                <!-- Tab Content -->
                <div class="tab-content" id="authTabContent">

                    <!-- ================= TAB 1: FORM ĐĂNG NHẬP ================= -->
                    <div class="tab-pane fade ${activeTab != 'register' ? 'show active' : ''}" id="login-pane" role="tabpanel">
                        <!-- Thông báo lỗi đăng nhập -->
                        <div class="alert alert-danger py-2 fs-7 ${not empty errorMessage ? '' : 'd-none'}" role="alert">
                            <i class="bi bi-exclamation-triangle-fill me-1"></i> ${errorMessage}
                        </div>

                        <form action="${pageContext.request.contextPath}/auth" method="post">
                            <input type="hidden" name="action" value="login">

                            <div class="mb-3">
                                <label for="login-username" class="form-label fw-semibold fs-7 text-dark">Tên đăng nhập</label>
                                <div class="input-group">
                                    <span class="input-group-text bg-light text-muted border-end-0"><i class="bi bi-person"></i></span>
                                    <input type="text" class="form-control border-start-0 ps-0" id="login-username" name="username" 
                                           value="${username}" placeholder="Nhập username của bạn" required autofocus>
                                </div>
                            </div>

                            <div class="mb-3">
                                <label for="login-password" class="form-label fw-semibold fs-7 text-dark">Mật khẩu</label>
                                <div class="input-group">
                                    <span class="input-group-text bg-light text-muted border-end-0"><i class="bi bi-lock"></i></span>
                                    <input type="password" class="form-control border-start-0 ps-0" id="login-password" name="password" 
                                           placeholder="Nhập mật khẩu" required>
                                </div>
                            </div>

                            <button type="submit" class="btn btn-primary-custom w-100 py-2 fw-semibold rounded-3 mt-2">
                                Đăng nhập vào hệ thống <i class="bi bi-arrow-right ms-1"></i>
                            </button>
                        </form>

                        <!-- Box gợi ý tài khoản mẫu để test nhanh -->
                        <div class="bg-light p-3 rounded-3 mt-4 border">
                            <div class="fw-semibold text-secondary fs-8 mb-1"><i class="bi bi-key-fill text-warning me-1"></i> Tài khoản mẫu để test nhanh:</div>
                            <div class="d-flex justify-content-between text-muted fs-8">
                                <span>Trưởng nhóm: <code>admin</code> / <code>admin123</code></span>
                                <span>Thành viên: <code>member1</code> / <code>pass123</code></span>
                            </div>
                        </div>
                    </div>

                    <!-- ================= TAB 2: FORM ĐĂNG KÝ ================= -->
                    <div class="tab-pane fade ${activeTab == 'register' ? 'show active' : ''}" id="register-pane" role="tabpanel">
                        <!-- Thông báo lỗi đăng ký -->
                        <div class="alert alert-danger py-2 fs-7 ${not empty regError ? '' : 'd-none'}" role="alert">
                            <i class="bi bi-exclamation-triangle-fill me-1"></i> ${regError}
                        </div>

                        <form action="${pageContext.request.contextPath}/auth" method="post">
                            <input type="hidden" name="action" value="register">

                            <div class="mb-3">
                                <label for="reg-fullname" class="form-label fw-semibold fs-7 text-dark">Họ và tên</label>
                                <input type="text" class="form-control" id="reg-fullname" name="fullName" 
                                       value="${regFullName}" placeholder="Ví dụ: Nguyễn Văn A" required>
                            </div>

                            <div class="mb-3">
                                <label for="reg-username" class="form-label fw-semibold fs-7 text-dark">Tên đăng nhập</label>
                                <input type="text" class="form-control" id="reg-username" name="username" 
                                       value="${regUsername}" placeholder="Tối thiểu 4 ký tự" required>
                            </div>

                            <div class="mb-3">
                                <label for="reg-email" class="form-label fw-semibold fs-7 text-dark">Email liên hệ</label>
                                <input type="email" class="form-control" id="reg-email" name="email" 
                                       value="${regEmail}" placeholder="name@example.com">
                            </div>

                            <div class="row g-2 mb-3">
                                <div class="col-6">
                                    <label for="reg-pass" class="form-label fw-semibold fs-7 text-dark">Mật khẩu</label>
                                    <input type="password" class="form-control" id="reg-pass" name="password" 
                                           placeholder="&ge; 6 ký tự" required>
                                </div>
                                <div class="col-6">
                                    <label for="reg-confirmpass" class="form-label fw-semibold fs-7 text-dark">Xác nhận</label>
                                    <input type="password" class="form-control" id="reg-confirmpass" name="confirmPassword" 
                                           placeholder="Nhập lại mật khẩu" required>
                                </div>
                            </div>

                            <button type="submit" class="btn btn-primary-custom w-100 py-2 fw-semibold rounded-3 mt-2">
                                <i class="bi bi-person-check me-1"></i> Tạo tài khoản mới
                            </button>
                        </form>
                    </div>

                </div>

            </div>

        </div>
    </div>
</div>

<jsp:include page="/includes/footer.jsp" />