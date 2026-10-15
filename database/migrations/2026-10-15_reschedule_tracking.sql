-- =========================================================================
-- DATABASE MIGRATION SCRIPT: DEADLINE RESCHEDULE COUNTER & AUDIT TRACKING
-- NGÀY TẠO: 2026-10-15
-- MỤC ĐÍCH:
--   Bổ sung cột original_due_date và reschedule_count vào bảng tasks
--   nhằm phát hiện và kiểm soát việc lùi hạn chót liên tục (Deadline Slippage).
-- =========================================================================

-- 1. Thêm cột original_due_date và reschedule_count cho PostgreSQL / Supabase
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'tasks' AND column_name = 'original_due_date'
    ) THEN
        ALTER TABLE tasks ADD COLUMN original_due_date VARCHAR(50) NULL;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'tasks' AND column_name = 'reschedule_count'
    ) THEN
        ALTER TABLE tasks ADD COLUMN reschedule_count INT NOT NULL DEFAULT 0;
    END IF;
END $$;

-- 2. Đồng bộ dữ liệu cũ: Nếu original_due_date chưa có, gán bằng due_date hiện tại
UPDATE tasks 
SET original_due_date = due_date 
WHERE original_due_date IS NULL AND due_date IS NOT NULL;
