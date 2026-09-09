-- =============================================================================
-- TEAMWORK-HUB — PostgreSQL Schema cho Supabase
-- Phiên bản: 1.0.0
-- Tác giả: Antigravity (dựa trên phân tích toàn bộ Java Model)
--
-- THIẾT KẾ ĐI NGƯỢC TỪ TRUY VẤN:
--   Q1. Lấy Task theo Project + lọc status, priority, assignee, label, deadline
--       → Index trên (project_id, status), (assignee_id), (due_date)
--   Q2. Kiểm tra quyền User trong Project (OWNER / MEMBER)
--       → Index UNIQUE trên (project_id, user_id)
--   Q3. Lấy SubTask theo Task cha
--       → Index trên (task_id)
--   Q4. Lấy Notification chưa đọc của User theo thời gian mới nhất
--       → Index trên (recipient_id, is_read, created_at DESC)
--   Q5. Lấy Message Chat theo Project, phân trang thời gian
--       → Index trên (project_id, task_id, sent_at DESC)
--   Q6. Lấy Invite/JoinRequest theo người nhận + trạng thái PENDING
--       → Index trên (receiver_id, status)
--   Q7. Kiểm tra projectCode không trùng → UNIQUE trên projects.project_code
--   Q8. Đăng nhập theo username hoặc email → UNIQUE trên cả 2
--
-- FOREIGN KEY HÀNH VI XÓA (được xác định rõ ràng, không để mặc định):
--   - Xóa Project → CASCADE xóa tất cả Member, Task, SubTask, Doc, Message, Invite, Label
--   - Xóa Task    → CASCADE xóa SubTask, TaskDoc, Message bình luận
--   - Xóa User    → RESTRICT (không cho xóa nếu còn là owner project)
--   - Xóa Doc     → CASCADE xóa liên kết TaskDoc
-- =============================================================================

-- ==========================================
-- DROP (theo thứ tự phụ thuộc ngược — bảng con trước, bảng cha sau)
-- ==========================================
DROP TABLE IF EXISTS activity_logs    CASCADE;
DROP TABLE IF EXISTS notifications    CASCADE;
DROP TABLE IF EXISTS task_docs        CASCADE;
DROP TABLE IF EXISTS messages         CASCADE;
DROP TABLE IF EXISTS subtasks         CASCADE;
DROP TABLE IF EXISTS labels           CASCADE;
DROP TABLE IF EXISTS tasks            CASCADE;
DROP TABLE IF EXISTS docs             CASCADE;
DROP TABLE IF EXISTS project_invites  CASCADE;
DROP TABLE IF EXISTS project_members  CASCADE;
DROP TABLE IF EXISTS projects         CASCADE;
DROP TABLE IF EXISTS users            CASCADE;

-- ==========================================
-- DROP TYPES
-- ==========================================
DROP TYPE IF EXISTS task_status_enum       CASCADE;
DROP TYPE IF EXISTS subtask_status_enum    CASCADE;
DROP TYPE IF EXISTS priority_enum          CASCADE;
DROP TYPE IF EXISTS project_role_enum      CASCADE;
DROP TYPE IF EXISTS invite_type_enum       CASCADE;
DROP TYPE IF EXISTS invite_status_enum     CASCADE;
DROP TYPE IF EXISTS notification_type_enum CASCADE;
DROP TYPE IF EXISTS label_color_enum       CASCADE;

-- ==========================================
-- ENUM TYPES (Tường minh hơn VARCHAR — DB tự kiểm tra giá trị hợp lệ)
-- ==========================================
CREATE TYPE task_status_enum AS ENUM (
    'TODO',         -- Cần làm (đang lập kế hoạch)
    'PLANNING',     -- Chờ PM duyệt kế hoạch phân rã
    'IN_PROGRESS',  -- Đang làm (đã khóa kế hoạch)
    'SUBMITTED',    -- Đã nộp, chờ PM duyệt nghiệm thu
    'REVISE',       -- PM yêu cầu cân chỉnh nhỏ
    'REJECTED',     -- PM từ chối — chưa đạt yêu cầu
    'DONE',         -- Đã nghiệm thu hoàn tất
    'APPROVED'      -- Alias của DONE (tương thích ngược Java code)
);

CREATE TYPE subtask_status_enum AS ENUM (
    'TODO',       -- Đang làm
    'SUBMITTED',  -- Cấp dưới đã nộp, chờ Task Lead duyệt
    'REVISE',     -- Task Lead yêu cầu cân chỉnh
    'REJECTED',   -- Task Lead từ chối — chưa đạt
    'APPROVED'    -- Đã nghiệm thu đạt
);

CREATE TYPE priority_enum          AS ENUM ('HIGH', 'MEDIUM', 'LOW');
CREATE TYPE project_role_enum      AS ENUM ('OWNER', 'MEMBER');
CREATE TYPE invite_type_enum       AS ENUM ('INVITATION', 'JOIN_REQUEST');
CREATE TYPE invite_status_enum     AS ENUM ('PENDING', 'ACCEPTED', 'REJECTED', 'REVOKED', 'EXPIRED');
CREATE TYPE notification_type_enum AS ENUM ('INVITE', 'TASK_ASSIGNED', 'PROGRESS', 'COMMENT', 'GENERAL');
CREATE TYPE label_color_enum       AS ENUM ('red', 'blue', 'purple', 'amber', 'green', 'pink', 'cyan', 'slate');


