-- Bảng vẽ (Whiteboard) cho mỗi dự án. Chạy MỘT trong ba khối tùy CSDL đang dùng.

-- ===== PostgreSQL / Supabase =====
CREATE TABLE IF NOT EXISTS whiteboards (
    id          SERIAL      PRIMARY KEY,
    project_id  INT         NOT NULL UNIQUE,
    content     TEXT        NOT NULL DEFAULT '',
    updated_by  INT,
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_whiteboards_project
        FOREIGN KEY (project_id) REFERENCES projects (id) ON DELETE CASCADE
);

-- ===== SQL Server (SSMS) =====
-- CREATE TABLE whiteboards (
--     id          INT IDENTITY(1,1) PRIMARY KEY,
--     project_id  INT           NOT NULL UNIQUE,
--     content     NVARCHAR(MAX) NOT NULL DEFAULT '',
--     updated_by  INT,
--     updated_at  DATETIME2     NOT NULL DEFAULT GETDATE(),
--     CONSTRAINT fk_whiteboards_project
--         FOREIGN KEY (project_id) REFERENCES projects (id) ON DELETE CASCADE
-- );

-- ===== MySQL: không cần chạy gì (hibernate.hbm2ddl.auto=update tự tạo bảng) =====
