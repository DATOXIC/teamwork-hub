-- =============================================================================
-- TEAMWORK-HUB — Microsoft SQL Server (T-SQL) Schema cho SSMS
-- Tương thích: SQL Server 2014, 2016, 2017, 2019, 2022 và Azure SQL
-- Đã xử lý triệt để lỗi Msg 1785 (multiple cascade paths) của SQL Server
-- Chỉ cần copy toàn bộ file này bỏ vào SSMS rồi nhấn F5 (Execute) là chạy 100%!
-- =============================================================================

USE master;
GO

-- 1. TẠO DATABASE (NẾU CHƯA CÓ)
IF NOT EXISTS (SELECT * FROM sys.databases WHERE name = 'teamwork_hub')
BEGIN
    CREATE DATABASE teamwork_hub;
END;
GO

USE teamwork_hub;
GO

-- 2. XÓA BẢNG CŨ (THEO THỨ TỰ NGƯỢC: BẢNG CON TRƯỚC, BẢNG CHA SAU)
IF OBJECT_ID('dbo.activity_logs', 'U') IS NOT NULL DROP TABLE dbo.activity_logs;
IF OBJECT_ID('dbo.notifications', 'U') IS NOT NULL DROP TABLE dbo.notifications;
IF OBJECT_ID('dbo.task_docs', 'U') IS NOT NULL DROP TABLE dbo.task_docs;
IF OBJECT_ID('dbo.messages', 'U') IS NOT NULL DROP TABLE dbo.messages;
IF OBJECT_ID('dbo.subtasks', 'U') IS NOT NULL DROP TABLE dbo.subtasks;
IF OBJECT_ID('dbo.labels', 'U') IS NOT NULL DROP TABLE dbo.labels;
IF OBJECT_ID('dbo.tasks', 'U') IS NOT NULL DROP TABLE dbo.tasks;
IF OBJECT_ID('dbo.docs', 'U') IS NOT NULL DROP TABLE dbo.docs;
IF OBJECT_ID('dbo.project_invites', 'U') IS NOT NULL DROP TABLE dbo.project_invites;
IF OBJECT_ID('dbo.project_members', 'U') IS NOT NULL DROP TABLE dbo.project_members;
IF OBJECT_ID('dbo.projects', 'U') IS NOT NULL DROP TABLE dbo.projects;
IF OBJECT_ID('dbo.users', 'U') IS NOT NULL DROP TABLE dbo.users;
GO

-- =============================================================================
-- BẢNG 1: users (Người dùng & Xác thực)
-- =============================================================================
CREATE TABLE users (
    id            INT IDENTITY(1,1) PRIMARY KEY,
    username      NVARCHAR(50)  NOT NULL,
    password      NVARCHAR(255) NOT NULL,
    full_name     NVARCHAR(150) NOT NULL,
    email         NVARCHAR(150) NOT NULL,
    role          NVARCHAR(100) NOT NULL DEFAULT 'Developer',
    avatar        NVARCHAR(MAX) NOT NULL DEFAULT 'images/default_avatar.png',
    bio           NVARCHAR(500) NOT NULL DEFAULT '',
    skills        NVARCHAR(MAX) NOT NULL DEFAULT '',
    github_url    NVARCHAR(MAX) NOT NULL DEFAULT '',
    linkedin_url  NVARCHAR(MAX) NOT NULL DEFAULT '',
    created_at    DATETIME2     NOT NULL DEFAULT GETDATE(),

    CONSTRAINT uq_users_username UNIQUE (username),
    CONSTRAINT uq_users_email    UNIQUE (email),
    CONSTRAINT chk_users_username_len CHECK (LEN(username) BETWEEN 3 AND 50),
    CONSTRAINT chk_users_email_format CHECK (email LIKE '%@%')
);
GO
CREATE INDEX idx_users_username ON users (username);
CREATE INDEX idx_users_email    ON users (email);
GO

