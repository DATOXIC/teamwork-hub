-- ============================================================================
-- MIGRATION: Reset Subtask Status Contamination
-- Ngày: 2026-09-19
-- Mô tả: Tất cả subtask hiện tại có status = 'APPROVED' đều bị "ô nhiễm" 
--         (không phân biệt được giữa assignee tự tick vs Task Lead duyệt thật).
--         Script này reset tất cả về DONE để Task Lead duyệt lại nếu cần.
-- ============================================================================

-- Bước 1: Xem trước data bị ảnh hưởng
SELECT id, title, status, assignee_name, completed 
FROM subtasks 
WHERE status = 'APPROVED';

-- Bước 2: Reset tất cả APPROVED → DONE
UPDATE subtasks 
SET status = 'DONE' 
WHERE status = 'APPROVED';

-- Bước 3: Xác nhận kết quả
SELECT id, title, status, assignee_name, completed 
FROM subtasks 
WHERE status IN ('DONE', 'APPROVED');
