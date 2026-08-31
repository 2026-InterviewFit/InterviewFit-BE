ALTER TABLE group_interview_sessions
    ADD COLUMN IF NOT EXISTS answer_started_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN IF NOT EXISTS answer_deadline_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

CREATE INDEX IF NOT EXISTS idx_group_interview_sessions_answer_deadline
    ON group_interview_sessions (answer_deadline_at)
    WHERE status = 'ANSWERING';