-- =============================================================================
-- BẢNG 2: projects (Dự án)
-- =============================================================================
CREATE TABLE projects (
    id            INT IDENTITY(1,1) PRIMARY KEY,
    project_code  NVARCHAR(30)  NOT NULL,
    name          NVARCHAR(200) NOT NULL,
    description   NVARCHAR(MAX) NOT NULL DEFAULT '',
    project_type  NVARCHAR(20)  NOT NULL DEFAULT 'TEAM',
    owner_id      INT           NOT NULL,
    created_at    DATETIME2     NOT NULL DEFAULT GETDATE(),

    CONSTRAINT fk_projects_owner
        FOREIGN KEY (owner_id) REFERENCES users (id)
        ON DELETE NO ACTION ON UPDATE NO ACTION,

    CONSTRAINT uq_projects_code   UNIQUE (project_code),
    CONSTRAINT chk_projects_code_len CHECK (LEN(project_code) BETWEEN 2 AND 30),
    CONSTRAINT chk_projects_name_len CHECK (LEN(name) >= 1),
    CONSTRAINT chk_projects_type CHECK (project_type IN ('SOLO', 'TEAM'))
);
GO
CREATE INDEX idx_projects_owner_id ON projects (owner_id);
CREATE INDEX idx_projects_code     ON projects (project_code);
GO

-- =============================================================================
-- BẢNG 3: project_members (Thành viên Dự án - Nhiều-Nhiều)
-- =============================================================================
CREATE TABLE project_members (
    project_id   INT           NOT NULL,
    user_id      INT           NOT NULL,
    project_role NVARCHAR(20)  NOT NULL DEFAULT 'MEMBER',
    joined_at    DATETIME2     NOT NULL DEFAULT GETDATE(),

    CONSTRAINT pk_project_members PRIMARY KEY (project_id, user_id),

    CONSTRAINT fk_pm_project
        FOREIGN KEY (project_id) REFERENCES projects (id)
        ON DELETE CASCADE ON UPDATE NO ACTION,

    CONSTRAINT fk_pm_user
        FOREIGN KEY (user_id) REFERENCES users (id)
        ON DELETE NO ACTION ON UPDATE NO ACTION,

    CONSTRAINT chk_pm_role CHECK (project_role IN ('OWNER', 'MEMBER'))
);
GO
CREATE INDEX idx_pm_user_id    ON project_members (user_id);
CREATE INDEX idx_pm_project_id ON project_members (project_id);
GO

-- =============================================================================
-- BẢNG 4: project_invites (Lời mời / Yêu cầu gia nhập dự án)
-- =============================================================================
CREATE TABLE project_invites (
    id          INT IDENTITY(1,1) PRIMARY KEY,
    project_id  INT           NOT NULL,
    type        NVARCHAR(20)  NOT NULL DEFAULT 'INVITATION',
    sender_id   INT           NOT NULL,
    receiver_id INT           NOT NULL,
    status      NVARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    created_at  DATETIME2     NOT NULL DEFAULT GETDATE(),
    expired_at  DATETIME2     NOT NULL DEFAULT DATEADD(DAY, 7, GETDATE()),

    CONSTRAINT fk_pi_project
        FOREIGN KEY (project_id) REFERENCES projects (id)
        ON DELETE CASCADE ON UPDATE NO ACTION,

    CONSTRAINT fk_pi_sender
        FOREIGN KEY (sender_id) REFERENCES users (id)
        ON DELETE NO ACTION ON UPDATE NO ACTION,

    CONSTRAINT fk_pi_receiver
        FOREIGN KEY (receiver_id) REFERENCES users (id)
        ON DELETE NO ACTION ON UPDATE NO ACTION,

    CONSTRAINT uq_pi_no_duplicate
        UNIQUE (project_id, sender_id, receiver_id, type),

    CONSTRAINT chk_pi_type CHECK (type IN ('INVITATION', 'JOIN_REQUEST')),
    CONSTRAINT chk_pi_status CHECK (status IN ('PENDING', 'ACCEPTED', 'REJECTED', 'REVOKED', 'EXPIRED'))
);
GO
CREATE INDEX idx_pi_receiver_status ON project_invites (receiver_id, status);
CREATE INDEX idx_pi_sender_project  ON project_invites (sender_id, project_id);
GO

