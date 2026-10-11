-- ============================================================================
-- MIGRATION: 2026-10-12_add_task_submissions.sql
-- Mục đích: Lưu trữ lịch sử nộp bàn giao & phản hồi nghiệm thu đa phiên bản
--           (Multi-revision Submission History & PM Review Feedback)
-- ============================================================================

-- ============================================================================
-- PHẦN 1: PostgreSQL / Supabase (Chạy trên Supabase SQL Editor)
-- ============================================================================
CREATE TABLE IF NOT EXISTS task_submissions (
    id               SERIAL       PRIMARY KEY,
    task_id          INT          NOT NULL,
    revision_number  INT          NOT NULL DEFAULT 1,
    submitted_by     INT,
    submission_note  TEXT         NOT NULL DEFAULT '',
    summary          TEXT         NOT NULL DEFAULT '',
    demo_url         VARCHAR(500) NOT NULL DEFAULT '',
    code_url         VARCHAR(500) NOT NULL DEFAULT '',
    test_result      TEXT         NOT NULL DEFAULT '',
    testing_guide    TEXT         NOT NULL DEFAULT '',
    file_path        VARCHAR(255) NOT NULL DEFAULT '',
    status           VARCHAR(20)  NOT NULL DEFAULT 'SUBMITTED', -- SUBMITTED, REVISE, REJECTED, APPROVED
    pm_feedback      TEXT         NOT NULL DEFAULT '',
    quality_rating   INT          NOT NULL DEFAULT 0,
    reviewed_by      INT,
    submitted_at     VARCHAR(50)  NOT NULL DEFAULT '',
    reviewed_at      VARCHAR(50)  NOT NULL DEFAULT '',
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_task_submissions_task
        FOREIGN KEY (task_id) REFERENCES tasks (id)
        ON DELETE CASCADE ON UPDATE CASCADE,

    CONSTRAINT fk_task_submissions_submitted_by
        FOREIGN KEY (submitted_by) REFERENCES users (id)
        ON DELETE SET NULL ON UPDATE CASCADE,

    CONSTRAINT fk_task_submissions_reviewed_by
        FOREIGN KEY (reviewed_by) REFERENCES users (id)
        ON DELETE SET NULL ON UPDATE CASCADE,

    CONSTRAINT uq_task_submissions_task_revision
        UNIQUE (task_id, revision_number)
);

CREATE INDEX IF NOT EXISTS idx_task_submissions_task_id ON task_submissions (task_id);
CREATE INDEX IF NOT EXISTS idx_task_submissions_status ON task_submissions (status);

-- Tự động chuyển đổi dữ liệu bàn giao cũ (Backfill) thành Revision 1 nếu có
INSERT INTO task_submissions (
    task_id, revision_number, submitted_by, submission_note, file_path, status,
    pm_feedback, quality_rating, submitted_at, reviewed_at
)
SELECT
    t.id,
    1,
    t.assignee_id,
    COALESCE(t.final_deliverable_note, 'Đã hoàn thành công việc theo yêu cầu.'),
    COALESCE(t.deliverable_file, ''),
    CASE 
        WHEN t.status::text = 'DONE' THEN 'APPROVED'
        WHEN t.status::text IN ('SUBMITTED', 'REVISE', 'REJECTED') THEN t.status::text
        ELSE 'SUBMITTED'
    END,
    COALESCE(t.pm_feedback, ''),
    COALESCE(t.quality_rating, 0),
    COALESCE(to_char(t.submitted_at, 'DD/MM/YYYY HH24:MI'), ''),
    COALESCE(to_char(t.reviewed_at, 'DD/MM/YYYY HH24:MI'), '')
FROM tasks t
WHERE (
    (t.final_deliverable_note IS NOT NULL AND t.final_deliverable_note <> '')
    OR (t.deliverable_file IS NOT NULL AND t.deliverable_file <> '')
    OR t.status::text IN ('SUBMITTED', 'REVISE', 'REJECTED', 'DONE')
)
AND NOT EXISTS (
    SELECT 1 FROM task_submissions ts WHERE ts.task_id = t.id
);


