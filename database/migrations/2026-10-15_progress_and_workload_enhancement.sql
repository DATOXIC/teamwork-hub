-- =============================================================================
-- MIGRATION: 2026-10-15_progress_and_workload_enhancement.sql
-- Nâng cấp mô hình dữ liệu giám sát tiến độ định lượng & quản trị tải công việc
-- Hỗ trợ: PostgreSQL (Supabase) và Microsoft SQL Server (SSMS)
-- =============================================================================

-- =============================================================================
-- 1. BẢN CHO MICROSOFT SQL SERVER (SSMS)
-- =============================================================================
IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('tasks') AND name = 'estimated_hours')
BEGIN
    ALTER TABLE tasks ADD estimated_hours FLOAT NOT NULL DEFAULT 0;
END;

IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('tasks') AND name = 'actual_hours')
BEGIN
    ALTER TABLE tasks ADD actual_hours FLOAT NOT NULL DEFAULT 0;
END;

IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('tasks') AND name = 'is_milestone')
BEGIN
    ALTER TABLE tasks ADD is_milestone BIT NOT NULL DEFAULT 0;
END;

IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('subtasks') AND name = 'estimated_hours')
BEGIN
    ALTER TABLE subtasks ADD estimated_hours FLOAT NOT NULL DEFAULT 0;
END;

IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('subtasks') AND name = 'actual_hours')
BEGIN
    ALTER TABLE subtasks ADD actual_hours FLOAT NOT NULL DEFAULT 0;
END;

IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('project_members') AND name = 'weekly_capacity_hours')
BEGIN
    ALTER TABLE project_members ADD weekly_capacity_hours INT NOT NULL DEFAULT 40;
END;

-- =============================================================================
-- 2. BẢN CHO POSTGRESQL (SUPABASE)
-- =============================================================================
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='tasks' AND column_name='estimated_hours') THEN
        ALTER TABLE tasks ADD COLUMN estimated_hours DOUBLE PRECISION DEFAULT 0;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='tasks' AND column_name='actual_hours') THEN
        ALTER TABLE tasks ADD COLUMN actual_hours DOUBLE PRECISION DEFAULT 0;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='tasks' AND column_name='is_milestone') THEN
        ALTER TABLE tasks ADD COLUMN is_milestone BOOLEAN DEFAULT FALSE;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='subtasks' AND column_name='estimated_hours') THEN
        ALTER TABLE subtasks ADD COLUMN estimated_hours DOUBLE PRECISION DEFAULT 0;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='subtasks' AND column_name='actual_hours') THEN
        ALTER TABLE subtasks ADD COLUMN actual_hours DOUBLE PRECISION DEFAULT 0;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='project_members' AND column_name='weekly_capacity_hours') THEN
        ALTER TABLE project_members ADD COLUMN weekly_capacity_hours INT DEFAULT 40;
    END IF;
END $$;