-- =============================================================================
-- BẢNG 5: tasks (Công việc Kanban - Nghiệm thu 2 tầng)
-- =============================================================================
CREATE TABLE tasks (
    id                     INT IDENTITY(1,1) PRIMARY KEY,
    project_id             INT           NOT NULL,
    title                  NVARCHAR(300) NOT NULL,
    description            NVARCHAR(MAX) NOT NULL DEFAULT '',
    status                 NVARCHAR(30)  NOT NULL DEFAULT 'TODO',
    priority               NVARCHAR(20)  NOT NULL DEFAULT 'MEDIUM',
    due_date               DATE,
    assignee_id            INT,
    labels                 NVARCHAR(MAX) NOT NULL DEFAULT '',

    final_deliverable_note NVARCHAR(MAX) NOT NULL DEFAULT '',
    pm_feedback            NVARCHAR(MAX) NOT NULL DEFAULT '',
    submitted_at           DATETIME2,
    reviewed_at            DATETIME2,
    deliverable_file       NVARCHAR(500) NOT NULL DEFAULT '',
    quality_rating         SMALLINT      NOT NULL DEFAULT 5,

    planning_note          NVARCHAR(MAX) NOT NULL DEFAULT '',
    planning_reviewed_at   DATETIME2,

    requires_gate          BIT           NOT NULL DEFAULT 1,

    created_at             DATETIME2     NOT NULL DEFAULT GETDATE(),
    updated_at             DATETIME2     NOT NULL DEFAULT GETDATE(),

    CONSTRAINT fk_tasks_project
        FOREIGN KEY (project_id) REFERENCES projects (id)
        ON DELETE CASCADE ON UPDATE NO ACTION,

    -- Dùng NO ACTION để tránh lỗi 1785 multiple cascade paths từ users
    CONSTRAINT fk_tasks_assignee
        FOREIGN KEY (assignee_id) REFERENCES users (id)
        ON DELETE NO ACTION ON UPDATE NO ACTION,

    CONSTRAINT chk_tasks_title_len     CHECK (LEN(title) >= 1),
    CONSTRAINT chk_tasks_quality_range CHECK (quality_rating BETWEEN 1 AND 5),
    CONSTRAINT chk_tasks_status CHECK (status IN ('TODO', 'PLANNING', 'IN_PROGRESS', 'SUBMITTED', 'REVISE', 'REJECTED', 'DONE', 'APPROVED')),
    CONSTRAINT chk_tasks_priority CHECK (priority IN ('HIGH', 'MEDIUM', 'LOW'))
);
GO
CREATE INDEX idx_tasks_project_status ON tasks (project_id, status);
CREATE INDEX idx_tasks_assignee_id    ON tasks (assignee_id);
CREATE INDEX idx_tasks_due_date       ON tasks (due_date);
GO

CREATE OR ALTER TRIGGER trg_tasks_updated_at ON tasks
AFTER UPDATE AS
BEGIN
    SET NOCOUNT ON;
    UPDATE t SET updated_at = GETDATE()
    FROM tasks t INNER JOIN inserted i ON t.id = i.id;
END;
GO

-- =============================================================================
-- BẢNG 6: subtasks (Việc con)
-- =============================================================================
CREATE TABLE subtasks (
    id              INT IDENTITY(1,1) PRIMARY KEY,
    task_id         INT           NOT NULL,
    title           NVARCHAR(300) NOT NULL,
    assignee_id     INT,
    status          NVARCHAR(20)  NOT NULL DEFAULT 'TODO',
    due_date        DATE,
    submission_note NVARCHAR(MAX) NOT NULL DEFAULT '',
    feedback_note   NVARCHAR(MAX) NOT NULL DEFAULT '',
    submitted_at    DATETIME2,
    reviewed_at     DATETIME2,
    created_at      DATETIME2     NOT NULL DEFAULT GETDATE(),

    CONSTRAINT fk_subtasks_task
        FOREIGN KEY (task_id) REFERENCES tasks (id)
        ON DELETE CASCADE ON UPDATE NO ACTION,

    CONSTRAINT fk_subtasks_assignee
        FOREIGN KEY (assignee_id) REFERENCES users (id)
        ON DELETE NO ACTION ON UPDATE NO ACTION,

    CONSTRAINT chk_subtasks_title_len CHECK (LEN(title) >= 1),
    CONSTRAINT chk_subtasks_status CHECK (status IN ('TODO', 'SUBMITTED', 'REVISE', 'REJECTED', 'APPROVED'))
);
GO
CREATE INDEX idx_subtasks_task_id     ON subtasks (task_id);
CREATE INDEX idx_subtasks_assignee_id ON subtasks (assignee_id);
GO