-- ============================================================
-- BẢNG 1: users
-- Truy vấn chính: đăng nhập bằng username hoặc email (Q8)
-- ============================================================
CREATE TABLE users (
    id            SERIAL       PRIMARY KEY,
    username      VARCHAR(50)  NOT NULL,
    password      VARCHAR(255) NOT NULL,   -- TODO: nâng cấp sang bcrypt hash khi tích hợp Auth chuẩn
    full_name     VARCHAR(150) NOT NULL,
    email         VARCHAR(150) NOT NULL,
    role          VARCHAR(100) NOT NULL DEFAULT 'Developer',
    avatar        TEXT         NOT NULL DEFAULT 'images/default_avatar.png',
    bio           VARCHAR(500) NOT NULL DEFAULT '',
    skills        TEXT         NOT NULL DEFAULT '',   -- "Java, MySQL, Docker" — phân cách bằng phẩy
    github_url    TEXT         NOT NULL DEFAULT '',
    linkedin_url  TEXT         NOT NULL DEFAULT '',
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_users_username UNIQUE (username),
    CONSTRAINT uq_users_email    UNIQUE (email),
    CONSTRAINT chk_users_username_len CHECK (char_length(username) BETWEEN 3 AND 50),
    CONSTRAINT chk_users_email_format CHECK (email LIKE '%@%')
);

CREATE INDEX idx_users_username ON users (username);
CREATE INDEX idx_users_email    ON users (email);

COMMENT ON TABLE  users          IS 'Người dùng hệ thống — xác thực và hồ sơ cá nhân';
COMMENT ON COLUMN users.skills   IS 'Danh sách kỹ năng phân cách bằng dấu phẩy: Java, MySQL, Docker';
COMMENT ON COLUMN users.password IS 'Mật khẩu — NÂNG CẤP sang bcrypt hash khi tích hợp Auth chuẩn';


-- ============================================================
-- BẢNG 2: projects
-- Truy vấn chính: lấy project của user, kiểm tra projectCode (Q7)
-- ============================================================
CREATE TABLE projects (
    id            SERIAL       PRIMARY KEY,
    project_code  VARCHAR(30)  NOT NULL,  -- Mã duy nhất: TW-HUB-01 (dùng luồng xin gia nhập Chiều 2)
    name          VARCHAR(200) NOT NULL,
    description   TEXT         NOT NULL DEFAULT '',
    owner_id      INT          NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    -- RESTRICT: không cho xóa user đang là owner project
    CONSTRAINT fk_projects_owner
        FOREIGN KEY (owner_id) REFERENCES users (id)
        ON DELETE RESTRICT ON UPDATE CASCADE,

    CONSTRAINT uq_projects_code   UNIQUE (project_code),
    CONSTRAINT chk_projects_code_len CHECK (char_length(project_code) BETWEEN 2 AND 30),
    CONSTRAINT chk_projects_name_len CHECK (char_length(name) >= 1)
);

CREATE INDEX idx_projects_owner_id ON projects (owner_id);
CREATE INDEX idx_projects_code     ON projects (project_code);

COMMENT ON TABLE  projects              IS 'Dự án — đơn vị tổ chức công việc chính';
COMMENT ON COLUMN projects.project_code IS 'Mã ngắn duy nhất dùng luồng xin gia nhập (Chiều 2)';
-- Ghi chú thiết kế: total_tasks và done_tasks trong Java là COMPUTED, không lưu DB
-- Java tính: SELECT COUNT(*), COUNT(*) FILTER (WHERE status=''DONE'') FROM tasks WHERE project_id=?


-- ============================================================
-- BẢNG 3: project_members  (Quan hệ Nhiều-Nhiều User ↔ Project)
-- Truy vấn chính: kiểm tra quyền trong project (Q2)
-- ============================================================
CREATE TABLE project_members (
    project_id   INT               NOT NULL,
    user_id      INT               NOT NULL,
    project_role project_role_enum NOT NULL DEFAULT 'MEMBER',
    joined_at    TIMESTAMPTZ       NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_project_members PRIMARY KEY (project_id, user_id),

    -- Xóa Project → xóa toàn bộ thành viên (CASCADE)
    CONSTRAINT fk_pm_project
        FOREIGN KEY (project_id) REFERENCES projects (id)
        ON DELETE CASCADE ON UPDATE CASCADE,

    -- RESTRICT: không cho xóa user đang là thành viên project
    CONSTRAINT fk_pm_user
        FOREIGN KEY (user_id) REFERENCES users (id)
        ON DELETE RESTRICT ON UPDATE CASCADE
);

CREATE INDEX idx_pm_user_id    ON project_members (user_id);
CREATE INDEX idx_pm_project_id ON project_members (project_id);

COMMENT ON TABLE  project_members             IS 'Bảng trung gian User ↔ Project: quản lý thành viên và vai trò';
COMMENT ON COLUMN project_members.project_role IS 'OWNER = Trưởng dự án, MEMBER = Thành viên thường';


