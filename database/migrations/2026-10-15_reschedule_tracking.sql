-- =========================================================================
-- DATABASE MIGRATION SCRIPT: DEADLINE RESCHEDULE COUNTER & AUDIT TRACKING
-- NGÀY TẠO: 2026-10-15
-- MỤC ĐÍCH:
--   Bổ sung cột original_due_date và reschedule_count vào bảng tasks
--   nhằm phát hiện và kiểm soát việc lùi hạn chót liên tục (Deadline Slippage).
-- =========================================================================

-- =========================================================================
-- PHẦN 1: DÀNH CHO MICROSOFT SQL SERVER (SSMS / AZURE SQL)
-- =========================================================================
IF COL_LENGTH('tasks', 'original_due_date') IS NULL
BEGIN
    ALTER TABLE tasks ADD original_due_date VARCHAR(50) NULL;
END
GO

IF COL_LENGTH('tasks', 'reschedule_count') IS NULL
BEGIN
    ALTER TABLE tasks ADD reschedule_count INT NOT NULL DEFAULT 0;
END
GO

UPDATE tasks 
SET original_due_date = due_date 
WHERE original_due_date IS NULL AND due_date IS NOT NULL;
GO

-- =========================================================================
-- PHẦN 2: DÀNH CHO POSTGRESQL / SUPABASE
-- =========================================================================
-- ALTER TABLE tasks ADD COLUMN IF NOT EXISTS original_due_date VARCHAR(50) NULL;
-- ALTER TABLE tasks ADD COLUMN IF NOT EXISTS reschedule_count INT NOT NULL DEFAULT 0;
-- 
-- UPDATE tasks 
-- SET original_due_date = due_date::text 
-- WHERE original_due_date IS NULL AND due_date IS NOT NULL;