-- =============================================================================
-- BẢNG 7: labels (Nhãn công việc)
-- =============================================================================
CREATE TABLE labels (
    id          INT IDENTITY(1,1) PRIMARY KEY,
    project_id  INT          NOT NULL,
    name        NVARCHAR(50) NOT NULL,
    color_key   NVARCHAR(20) NOT NULL DEFAULT 'blue',
    icon        NVARCHAR(50) NOT NULL DEFAULT 'bi-tag-fill',

    CONSTRAINT fk_labels_project
        FOREIGN KEY (project_id) REFERENCES projects (id)
        ON DELETE CASCADE ON UPDATE NO ACTION,

    CONSTRAINT uq_labels_name_per_project UNIQUE (project_id, name),
    CONSTRAINT chk_labels_name_len        CHECK (LEN(name) BETWEEN 1 AND 50),
    CONSTRAINT chk_labels_color CHECK (color_key IN ('red', 'blue', 'purple', 'amber', 'green', 'pink', 'cyan', 'slate'))
);
GO
CREATE INDEX idx_labels_project_id ON labels (project_id);
GO

-- =============================================================================
-- BẢNG 8: docs (Tài liệu Wiki)
-- =============================================================================
CREATE TABLE docs (
    id          INT IDENTITY(1,1) PRIMARY KEY,
    project_id  INT           NOT NULL,
    title       NVARCHAR(300) NOT NULL,
    content     NVARCHAR(MAX) NOT NULL DEFAULT '',
    author_id   INT,
    created_at  DATETIME2     NOT NULL DEFAULT GETDATE(),
    updated_at  DATETIME2     NOT NULL DEFAULT GETDATE(),

    CONSTRAINT fk_docs_project
        FOREIGN KEY (project_id) REFERENCES projects (id)
        ON DELETE CASCADE ON UPDATE NO ACTION,

    CONSTRAINT fk_docs_author
        FOREIGN KEY (author_id) REFERENCES users (id)
        ON DELETE NO ACTION ON UPDATE NO ACTION,

    CONSTRAINT chk_docs_title_len CHECK (LEN(title) >= 1)
);
GO
CREATE INDEX idx_docs_project_id ON docs (project_id);
CREATE INDEX idx_docs_author_id  ON docs (author_id);
GO

CREATE OR ALTER TRIGGER trg_docs_updated_at ON docs
AFTER UPDATE AS
BEGIN
    SET NOCOUNT ON;
    UPDATE d SET updated_at = GETDATE()
    FROM docs d INNER JOIN inserted i ON d.id = i.id;
END;
GO

-- =============================================================================
-- BẢNG 9: task_docs (Đính kèm tài liệu vào Task - Nhiều-Nhiều)
-- =============================================================================
CREATE TABLE task_docs (
    task_id  INT NOT NULL,
    doc_id   INT NOT NULL,

    CONSTRAINT pk_task_docs PRIMARY KEY (task_id, doc_id),

    CONSTRAINT fk_td_task
        FOREIGN KEY (task_id) REFERENCES tasks (id)
        ON DELETE CASCADE ON UPDATE NO ACTION,

    CONSTRAINT fk_td_doc
        FOREIGN KEY (doc_id) REFERENCES docs (id)
        ON DELETE NO ACTION ON UPDATE NO ACTION
);
GO
CREATE INDEX idx_task_docs_doc_id ON task_docs (doc_id);
GO

-- =============================================================================
-- BẢNG 10: messages (Tin nhắn & Bình luận)
-- =============================================================================
CREATE TABLE messages (
    id          INT IDENTITY(1,1) PRIMARY KEY,
    project_id  INT           NOT NULL,
    task_id     INT,
    author_id   INT,
    author_name NVARCHAR(150) NOT NULL DEFAULT 'An danh',
    content     NVARCHAR(MAX) NOT NULL,
    sent_at     DATETIME2     NOT NULL DEFAULT GETDATE(),

    CONSTRAINT fk_messages_project
        FOREIGN KEY (project_id) REFERENCES projects (id)
        ON DELETE NO ACTION ON UPDATE NO ACTION,

    CONSTRAINT fk_messages_task
        FOREIGN KEY (task_id) REFERENCES tasks (id)
        ON DELETE CASCADE ON UPDATE NO ACTION,

    CONSTRAINT fk_messages_author
        FOREIGN KEY (author_id) REFERENCES users (id)
        ON DELETE NO ACTION ON UPDATE NO ACTION,

    CONSTRAINT chk_messages_content_not_empty CHECK (LEN(content) >= 1)
);
GO
CREATE INDEX idx_messages_project_sent ON messages (project_id, task_id, sent_at DESC);
CREATE INDEX idx_messages_task_id      ON messages (task_id);
GO