-- ============================================================
-- BẢNG 4: project_invites
-- Truy vấn chính: lấy invite PENDING của người nhận (Q6)
-- ============================================================
CREATE TABLE project_invites (
    id          SERIAL             PRIMARY KEY,
    project_id  INT                NOT NULL,
    type        invite_type_enum   NOT NULL DEFAULT 'INVITATION',
    sender_id   INT                NOT NULL,
    receiver_id INT                NOT NULL,
    status      invite_status_enum NOT NULL DEFAULT 'PENDING',
    created_at  TIMESTAMPTZ        NOT NULL DEFAULT NOW(),
    expired_at  TIMESTAMPTZ        NOT NULL DEFAULT (NOW() + INTERVAL '7 days'),

    -- Xóa Project → xóa invite liên quan (CASCADE)
    CONSTRAINT fk_pi_project
        FOREIGN KEY (project_id) REFERENCES projects (id)
        ON DELETE CASCADE ON UPDATE CASCADE,

    -- RESTRICT: không cho xóa user đang có invite
    CONSTRAINT fk_pi_sender
        FOREIGN KEY (sender_id) REFERENCES users (id)
        ON DELETE RESTRICT ON UPDATE CASCADE,

    CONSTRAINT fk_pi_receiver
        FOREIGN KEY (receiver_id) REFERENCES users (id)
        ON DELETE RESTRICT ON UPDATE CASCADE,

    -- Tránh gửi invite trùng lặp: cùng project + sender + receiver + type
    CONSTRAINT uq_pi_no_duplicate
        UNIQUE (project_id, sender_id, receiver_id, type)
);

CREATE INDEX idx_pi_receiver_status ON project_invites (receiver_id, status);
CREATE INDEX idx_pi_sender_project  ON project_invites (sender_id, project_id);

COMMENT ON TABLE  project_invites          IS 'Lời mời & Yêu cầu gia nhập dự án 2 chiều';
COMMENT ON COLUMN project_invites.type     IS 'INVITATION = PM mời, JOIN_REQUEST = User xin gia nhập bằng mã';
COMMENT ON COLUMN project_invites.expired_at IS 'Tự động hết hạn sau 7 ngày';


-- ============================================================
-- BẢNG 5: tasks  (Công việc lớn — Kanban Card)
-- Truy vấn chính: lọc task theo nhiều điều kiện (Q1)
-- ============================================================
CREATE TABLE tasks (
    id                     SERIAL           PRIMARY KEY,
    project_id             INT              NOT NULL,
    title                  VARCHAR(300)     NOT NULL,
    description            TEXT             NOT NULL DEFAULT '',
    status                 task_status_enum NOT NULL DEFAULT 'TODO',
    priority               priority_enum    NOT NULL DEFAULT 'MEDIUM',
    due_date               DATE,            -- NULL = chưa đặt hạn chót
    assignee_id            INT,             -- NULL = chưa phân công

    -- Nhãn phân loại — denormalize có chủ đích: tần suất đọc cao, tránh join bảng labels mỗi render
    -- Dạng: "BUG,BACKEND" — Java code xử lý split/join, không cần bảng trung gian
    labels                 TEXT             NOT NULL DEFAULT '',

    -- Trường nghiệm thu tầng Task (PM review — Gate 2)
    final_deliverable_note TEXT             NOT NULL DEFAULT '',
    pm_feedback            TEXT             NOT NULL DEFAULT '',
    submitted_at           TIMESTAMPTZ,
    reviewed_at            TIMESTAMPTZ,
    deliverable_file       VARCHAR(500)     NOT NULL DEFAULT '',
    quality_rating         SMALLINT         NOT NULL DEFAULT 5,

    -- Trường kế hoạch phân rã (Gate 1)
    planning_note          TEXT             NOT NULL DEFAULT '',
    planning_reviewed_at   TIMESTAMPTZ,

    created_at             TIMESTAMPTZ      NOT NULL DEFAULT NOW(),
    updated_at             TIMESTAMPTZ      NOT NULL DEFAULT NOW(),

    -- Xóa Project → xóa Task (CASCADE)
    CONSTRAINT fk_tasks_project
        FOREIGN KEY (project_id) REFERENCES projects (id)
        ON DELETE CASCADE ON UPDATE CASCADE,

    -- Xóa User → SET NULL (task vẫn còn, bỏ trống người phụ trách)
    CONSTRAINT fk_tasks_assignee
        FOREIGN KEY (assignee_id) REFERENCES users (id)
        ON DELETE SET NULL ON UPDATE CASCADE,

    CONSTRAINT chk_tasks_title_len     CHECK (char_length(title) >= 1),
    CONSTRAINT chk_tasks_quality_range CHECK (quality_rating BETWEEN 1 AND 5)
);

-- Composite index quan trọng nhất — Q1: lọc task theo project + status
CREATE INDEX idx_tasks_project_status ON tasks (project_id, status);
-- Lọc theo người phụ trách
CREATE INDEX idx_tasks_assignee_id    ON tasks (assignee_id);
-- Sắp xếp và lọc deadline
CREATE INDEX idx_tasks_due_date       ON tasks (due_date);
-- Full-text search nhãn (vd: WHERE labels LIKE '%BUG%')
CREATE INDEX idx_tasks_labels_gin     ON tasks USING GIN (to_tsvector('simple', labels));

COMMENT ON TABLE  tasks             IS 'Công việc lớn (Kanban Card) — nghiệm thu 2 tầng bởi PM';
COMMENT ON COLUMN tasks.labels      IS 'Denormalize có chủ đích: "BUG,BACKEND" — tần suất đọc cao, tránh join bảng labels';
COMMENT ON COLUMN tasks.assignee_id IS 'SET NULL khi user bị xóa — task vẫn tồn tại';
COMMENT ON COLUMN tasks.due_date    IS 'NULL = chưa đặt hạn chót — logic deadline tính trong Java';

-- Trigger tự động cập nhật updated_at
CREATE OR REPLACE FUNCTION fn_update_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_tasks_updated_at
    BEFORE UPDATE ON tasks
    FOR EACH ROW EXECUTE FUNCTION fn_update_updated_at();


