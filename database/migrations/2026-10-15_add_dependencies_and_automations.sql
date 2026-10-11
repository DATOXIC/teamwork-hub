-- ============================================================================
-- MIGRATION: 2026-10-15_add_dependencies_and_automations.sql
-- Mục đích: Hỗ trợ Biểu đồ Gantt, Quan hệ phụ thuộc công việc (Task Dependencies),
--           và Hệ thống Tự động hóa Quy trình (No-Code Automations)
-- ============================================================================

-- ============================================================================
-- PHẦN 1: PostgreSQL / Supabase (Chạy trên Supabase SQL Editor)
-- ============================================================================

-- 1. Thêm cột start_date cho bảng tasks nếu chưa tồn tại
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'tasks' AND column_name = 'start_date'
    ) THEN
        ALTER TABLE tasks ADD COLUMN start_date DATE;
    END IF;
END $$;

-- Backfill dữ liệu start_date cho tasks cũ (mặc định bằng due_date - 3 ngày hoặc created_at hoặc hôm nay)
UPDATE tasks 
SET start_date = COALESCE(due_date - INTERVAL '3 days', created_at::date, CURRENT_DATE)
WHERE start_date IS NULL;

-- 2. Bảng task_dependencies
CREATE TABLE IF NOT EXISTS task_dependencies (
    id                 SERIAL      PRIMARY KEY,
    project_id         INT         NOT NULL,
    task_id            INT         NOT NULL,
    depends_on_task_id INT         NOT NULL,
    dependency_type    VARCHAR(30) NOT NULL DEFAULT 'FINISH_TO_START',
    lag_days           INT         NOT NULL DEFAULT 0,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_task_dep_project
        FOREIGN KEY (project_id) REFERENCES projects (id)
        ON DELETE CASCADE ON UPDATE CASCADE,

    CONSTRAINT fk_task_dep_task
        FOREIGN KEY (task_id) REFERENCES tasks (id)
        ON DELETE CASCADE ON UPDATE CASCADE,

    CONSTRAINT fk_task_dep_depends_on
        FOREIGN KEY (depends_on_task_id) REFERENCES tasks (id)
        ON DELETE CASCADE ON UPDATE CASCADE,

    CONSTRAINT chk_task_dep_no_self
        CHECK (task_id <> depends_on_task_id),

    CONSTRAINT uq_task_dep_pair
        UNIQUE (task_id, depends_on_task_id)
);

CREATE INDEX IF NOT EXISTS idx_task_deps_project    ON task_dependencies (project_id);
CREATE INDEX IF NOT EXISTS idx_task_deps_task       ON task_dependencies (task_id);
CREATE INDEX IF NOT EXISTS idx_task_deps_depends_on ON task_dependencies (depends_on_task_id);

-- 3. Bảng project_automations
CREATE TABLE IF NOT EXISTS project_automations (
    id                SERIAL       PRIMARY KEY,
    project_id        INT          NOT NULL,
    recipe_id         VARCHAR(50)  NOT NULL, -- 'AUTO_PROMPT_GATE2', 'AUTO_CLOSE_SUBTASKS', 'AUTO_ESCALATE_PRIORITY'
    recipe_name       VARCHAR(200) NOT NULL,
    description       TEXT         NOT NULL DEFAULT '',
    is_enabled        BOOLEAN      NOT NULL DEFAULT TRUE,
    config_json       TEXT         NOT NULL DEFAULT '{}',
    last_triggered_at TIMESTAMPTZ,
    trigger_count     INT          NOT NULL DEFAULT 0,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_proj_auto_project
        FOREIGN KEY (project_id) REFERENCES projects (id)
        ON DELETE CASCADE ON UPDATE CASCADE,

    CONSTRAINT uq_proj_auto_recipe
        UNIQUE (project_id, recipe_id)
);

CREATE INDEX IF NOT EXISTS idx_proj_auto_project ON project_automations (project_id);

-- Bật RLS bảo vệ bảng mới trên Supabase
ALTER TABLE task_dependencies  ENABLE ROW LEVEL SECURITY;
ALTER TABLE project_automations ENABLE ROW LEVEL SECURITY;