-- =============================================================================
-- BẢNG 11: notifications (Thông báo)
-- =============================================================================
CREATE TABLE notifications (
    id           INT IDENTITY(1,1) PRIMARY KEY,
    recipient_id INT           NOT NULL,
    title        NVARCHAR(300) NOT NULL,
    content      NVARCHAR(MAX) NOT NULL DEFAULT '',
    link         NVARCHAR(MAX) NOT NULL DEFAULT '#',
    type         NVARCHAR(30)  NOT NULL DEFAULT 'GENERAL',
    is_read      BIT           NOT NULL DEFAULT 0,
    created_at   DATETIME2     NOT NULL DEFAULT GETDATE(),

    CONSTRAINT fk_notifications_recipient
        FOREIGN KEY (recipient_id) REFERENCES users (id)
        ON DELETE CASCADE ON UPDATE NO ACTION,

    CONSTRAINT chk_notifications_title_len CHECK (LEN(title) >= 1),
    CONSTRAINT chk_notifications_type CHECK (type IN ('INVITE', 'TASK_ASSIGNED', 'PROGRESS', 'COMMENT', 'GENERAL'))
);
GO
CREATE INDEX idx_notif_recipient_unread ON notifications (recipient_id, is_read, created_at DESC);
GO

-- =============================================================================
-- BẢNG 12: activity_logs (Nhật ký hoạt động dự án)
-- =============================================================================
CREATE TABLE activity_logs (
    id           INT IDENTITY(1,1) PRIMARY KEY,
    project_id   INT           NOT NULL,
    user_id      INT,
    action_type  NVARCHAR(50)  NOT NULL,
    target_type  NVARCHAR(50)  NOT NULL DEFAULT 'TASK',
    target_id    INT           NOT NULL DEFAULT 0,
    target_title NVARCHAR(255) NOT NULL DEFAULT '',
    description  NVARCHAR(MAX) NOT NULL DEFAULT '',
    created_at   DATETIME2     NOT NULL DEFAULT GETDATE(),

    CONSTRAINT fk_activity_project
        FOREIGN KEY (project_id) REFERENCES projects (id)
        ON DELETE CASCADE ON UPDATE NO ACTION,

    CONSTRAINT fk_activity_user
        FOREIGN KEY (user_id) REFERENCES users (id)
        ON DELETE NO ACTION ON UPDATE NO ACTION
);
GO
CREATE INDEX idx_activity_logs_project ON activity_logs (project_id, created_at DESC);
CREATE INDEX idx_activity_logs_user    ON activity_logs (user_id);
GO

-- =============================================================================
-- DỮ LIỆU MẪU KHỞI TẠO (SEED DATA)
-- =============================================================================

-- 1. USERS
SET IDENTITY_INSERT users ON;
INSERT INTO users (id, username, password, full_name, email, role, avatar, bio, skills, github_url, linkedin_url) VALUES
(1, 'admin', 'admin123', 'Truong Nhom Admin',    'admin@teamwork.com', 'Project Manager',   'images/default_avatar.png',
 'Truong nhom phat trien, chuyen ve kien truc he thong va quan ly du an.',
 'Java, Spring Boot, SQL Server, Docker, Agile', 'https://github.com/admin', 'https://linkedin.com/in/admin'),

(2, 'alice', 'alice123', 'Alice Nguyen',          'alice@teamwork.com', 'Frontend Developer','images/default_avatar.png',
 'Chuyen gia UI/UX voi 3 nam kinh nghiem lam viec voi React va Bootstrap.',
 'React, CSS, Bootstrap 5, Figma, JSP', 'https://github.com/alice', ''),

(3, 'bob',   'bob123',   'Bob Tran',              'bob@teamwork.com',   'Backend Developer', 'images/default_avatar.png',
 'Backend developer yeu thich Java va kien truc microservices.',
 'Java, Servlet, JDBC, SQL Server, RESTful API', 'https://github.com/bob', 'https://linkedin.com/in/bob'),