-- ============================================================
-- BẢNG 6: subtasks  (Việc con — nghiệm thu 5 trạng thái)
-- Truy vấn chính: lấy subtask theo task cha (Q3)
-- ============================================================
CREATE TABLE subtasks (
    id              SERIAL              PRIMARY KEY,
    task_id         INT                 NOT NULL,
    title           VARCHAR(300)        NOT NULL,
    assignee_id     INT,                -- NULL = chưa phân công
    status          subtask_status_enum NOT NULL DEFAULT 'TODO',
    due_date        DATE,               -- NULL = chưa đặt; không được vượt quá task cha
    submission_note TEXT                NOT NULL DEFAULT '',
    feedback_note   TEXT                NOT NULL DEFAULT '',
    submitted_at    TIMESTAMPTZ,
    reviewed_at     TIMESTAMPTZ,
    created_at      TIMESTAMPTZ         NOT NULL DEFAULT NOW(),

    -- Xóa Task → xóa SubTask (CASCADE)
    CONSTRAINT fk_subtasks_task
        FOREIGN KEY (task_id) REFERENCES tasks (id)
        ON DELETE CASCADE ON UPDATE CASCADE,

    -- Xóa User → SET NULL (giữ subtask, bỏ trống người phụ trách)
    CONSTRAINT fk_subtasks_assignee
        FOREIGN KEY (assignee_id) REFERENCES users (id)
        ON DELETE SET NULL ON UPDATE CASCADE,

    CONSTRAINT chk_subtasks_title_len CHECK (char_length(title) >= 1)
);

-- Index chính — Q3: lấy subtask theo task cha
CREATE INDEX idx_subtasks_task_id     ON subtasks (task_id);
CREATE INDEX idx_subtasks_assignee_id ON subtasks (assignee_id);

COMMENT ON TABLE subtasks IS 'Việc con trong Task — nghiệm thu 5 trạng thái bởi Task Lead';


-- ============================================================
-- BẢNG 7: labels  (Nhãn phân loại tùy biến theo Project)
-- ============================================================
CREATE TABLE labels (
    id          SERIAL           PRIMARY KEY,
    project_id  INT              NOT NULL,
    name        VARCHAR(50)      NOT NULL,
    color_key   label_color_enum NOT NULL DEFAULT 'blue',
    icon        VARCHAR(50)      NOT NULL DEFAULT 'bi-tag-fill',

    -- Xóa Project → xóa Label (CASCADE)
    CONSTRAINT fk_labels_project
        FOREIGN KEY (project_id) REFERENCES projects (id)
        ON DELETE CASCADE ON UPDATE CASCADE,

    -- Tên nhãn UNIQUE trong phạm vi project — không được trùng
    CONSTRAINT uq_labels_name_per_project UNIQUE (project_id, name),
    CONSTRAINT chk_labels_name_len        CHECK (char_length(name) BETWEEN 1 AND 50)
);

CREATE INDEX idx_labels_project_id ON labels (project_id);

COMMENT ON TABLE  labels      IS 'Nhãn phân loại tùy biến — thuộc về từng Project cụ thể';
COMMENT ON COLUMN labels.name IS 'Tên nhãn UNIQUE trong phạm vi project';


-- ============================================================
-- BẢNG 8: docs  (Tài liệu Wiki của dự án)
-- ============================================================
CREATE TABLE docs (
    id          SERIAL       PRIMARY KEY,
    project_id  INT          NOT NULL,
    title       VARCHAR(300) NOT NULL,
    content     TEXT         NOT NULL DEFAULT '',
    author_id   INT,         -- SET NULL nếu author bị xóa
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    -- Xóa Project → xóa Doc (CASCADE)
    CONSTRAINT fk_docs_project
        FOREIGN KEY (project_id) REFERENCES projects (id)
        ON DELETE CASCADE ON UPDATE CASCADE,

    CONSTRAINT fk_docs_author
        FOREIGN KEY (author_id) REFERENCES users (id)
        ON DELETE SET NULL ON UPDATE CASCADE,

    CONSTRAINT chk_docs_title_len CHECK (char_length(title) >= 1)
);

CREATE INDEX idx_docs_project_id ON docs (project_id);
CREATE INDEX idx_docs_author_id  ON docs (author_id);

CREATE TRIGGER trg_docs_updated_at
    BEFORE UPDATE ON docs
    FOR EACH ROW EXECUTE FUNCTION fn_update_updated_at();

COMMENT ON TABLE docs IS 'Bài viết Wiki / Tài liệu kỹ thuật thuộc dự án';


-- ============================================================
-- BẢNG 9: task_docs  (Quan hệ Nhiều-Nhiều Task ↔ Doc)
-- ============================================================
CREATE TABLE task_docs (
    task_id  INT NOT NULL,
    doc_id   INT NOT NULL,

    CONSTRAINT pk_task_docs PRIMARY KEY (task_id, doc_id),

    -- Xóa Task hoặc Doc → xóa liên kết (CASCADE)
    CONSTRAINT fk_td_task
        FOREIGN KEY (task_id) REFERENCES tasks (id)
        ON DELETE CASCADE ON UPDATE CASCADE,

    CONSTRAINT fk_td_doc
        FOREIGN KEY (doc_id) REFERENCES docs (id)
        ON DELETE CASCADE ON UPDATE CASCADE
);

CREATE INDEX idx_task_docs_doc_id ON task_docs (doc_id);

COMMENT ON TABLE task_docs IS 'Liên kết Nhiều-Nhiều giữa Task và Doc hướng dẫn kỹ thuật';


