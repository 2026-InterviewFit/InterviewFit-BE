ALTER TABLE group_interview_sessions
    ADD COLUMN IF NOT EXISTS answer_start_deadline_at TIMESTAMP WITH TIME ZONE;

CREATE INDEX IF NOT EXISTS idx_group_interview_sessions_answer_start_deadline
    ON group_interview_sessions (answer_start_deadline_at)
    WHERE status = 'QUESTION_VIEW';