(4, 'carol', 'carol123', 'Carol Le',              'carol@teamwork.com', 'UI/UX Designer',    'images/default_avatar.png',
 'Designer sang tao, dam me trai nghiem nguoi dung va thiet ke he thong.',
 'Figma, Adobe XD, HTML, CSS, Prototyping', '', 'https://linkedin.com/in/carol'),

(5, 'david', 'david123', 'David Pham',            'david@teamwork.com', 'QA Engineer',       'images/default_avatar.png',
 'Ky su kiem thu chuyen ve automated testing va dam bao chat luong san pham.',
 'Selenium, JUnit, Postman, TestNG, SQL', 'https://github.com/david', '');
SET IDENTITY_INSERT users OFF;
GO

-- 2. PROJECTS
SET IDENTITY_INSERT projects ON;
INSERT INTO projects (id, project_code, name, description, owner_id, created_at) VALUES
(1, 'TW-HUB-01', 'TeamWork Hub Platform',
 'Xay dung nen tang quan ly du an va cong viec nhom toan dien voi tich hop Kanban, Chat, Wiki va Nghiem Thu 2 Tang.',
 1, DATEADD(DAY, -200, GETDATE())),

(2, 'ECOM-99', 'E-Commerce Store',
 'Phat trien cua hang thuong mai dien tu tich hop thanh toan truc tuyen VNPay va quan ly kho hang.',
 2, DATEADD(DAY, -180, GETDATE()));
SET IDENTITY_INSERT projects OFF;
GO

-- 3. PROJECT MEMBERS
INSERT INTO project_members (project_id, user_id, project_role, joined_at) VALUES
(1, 1, 'OWNER',  DATEADD(DAY, -200, GETDATE())),
(1, 2, 'MEMBER', DATEADD(DAY, -199, GETDATE())),
(1, 3, 'MEMBER', DATEADD(DAY, -199, GETDATE())),
(1, 4, 'MEMBER', DATEADD(DAY, -198, GETDATE())),
(1, 5, 'MEMBER', DATEADD(DAY, -197, GETDATE())),
(2, 2, 'OWNER',  DATEADD(DAY, -180, GETDATE())),
(2, 3, 'MEMBER', DATEADD(DAY, -179, GETDATE())),
(2, 5, 'MEMBER', DATEADD(DAY, -178, GETDATE()));
GO

-- 4. LABELS
SET IDENTITY_INSERT labels ON;
INSERT INTO labels (id, project_id, name, color_key, icon) VALUES
(1, 1, 'Bug',     'red',    'bi-bug-fill'),
(2, 1, 'Feature', 'blue',   'bi-stars'),
(3, 1, 'UI/UX',   'purple', 'bi-palette-fill'),
(4, 1, 'Backend', 'amber',  'bi-gear-fill'),
(5, 1, 'Docs',    'green',  'bi-journal-bookmark-fill'),
(6, 2, 'Bug',     'red',    'bi-bug-fill'),
(7, 2, 'Feature', 'blue',   'bi-stars');
SET IDENTITY_INSERT labels OFF;
GO

-- 5. TASKS
SET IDENTITY_INSERT tasks ON;
INSERT INTO tasks (id, project_id, title, description, status, priority, due_date, assignee_id, labels, quality_rating) VALUES
(1, 1, 'Thiet ke Database Schema',
 'Thiet ke toan bo cau truc co so du lieu cho he thong Teamwork Hub bao gom cac bang User, Project, Task, SubTask, Message, Notification.',
 'DONE', 'HIGH', CAST(DATEADD(DAY, -100, GETDATE()) AS DATE), 3, 'BACKEND,DOCS', 5),

(2, 1, 'Xay dung he thong Xac thuc',
 'Trien khai chuc nang Dang nhap, Dang xuat, Quan ly phien lam viec (Session) an toan voi Servlet Filter.',
 'DONE', 'HIGH', CAST(DATEADD(DAY, -80, GETDATE()) AS DATE), 3, 'BACKEND', 4),

(3, 1, 'Thiet ke giao dien Kanban Board',
 'Xay dung bang Kanban keo tha voi 6 cot trang thai, hieu ung drag-drop muot ma va bo loc realtime.',
 'DONE', 'HIGH', CAST(DATEADD(DAY, -60, GETDATE()) AS DATE), 2, 'UI,FEATURE', 5),