-- ============================================================================
-- PHẦN 2: Microsoft SQL Server (SSMS)
-- ============================================================================
/*
IF NOT EXISTS (
    SELECT 1 FROM sys.columns 
    WHERE object_id = OBJECT_ID('tasks') AND name = 'start_date'
)
BEGIN
    ALTER TABLE tasks ADD start_date DATE NULL;
END;

UPDATE tasks 
SET start_date = COALESCE(DATEADD(day, -3, due_date), CAST(created_at AS DATE), CAST(GETDATE() AS DATE))
WHERE start_date IS NULL;

IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'task_dependencies')
BEGIN
    CREATE TABLE task_dependencies (
        id                 INT IDENTITY(1,1) PRIMARY KEY,
        project_id         INT          NOT NULL,
        task_id            INT          NOT NULL,
        depends_on_task_id INT          NOT NULL,
        dependency_type    VARCHAR(30)  NOT NULL DEFAULT 'FINISH_TO_START',
        lag_days           INT          NOT NULL DEFAULT 0,
        created_at         DATETIME2    NOT NULL DEFAULT GETDATE(),

        CONSTRAINT fk_task_dep_project
            FOREIGN KEY (project_id) REFERENCES projects (id)
            ON DELETE CASCADE,

        CONSTRAINT fk_task_dep_task
            FOREIGN KEY (task_id) REFERENCES tasks (id)
            ON DELETE CASCADE,

        -- SQL Server tránh Multiple Cascade Paths: NO ACTION trên depends_on_task_id
        CONSTRAINT fk_task_dep_depends_on
            FOREIGN KEY (depends_on_task_id) REFERENCES tasks (id)
            ON DELETE NO ACTION,

        CONSTRAINT chk_task_dep_no_self
            CHECK (task_id <> depends_on_task_id),

        CONSTRAINT uq_task_dep_pair
            UNIQUE (task_id, depends_on_task_id)
    );

    CREATE INDEX idx_task_deps_project    ON task_dependencies (project_id);
    CREATE INDEX idx_task_deps_task       ON task_dependencies (task_id);
    CREATE INDEX idx_task_deps_depends_on ON task_dependencies (depends_on_task_id);
END;

IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'project_automations')
BEGIN
    CREATE TABLE project_automations (
        id                INT IDENTITY(1,1) PRIMARY KEY,
        project_id        INT           NOT NULL,
        recipe_id         VARCHAR(50)   NOT NULL,
        recipe_name       NVARCHAR(200) NOT NULL,
        description       NVARCHAR(MAX) NOT NULL DEFAULT '',
        is_enabled        BIT           NOT NULL DEFAULT 1,
        config_json       NVARCHAR(MAX) NOT NULL DEFAULT '{}',
        last_triggered_at DATETIME2     NULL,
        trigger_count     INT           NOT NULL DEFAULT 0,
        created_at        DATETIME2     NOT NULL DEFAULT GETDATE(),
        updated_at        DATETIME2     NOT NULL DEFAULT GETDATE(),

        CONSTRAINT fk_proj_auto_project
            FOREIGN KEY (project_id) REFERENCES projects (id)
            ON DELETE CASCADE,

        CONSTRAINT uq_proj_auto_recipe
            UNIQUE (project_id, recipe_id)
    );

    CREATE INDEX idx_proj_auto_project ON project_automations (project_id);
END;
*/

-- ============================================================================
-- PHẦN 3: Local MySQL (hibernate.hbm2ddl.auto=update tự sinh, hoặc chạy script)
-- ============================================================================
/*
ALTER TABLE tasks ADD COLUMN IF NOT EXISTS start_date DATE NULL;

UPDATE tasks 
SET start_date = COALESCE(DATE_SUB(due_date, INTERVAL 3 DAY), DATE(created_at), CURRENT_DATE)
WHERE start_date IS NULL;

CREATE TABLE IF NOT EXISTS task_dependencies (
    id                 INT AUTO_INCREMENT PRIMARY KEY,
    project_id         INT         NOT NULL,
    task_id            INT         NOT NULL,
    depends_on_task_id INT         NOT NULL,
    dependency_type    VARCHAR(30) NOT NULL DEFAULT 'FINISH_TO_START',
    lag_days           INT         NOT NULL DEFAULT 0,
    created_at         DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_task_dep_project
        FOREIGN KEY (project_id) REFERENCES projects (id)
        ON DELETE CASCADE ON UPDATE CASCADE,

    CONSTRAINT fk_task_dep_task
        FOREIGN KEY (task_id) REFERENCES tasks (id)
        ON DELETE CASCADE ON UPDATE CASCADE,

    CONSTRAINT fk_task_dep_depends_on
        FOREIGN KEY (depends_on_task_id) REFERENCES tasks (id)
        ON DELETE CASCADE ON UPDATE CASCADE,

    CONSTRAINT chk_task_dep_no_self
        CHECK (task_id <> depends_on_task_id),

    CONSTRAINT uq_task_dep_pair
        UNIQUE KEY (task_id, depends_on_task_id),

    INDEX idx_task_deps_project (project_id),
    INDEX idx_task_deps_task (task_id),
    INDEX idx_task_deps_depends_on (depends_on_task_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS project_automations (
    id                INT AUTO_INCREMENT PRIMARY KEY,
    project_id        INT          NOT NULL,
    recipe_id         VARCHAR(50)  NOT NULL,
    recipe_name       VARCHAR(200) NOT NULL,
    description       LONGTEXT     NOT NULL,
    is_enabled        TINYINT(1)   NOT NULL DEFAULT 1,
    config_json       LONGTEXT     NOT NULL,
    last_triggered_at DATETIME     NULL,
    trigger_count     INT          NOT NULL DEFAULT 0,
    created_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_proj_auto_project
        FOREIGN KEY (project_id) REFERENCES projects (id)
        ON DELETE CASCADE ON UPDATE CASCADE,

    CONSTRAINT uq_proj_auto_recipe
        UNIQUE KEY (project_id, recipe_id),

    INDEX idx_proj_auto_project (project_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
*/