-- ============================================================
-- BẢNG 10: messages  (Chat dự án + Bình luận Task)
-- Truy vấn chính: lấy tin nhắn theo project + thời gian (Q5)
-- ============================================================
CREATE TABLE messages (
    id          SERIAL       PRIMARY KEY,
    project_id  INT          NOT NULL,
    task_id     INT,         -- NULL = Chat chung dự án; giá trị = Comment của Task cụ thể
    author_id   INT,         -- SET NULL nếu user bị xóa
    author_name VARCHAR(150) NOT NULL DEFAULT 'An danh',  -- Denormalize để hiển thị không cần join
    content     TEXT         NOT NULL,
    sent_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    -- Xóa Project → xóa Message (CASCADE)
    CONSTRAINT fk_messages_project
        FOREIGN KEY (project_id) REFERENCES projects (id)
        ON DELETE CASCADE ON UPDATE CASCADE,

    -- Xóa Task → xóa Comment liên quan (CASCADE)
    CONSTRAINT fk_messages_task
        FOREIGN KEY (task_id) REFERENCES tasks (id)
        ON DELETE CASCADE ON UPDATE CASCADE,

    -- Xóa User → SET NULL (giữ tin nhắn, mất thông tin tác giả)
    CONSTRAINT fk_messages_author
        FOREIGN KEY (author_id) REFERENCES users (id)
        ON DELETE SET NULL ON UPDATE CASCADE,

    CONSTRAINT chk_messages_content_not_empty CHECK (char_length(content) >= 1)
);

-- Index chính — Q5: lấy chat theo project, phân trang theo thời gian mới nhất
CREATE INDEX idx_messages_project_sent ON messages (project_id, task_id, sent_at DESC);
CREATE INDEX idx_messages_task_id      ON messages (task_id);

COMMENT ON TABLE  messages             IS 'Tin nhắn chat dự án (task_id=NULL) và bình luận task (task_id>0)';
COMMENT ON COLUMN messages.task_id     IS 'NULL = Chat chung dự án; giá trị = Comment của Task cụ thể';
COMMENT ON COLUMN messages.author_name IS 'Denormalize — hiển thị nhanh không cần join bảng users mỗi lần';


-- ============================================================
-- BẢNG 11: notifications  (Thông báo hệ thống)
-- Truy vấn chính: lấy notification chưa đọc của user (Q4)
-- ============================================================
CREATE TABLE notifications (
    id           SERIAL                 PRIMARY KEY,
    recipient_id INT                    NOT NULL,
    title        VARCHAR(300)           NOT NULL,
    content      TEXT                   NOT NULL DEFAULT '',
    link         TEXT                   NOT NULL DEFAULT '#',
    type         notification_type_enum NOT NULL DEFAULT 'GENERAL',
    is_read      BOOLEAN                NOT NULL DEFAULT FALSE,
    created_at   TIMESTAMPTZ            NOT NULL DEFAULT NOW(),

    -- Xóa User → xóa toàn bộ notification của họ (CASCADE)
    CONSTRAINT fk_notifications_recipient
        FOREIGN KEY (recipient_id) REFERENCES users (id)
        ON DELETE CASCADE ON UPDATE CASCADE,

    CONSTRAINT chk_notifications_title_len CHECK (char_length(title) >= 1)
);

-- Index chính — Q4: lấy notification chưa đọc, mới nhất trước
CREATE INDEX idx_notif_recipient_unread ON notifications (recipient_id, is_read, created_at DESC);

COMMENT ON TABLE  notifications              IS 'Thông báo hệ thống — quả chuông và trung tâm thông báo';
COMMENT ON COLUMN notifications.recipient_id IS 'CASCADE: xóa user thì xóa toàn bộ thông báo của họ';
COMMENT ON COLUMN notifications.link         IS 'Deep-link URL: bấm vào chuyển thẳng đến Task/Dự án';


-- ============================================================
-- BẢNG 12: activity_logs  (Nhật ký hoạt động & Audit Trail dự án)
-- Truy vấn chính: lấy dòng thời gian hoạt động của dự án theo thời gian mới nhất
-- ============================================================
CREATE TABLE activity_logs (
    id           SERIAL       PRIMARY KEY,
    project_id   INT          NOT NULL,
    user_id      INT,
    action_type  VARCHAR(50)  NOT NULL,
    target_type  VARCHAR(50)  NOT NULL DEFAULT 'TASK',
    target_id    INT          NOT NULL DEFAULT 0,
    target_title VARCHAR(255) NOT NULL DEFAULT '',
    description  TEXT         NOT NULL DEFAULT '',
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    -- Xóa Project → xóa toàn bộ log hoạt động của dự án (CASCADE)
    CONSTRAINT fk_activity_project
        FOREIGN KEY (project_id) REFERENCES projects (id)
        ON DELETE CASCADE ON UPDATE CASCADE,

    -- Xóa User → giữ lại log và gán user_id = NULL (SET NULL) để bảo toàn lịch sử
    CONSTRAINT fk_activity_user
        FOREIGN KEY (user_id) REFERENCES users (id)
        ON DELETE SET NULL ON UPDATE CASCADE
);

CREATE INDEX idx_activity_logs_project ON activity_logs (project_id, created_at DESC);
CREATE INDEX idx_activity_logs_user    ON activity_logs (user_id);

COMMENT ON TABLE  activity_logs              IS 'Dòng thời gian nhật ký hoạt động / Audit trail chuẩn ClickUp 3.0';
COMMENT ON COLUMN activity_logs.action_type  IS 'TASK_CREATE, STATUS_CHANGE, TASK_SUBMIT, PM_APPROVE, PM_REVISE, PM_REJECT, DOC_CREATE';