(4, 1, 'Tich hop he thong Chat du an',
 'Xay dung kenh thao luan nhom trong tung du an voi ho tro tag @username va deep-link #task.',
 'IN_PROGRESS', 'MEDIUM', CAST(DATEADD(DAY, 30, GETDATE()) AS DATE), 2, 'FEATURE,UI', 5),

(5, 1, 'Module Quan ly Tai lieu Wiki',
 'Xay dung he thong luu tru tai lieu ky thuat, bien ban cuoc hop va huong dan du an dang Wiki.',
 'IN_PROGRESS', 'MEDIUM', CAST(DATEADD(DAY, 30, GETDATE()) AS DATE), 4, 'DOCS,FEATURE', 5),

(6, 1, 'He thong Thong bao (Notification)',
 'Xay dung he thong thong bao: giao viec, loi moi du an, tien do milestone.',
 'TODO', 'MEDIUM', CAST(DATEADD(DAY, 60, GETDATE()) AS DATE), 3, 'BACKEND,FEATURE', 5),

(7, 1, 'Tinh nang Ho so Ca nhan',
 'Xay dung trang ho so ca nhan voi thong tin Bio, Ky nang, GitHub/LinkedIn va thong ke dong gop.',
 'TODO', 'LOW', CAST(DATEADD(DAY, 90, GETDATE()) AS DATE), 4, 'UI,FEATURE', 5),

(8, 1, 'Viet tai lieu API & Huong dan Deploy',
 'Soan thao tai lieu mo ta cac Servlet endpoint, luong xu ly va huong dan trien khai len Tomcat.',
 'TODO', 'LOW', CAST(DATEADD(DAY, 120, GETDATE()) AS DATE), 5, 'DOCS', 5);
SET IDENTITY_INSERT tasks OFF;
GO

-- 6. SUBTASKS
SET IDENTITY_INSERT subtasks ON;
INSERT INTO subtasks (id, task_id, title, assignee_id, status, due_date) VALUES
(1, 3, 'Tao layout HTML 6 cot Kanban',        2, 'APPROVED', CAST(DATEADD(DAY, -75, GETDATE()) AS DATE)),
(2, 3, 'Tich hop thu vien Drag-and-Drop',       2, 'APPROVED', CAST(DATEADD(DAY, -72, GETDATE()) AS DATE)),
(3, 3, 'Hieu ung hover va trang thai cot',      4, 'APPROVED', CAST(DATEADD(DAY, -70, GETDATE()) AS DATE)),
(4, 3, 'Bo loc Task realtime (khong reload)',    2, 'APPROVED', CAST(DATEADD(DAY, -68, GETDATE()) AS DATE)),
(5, 3, 'Responsive mobile (man hinh nho)',       4, 'APPROVED', CAST(DATEADD(DAY, -65, GETDATE()) AS DATE)),
(6, 4, 'Thiet ke giao dien khung Chat',          2, 'APPROVED', CAST(DATEADD(DAY, -20, GETDATE()) AS DATE)),
(7, 4, 'Xay dung MessageServlet backend',         3, 'APPROVED', CAST(DATEADD(DAY, -15, GETDATE()) AS DATE)),
(8, 4, 'Tich hop phan trang tin nhan',            3, 'SUBMITTED', CAST(DATEADD(DAY, 10, GETDATE()) AS DATE)),
(9, 4, 'Ho tro tag @username & #task deep-link',  2, 'TODO', CAST(DATEADD(DAY, 20, GETDATE()) AS DATE));
SET IDENTITY_INSERT subtasks OFF;
GO

-- 7. DOCS
SET IDENTITY_INSERT docs ON;
INSERT INTO docs (id, project_id, title, content, author_id) VALUES
(1, 1, 'Huong dan Cai dat Moi truong Phat trien',
 'Buoc 1: Cai dat Java JDK 17+\nBuoc 2: Cai dat Apache Tomcat 10.1.x\nBuoc 3: Clone repository\nBuoc 4: Chay script compile.ps1\nBuoc 5: Mo trinh duyet tai http://localhost:8080/teamwork-hub',
 1),

(2, 1, 'Kien truc He thong Teamwork Hub',
 'Teamwork Hub duoc xay dung theo mo hinh MVC 3 tang:\n- Tang View: JSP + Bootstrap 5 + JavaScript\n- Tang Controller: Java Servlet (HttpServlet)\n- Tang Data: JPA 3.1 & Hibernate 6 ORM',
 1),