-- ============================================================================
-- PHẦN 2: Microsoft SQL Server (SSMS)
-- ============================================================================
/*
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'task_submissions')
BEGIN
    CREATE TABLE task_submissions (
        id               INT IDENTITY(1,1) PRIMARY KEY,
        task_id          INT               NOT NULL,
        revision_number  INT               NOT NULL DEFAULT 1,
        submitted_by     INT               NULL,
        submission_note  NVARCHAR(MAX)     NOT NULL DEFAULT '',
        summary          NVARCHAR(MAX)     NOT NULL DEFAULT '',
        demo_url         NVARCHAR(500)     NOT NULL DEFAULT '',
        code_url         NVARCHAR(500)     NOT NULL DEFAULT '',
        test_result      NVARCHAR(MAX)     NOT NULL DEFAULT '',
        testing_guide    NVARCHAR(MAX)     NOT NULL DEFAULT '',
        file_path        NVARCHAR(255)     NOT NULL DEFAULT '',
        status           VARCHAR(20)       NOT NULL DEFAULT 'SUBMITTED',
        pm_feedback      NVARCHAR(MAX)     NOT NULL DEFAULT '',
        quality_rating   INT               NOT NULL DEFAULT 0,
        reviewed_by      INT               NULL,
        submitted_at     NVARCHAR(50)      NOT NULL DEFAULT '',
        reviewed_at      NVARCHAR(50)      NOT NULL DEFAULT '',
        created_at       DATETIME2         NOT NULL DEFAULT GETDATE(),

        CONSTRAINT fk_task_submissions_task
            FOREIGN KEY (task_id) REFERENCES tasks (id)
            ON DELETE CASCADE,

        CONSTRAINT fk_task_submissions_submitted_by
            FOREIGN KEY (submitted_by) REFERENCES users (id)
            ON DELETE NO ACTION,

        CONSTRAINT fk_task_submissions_reviewed_by
            FOREIGN KEY (reviewed_by) REFERENCES users (id)
            ON DELETE NO ACTION,

        CONSTRAINT uq_task_submissions_task_revision
            UNIQUE (task_id, revision_number)
    );

    CREATE INDEX idx_task_submissions_task_id ON task_submissions (task_id);
END
*/

-- ============================================================================
-- PHẦN 3: Local MySQL (hibernate.hbm2ddl.auto=update tự sinh, hoặc chạy script)
-- ============================================================================
/*
CREATE TABLE IF NOT EXISTS task_submissions (
    id               INT AUTO_INCREMENT PRIMARY KEY,
    task_id          INT          NOT NULL,
    revision_number  INT          NOT NULL DEFAULT 1,
    submitted_by     INT          NULL,
    submission_note  LONGTEXT     NOT NULL,
    summary          LONGTEXT     NOT NULL,
    demo_url         VARCHAR(500) NOT NULL DEFAULT '',
    code_url         VARCHAR(500) NOT NULL DEFAULT '',
    test_result      LONGTEXT     NOT NULL,
    testing_guide    LONGTEXT     NOT NULL,
    file_path        VARCHAR(255) NOT NULL DEFAULT '',
    status           VARCHAR(20)  NOT NULL DEFAULT 'SUBMITTED',
    pm_feedback      LONGTEXT     NOT NULL,
    quality_rating   INT          NOT NULL DEFAULT 0,
    reviewed_by      INT          NULL,
    submitted_at     VARCHAR(50)  NOT NULL DEFAULT '',
    reviewed_at      VARCHAR(50)  NOT NULL DEFAULT '',
    created_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_task_submissions_task
        FOREIGN KEY (task_id) REFERENCES tasks (id)
        ON DELETE CASCADE ON UPDATE CASCADE,

    CONSTRAINT fk_task_submissions_submitted_by
        FOREIGN KEY (submitted_by) REFERENCES users (id)
        ON DELETE SET NULL ON UPDATE CASCADE,

    CONSTRAINT fk_task_submissions_reviewed_by
        FOREIGN KEY (reviewed_by) REFERENCES users (id)
        ON DELETE SET NULL ON UPDATE CASCADE,

    CONSTRAINT uq_task_submissions_task_revision
        UNIQUE KEY (task_id, revision_number),

    INDEX idx_task_submissions_task_id (task_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
*/