-- =============================================================================
-- BẢO MẬT: BẬT ROW LEVEL SECURITY (RLS) CHO TOÀN BỘ CÁC BẢNG
-- Mục đích: Khóa toàn bộ các API HTTP công khai (PostgREST / anon key) của Supabase.
-- Chỉ cho phép Java Backend (kết nối trực tiếp qua JDBC quyền postgres/admin) truy cập.
-- =============================================================================
ALTER TABLE users           ENABLE ROW LEVEL SECURITY;
ALTER TABLE projects        ENABLE ROW LEVEL SECURITY;
ALTER TABLE project_members ENABLE ROW LEVEL SECURITY;
ALTER TABLE project_invites ENABLE ROW LEVEL SECURITY;
ALTER TABLE activity_logs   ENABLE ROW LEVEL SECURITY;
ALTER TABLE tasks           ENABLE ROW LEVEL SECURITY;
ALTER TABLE subtasks        ENABLE ROW LEVEL SECURITY;
ALTER TABLE labels          ENABLE ROW LEVEL SECURITY;
ALTER TABLE docs            ENABLE ROW LEVEL SECURITY;
ALTER TABLE task_docs       ENABLE ROW LEVEL SECURITY;
ALTER TABLE messages        ENABLE ROW LEVEL SECURITY;
ALTER TABLE notifications   ENABLE ROW LEVEL SECURITY;


-- =============================================================================
-- SEED DATA — Dữ liệu mẫu khởi tạo (giữ nguyên cấu trúc In-Memory hiện tại)
-- =============================================================================

-- ──────────────── USERS ────────────────
INSERT INTO users (id, username, password, full_name, email, role, avatar, bio, skills, github_url, linkedin_url) VALUES
(1, 'admin', 'admin123', 'Truong Nhom Admin',    'admin@teamwork.com', 'Project Manager',   'images/default_avatar.png',
 'Truong nhom phat trien, chuyen ve kien truc he thong va quan ly du an.',
 'Java, Spring Boot, PostgreSQL, Docker, Agile', 'https://github.com/admin', 'https://linkedin.com/in/admin'),

(2, 'alice', 'alice123', 'Alice Nguyen',          'alice@teamwork.com', 'Frontend Developer','images/default_avatar.png',
 'Chuyen gia UI/UX voi 3 nam kinh nghiem lam viec voi React va Bootstrap.',
 'React, CSS, Bootstrap 5, Figma, JSP', 'https://github.com/alice', ''),

(3, 'bob',   'bob123',   'Bob Tran',              'bob@teamwork.com',   'Backend Developer', 'images/default_avatar.png',
 'Backend developer yeu thich Java va kien truc microservices.',
 'Java, Servlet, JDBC, MySQL, RESTful API', 'https://github.com/bob', 'https://linkedin.com/in/bob'),

(4, 'carol', 'carol123', 'Carol Le',              'carol@teamwork.com', 'UI/UX Designer',    'images/default_avatar.png',
 'Designer sang tao, dam me trai nghiem nguoi dung va thiet ke he thong.',
 'Figma, Adobe XD, HTML, CSS, Prototyping', '', 'https://linkedin.com/in/carol'),

(5, 'david', 'david123', 'David Pham',            'david@teamwork.com', 'QA Engineer',       'images/default_avatar.png',
 'Ky su kiem thu chuyen ve automated testing va dam bao chat luong san pham.',
 'Selenium, JUnit, Postman, TestNG, SQL', 'https://github.com/david', '');

SELECT setval('users_id_seq', (SELECT MAX(id) FROM users));


-- ──────────────── PROJECTS ────────────────
INSERT INTO projects (id, project_code, name, description, owner_id, created_at) VALUES
(1, 'TW-HUB-01', 'TeamWork Hub Platform',
 'Xay dung nen tang quan ly du an va cong viec nhom toan dien voi tich hop Kanban, Chat, Wiki va Nghiem Thu 2 Tang.',
 1, NOW() - INTERVAL '200 days'),

(2, 'ECOM-99', 'E-Commerce Store',
 'Phat trien cua hang thuong mai dien tu tich hop thanh toan truc tuyen VNPay va quan ly kho hang.',
 2, NOW() - INTERVAL '180 days');

SELECT setval('projects_id_seq', (SELECT MAX(id) FROM projects));


-- ──────────────── PROJECT MEMBERS ────────────────
INSERT INTO project_members (project_id, user_id, project_role, joined_at) VALUES
(1, 1, 'OWNER',  NOW() - INTERVAL '200 days'),
(1, 2, 'MEMBER', NOW() - INTERVAL '199 days'),
(1, 3, 'MEMBER', NOW() - INTERVAL '199 days'),
(1, 4, 'MEMBER', NOW() - INTERVAL '198 days'),
(1, 5, 'MEMBER', NOW() - INTERVAL '197 days'),
(2, 2, 'OWNER',  NOW() - INTERVAL '180 days'),
(2, 3, 'MEMBER', NOW() - INTERVAL '179 days'),
(2, 5, 'MEMBER', NOW() - INTERVAL '178 days');


-- ──────────────── LABELS ────────────────
INSERT INTO labels (id, project_id, name, color_key, icon) VALUES
(1, 1, 'Bug',     'red',    'bi-bug-fill'),
(2, 1, 'Feature', 'blue',   'bi-stars'),
(3, 1, 'UI/UX',   'purple', 'bi-palette-fill'),
(4, 1, 'Backend', 'amber',  'bi-gear-fill'),
(5, 1, 'Docs',    'green',  'bi-journal-bookmark-fill'),
(6, 2, 'Bug',     'red',    'bi-bug-fill'),
(7, 2, 'Feature', 'blue',   'bi-stars');