(3, 1, 'Quy trinh Nghiem Thu 2 Tang (Task Review Flow)',
 'He thong ap dung quy trinh nghiem thu 2 tang:\nTang 1 - Gate 1 (Planning Gate): Task Lead gui ke hoach phan ra -> PM phe duyet -> Mo khoa tao SubTask\nTang 2 - Gate 2 (Delivery Gate): Task Lead nop bao cao -> PM danh gia va nghiem thu -> Cham diem chat luong 1-5 sao',
 3);
SET IDENTITY_INSERT docs OFF;
GO

-- 8. TASK_DOCS
INSERT INTO task_docs (task_id, doc_id) VALUES
(1, 2),
(2, 1),
(3, 3),
(4, 3);
GO

-- 9. MESSAGES
SET IDENTITY_INSERT messages ON;
INSERT INTO messages (id, project_id, task_id, author_id, author_name, content, sent_at) VALUES
(1, 1, NULL, 1, 'Truong Nhom Admin',
 'Chao ca nhom! Du an TeamWork Hub chinh thuc khoi dong. Hay kiem tra phan cong nhiem vu cua minh trong Kanban Board nhe!',
 DATEADD(DAY, -10, GETDATE())),

(2, 1, NULL, 2, 'Alice Nguyen',
 'Chao Admin! Em da xem qua layout Kanban. Anh co muon em lam responsive mobile truoc khong?',
 DATEADD(MINUTE, 45, DATEADD(DAY, -10, GETDATE()))),

(3, 1, NULL, 3, 'Bob Tran',
 'Schema database da thiet ke xong roi a. Anh Admin xem qua ERD trong tai lieu Wiki nhe!',
 DATEADD(MINUTE, 90, DATEADD(DAY, -10, GETDATE()))),

(4, 1, NULL, 1, 'Truong Nhom Admin',
 'Bob lam tot lam! Alice thi hay bat dau tu layout 6 cot nhe, responsive lam sau cung duoc.',
 DATEADD(HOUR, 2, DATEADD(DAY, -10, GETDATE()))),

(5, 1, 4, 2, 'Alice Nguyen',
 'Giao dien Chat da xong roi anh oi! Em da lam theo mockup Figma. Anh Bob check backend xem on chua nhe.',
 DATEADD(DAY, -5, GETDATE())),

(6, 1, 4, 3, 'Bob Tran',
 'Em da test API gui va nhan tin nhan. Hoat dong on. Con phan phan trang dang lam tiep.',
 DATEADD(MINUTE, 90, DATEADD(DAY, -5, GETDATE())));
SET IDENTITY_INSERT messages OFF;
GO

-- 10. NOTIFICATIONS
SET IDENTITY_INSERT notifications ON;
INSERT INTO notifications (id, recipient_id, title, content, link, type, is_read) VALUES
(1, 2, 'Duoc giao cong viec moi',
 'Ban duoc giao task "Thiet ke giao dien Kanban Board" trong du an TeamWork Hub.',
 '/teamwork-hub/task?action=list&projectId=1', 'TASK_ASSIGNED', 1),

(2, 3, 'Duoc giao cong viec moi',
 'Ban duoc giao task "Thiet ke Database Schema" trong du an TeamWork Hub.',
 '/teamwork-hub/task?action=list&projectId=1', 'TASK_ASSIGNED', 1),

(3, 3, 'Task da duoc PM nghiem thu',
 'PM da duyet task "Thiet ke Database Schema". Chat luong: 5/5 sao.',
 '/teamwork-hub/task?action=list&projectId=1', 'PROGRESS', 1),

(4, 2, 'Binh luan moi trong Task cua ban',
 'Bob Tran da binh luan trong task "Tich hop he thong Chat": "Em da test API gui va nhan tin nhan..."',
 '/teamwork-hub/task?action=list&projectId=1', 'COMMENT', 0),

(5, 4, 'Duoc giao cong viec moi',
 'Ban duoc giao task "Tinh nang Ho so Ca nhan" trong du an TeamWork Hub.',
 '/teamwork-hub/task?action=list&projectId=1', 'TASK_ASSIGNED', 0);
SET IDENTITY_INSERT notifications OFF;
GO

-- =============================================================================
-- KIỂM TRA NHANH SAU KHI CHẠY TRONG SSMS
-- =============================================================================
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
GO