SELECT setval('labels_id_seq', (SELECT MAX(id) FROM labels));


-- ──────────────── TASKS (Project 1) ────────────────
INSERT INTO tasks (id, project_id, title, description, status, priority, due_date, assignee_id, labels, quality_rating) VALUES
(1, 1, 'Thiet ke Database Schema',
 'Thiet ke toan bo cau truc co so du lieu cho he thong Teamwork Hub bao gom cac bang User, Project, Task, SubTask, Message, Notification.',
 'DONE', 'HIGH', NOW()::date - 100, 3, 'BACKEND,DOCS', 5),

(2, 1, 'Xay dung he thong Xac thuc',
 'Trien khai chuc nang Dang nhap, Dang xuat, Quan ly phien lam viec (Session) an toan voi Servlet Filter.',
 'DONE', 'HIGH', NOW()::date - 80, 3, 'BACKEND', 4),

(3, 1, 'Thiet ke giao dien Kanban Board',
 'Xay dung bang Kanban keo tha voi 6 cot trang thai, hieu ung drag-drop muot ma va bo loc realtime.',
 'DONE', 'HIGH', NOW()::date - 60, 2, 'UI,FEATURE', 5),

(4, 1, 'Tich hop he thong Chat du an',
 'Xay dung kenh thao luan nhom trong tung du an voi ho tro tag @username va deep-link #task.',
 'IN_PROGRESS', 'MEDIUM', NOW()::date + 30, 2, 'FEATURE,UI', 5),

(5, 1, 'Module Quan ly Tai lieu Wiki',
 'Xay dung he thong luu tru tai lieu ky thuat, bien ban cuoc hop va huong dan du an dang Wiki.',
 'IN_PROGRESS', 'MEDIUM', NOW()::date + 30, 4, 'DOCS,FEATURE', 5),

(6, 1, 'He thong Thong bao (Notification)',
 'Xay dung he thong thong bao: giao viec, loi moi du an, tien do milestone.',
 'TODO', 'MEDIUM', NOW()::date + 60, 3, 'BACKEND,FEATURE', 5),

(7, 1, 'Tinh nang Ho so Ca nhan',
 'Xay dung trang ho so ca nhan voi thong tin Bio, Ky nang, GitHub/LinkedIn va thong ke dong gop.',
 'TODO', 'LOW', NOW()::date + 90, 4, 'UI,FEATURE', 5),

(8, 1, 'Viet tai lieu API & Huong dan Deploy',
 'Soan thao tai lieu mo ta cac Servlet endpoint, luong xu ly va huong dan trien khai len Tomcat.',
 'TODO', 'LOW', NOW()::date + 120, 5, 'DOCS', 5);

SELECT setval('tasks_id_seq', (SELECT MAX(id) FROM tasks));


-- ──────────────── SUBTASKS (Task 3: Kanban Board) ────────────────
INSERT INTO subtasks (id, task_id, title, assignee_id, status, due_date) VALUES
(1, 3, 'Tao layout HTML 6 cot Kanban',        2, 'APPROVED', NOW()::date - 75),
(2, 3, 'Tich hop thu vien Drag-and-Drop',       2, 'APPROVED', NOW()::date - 72),
(3, 3, 'Hieu ung hover va trang thai cot',      4, 'APPROVED', NOW()::date - 70),
(4, 3, 'Bo loc Task realtime (khong reload)',    2, 'APPROVED', NOW()::date - 68),
(5, 3, 'Responsive mobile (man hinh nho)',       4, 'APPROVED', NOW()::date - 65);

-- ──────────────── SUBTASKS (Task 4: Chat) ────────────────
INSERT INTO subtasks (id, task_id, title, assignee_id, status, due_date) VALUES
(6, 4, 'Thiet ke giao dien khung Chat',          2, 'APPROVED',   NOW()::date - 20),
(7, 4, 'Xay dung MessageServlet backend',         3, 'APPROVED',   NOW()::date - 15),
(8, 4, 'Tich hop phan trang tin nhan',            3, 'SUBMITTED',  NOW()::date + 10),
(9, 4, 'Ho tro tag @username & #task deep-link',  2, 'TODO',       NOW()::date + 20);

SELECT setval('subtasks_id_seq', (SELECT MAX(id) FROM subtasks));


-- ──────────────── DOCS ────────────────
INSERT INTO docs (id, project_id, title, content, author_id) VALUES
(1, 1, 'Huong dan Cai dat Moi truong Phat trien',
 'Buoc 1: Cai dat Java JDK 17+\nBuoc 2: Cai dat Apache Tomcat 10.1.x\nBuoc 3: Clone repository\nBuoc 4: Chay script compile.ps1\nBuoc 5: Mo trinh duyet tai http://localhost:8080/teamwork-hub',
 1),

(2, 1, 'Kien truc He thong Teamwork Hub',
 'Teamwork Hub duoc xay dung theo mo hinh MVC 3 tang:\n- Tang View: JSP + Bootstrap 5 + JavaScript\n- Tang Controller: Java Servlet (HttpServlet)\n- Tang Data: In-Memory → Migrate sang PostgreSQL Supabase',
 1),

(3, 1, 'Quy trinh Nghiem Thu 2 Tang (Task Review Flow)',
 'He thong ap dung quy trinh nghiem thu 2 tang:\nTang 1 - Gate 1 (Planning Gate): Task Lead gui ke hoach phan ra → PM phe duyet → Mo khoa tao SubTask\nTang 2 - Gate 2 (Delivery Gate): Task Lead nop bao cao → PM danh gia va nghiem thu → Cham diem chat luong 1-5 sao',
 3);

SELECT setval('docs_id_seq', (SELECT MAX(id) FROM docs));


-- ──────────────── TASK_DOCS ────────────────
INSERT INTO task_docs (task_id, doc_id) VALUES
(1, 2),
(2, 1),
(3, 3),
(4, 3);


-- ──────────────── MESSAGES ────────────────
INSERT INTO messages (id, project_id, task_id, author_id, author_name, content, sent_at) VALUES
(1, 1, NULL, 1, 'Truong Nhom Admin',
 'Chao ca nhom! Du an TeamWork Hub chinh thuc khoi dong. Hay kiem tra phan cong nhiem vu cua minh trong Kanban Board nhe!',
 NOW() - INTERVAL '10 days'),

(2, 1, NULL, 2, 'Alice Nguyen',
 'Chao Admin! Em da xem qua layout Kanban. Anh co muon em lam responsive mobile truoc khong?',
 NOW() - INTERVAL '10 days' + INTERVAL '45 minutes'),

(3, 1, NULL, 3, 'Bob Tran',
 'Schema database da thiet ke xong roi a. Anh Admin xem qua ERD trong tai lieu Wiki nhe!',
 NOW() - INTERVAL '10 days' + INTERVAL '90 minutes'),

(4, 1, NULL, 1, 'Truong Nhom Admin',
 'Bob lam tot lam! Alice thi hay bat dau tu layout 6 cot nhe, responsive lam sau cung duoc.',
 NOW() - INTERVAL '10 days' + INTERVAL '2 hours'),

(5, 1, 4, 2, 'Alice Nguyen',
 'Giao dien Chat da xong roi anh oi! Em da lam theo mockup Figma. Anh Bob check backend xem on chua nhe.',
 NOW() - INTERVAL '5 days'),

(6, 1, 4, 3, 'Bob Tran',
 'Em da test API gui va nhan tin nhan. Hoat dong on. Con phan phan trang dang lam tiep.',
 NOW() - INTERVAL '5 days' + INTERVAL '90 minutes');

SELECT setval('messages_id_seq', (SELECT MAX(id) FROM messages));


-- ──────────────── NOTIFICATIONS ────────────────
INSERT INTO notifications (id, recipient_id, title, content, link, type, is_read) VALUES
(1, 2, 'Duoc giao cong viec moi',
 'Ban duoc giao task "Thiet ke giao dien Kanban Board" trong du an TeamWork Hub.',
 '/teamwork-hub/task?action=list&projectId=1', 'TASK_ASSIGNED', TRUE),

(2, 3, 'Duoc giao cong viec moi',
 'Ban duoc giao task "Thiet ke Database Schema" trong du an TeamWork Hub.',
 '/teamwork-hub/task?action=list&projectId=1', 'TASK_ASSIGNED', TRUE),

(3, 3, 'Task da duoc PM nghiem thu',
 'PM da duyet task "Thiet ke Database Schema". Chat luong: 5/5 sao.',
 '/teamwork-hub/task?action=list&projectId=1', 'PROGRESS', TRUE),

(4, 2, 'Binh luan moi trong Task cua ban',
 'Bob Tran da binh luan trong task "Tich hop he thong Chat": "Em da test API gui va nhan tin nhan..."',
 '/teamwork-hub/task?action=list&projectId=1', 'COMMENT', FALSE),

(5, 4, 'Duoc giao cong viec moi',
 'Ban duoc giao task "Tinh nang Ho so Ca nhan" trong du an TeamWork Hub.',
 '/teamwork-hub/task?action=list&projectId=1', 'TASK_ASSIGNED', FALSE);

SELECT setval('notifications_id_seq', (SELECT MAX(id) FROM notifications));


-- =============================================================================
-- KIEM TRA NHANH SAU KHI CHAY (uncomment de dung)
-- =============================================================================
/*
-- 1. Kiem tra so ban ghi moi bang
SELECT 'users'           AS bang, COUNT(*) AS so_luong FROM users
UNION ALL SELECT 'projects',        COUNT(*) FROM projects
UNION ALL SELECT 'project_members', COUNT(*) FROM project_members
UNION ALL SELECT 'tasks',           COUNT(*) FROM tasks
UNION ALL SELECT 'subtasks',        COUNT(*) FROM subtasks
UNION ALL SELECT 'labels',          COUNT(*) FROM labels
UNION ALL SELECT 'docs',            COUNT(*) FROM docs
UNION ALL SELECT 'task_docs',       COUNT(*) FROM task_docs
UNION ALL SELECT 'messages',        COUNT(*) FROM messages
UNION ALL SELECT 'notifications',   COUNT(*) FROM notifications;

-- 2. Q1: Lay Task Project 1 theo status IN_PROGRESS
SELECT id, title, status, priority, due_date FROM tasks
WHERE project_id = 1 AND status = 'IN_PROGRESS'
ORDER BY due_date;

-- 3. Q2: Kiem tra quyen cua alice (user 2) trong project 1
SELECT project_role FROM project_members WHERE project_id = 1 AND user_id = 2;

-- 4. Q4: Thong bao chua doc cua alice (user 2)
SELECT id, title, type, created_at FROM notifications
WHERE recipient_id = 2 AND is_read = FALSE ORDER BY created_at DESC;

-- 5. Q5: 5 tin nhan moi nhat cua Project 1 (chat chung)
SELECT id, author_name, content, sent_at FROM messages
WHERE project_id = 1 AND task_id IS NULL ORDER BY sent_at DESC LIMIT 5;
*/
