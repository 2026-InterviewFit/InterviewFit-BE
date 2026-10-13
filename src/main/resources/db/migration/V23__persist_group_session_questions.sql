-- 선택된 질문을 세션에 영속화하여 재접속, 재시작, 다중 인스턴스에서도 동일하게 유지한다.
ALTER TABLE group_questions
    ADD COLUMN IF NOT EXISTS selection_enabled BOOLEAN;

-- 기존 행을 먼저 백필한 뒤 NOT NULL을 적용해야 데이터가 있는 DB에서도 안전하다.
UPDATE group_questions
SET selection_enabled = TRUE
WHERE selection_enabled IS NULL;

ALTER TABLE group_questions
    ALTER COLUMN selection_enabled SET DEFAULT TRUE,
    ALTER COLUMN selection_enabled SET NOT NULL;

ALTER TABLE group_interview_sessions
    ADD COLUMN IF NOT EXISTS round1_question_id BIGINT,
    ADD COLUMN IF NOT EXISTS round2_question_id BIGINT,
    ADD COLUMN IF NOT EXISTS round3_question_id BIGINT,
    ADD COLUMN IF NOT EXISTS round4_question_id BIGINT;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_group_session_round1_question') THEN
        ALTER TABLE group_interview_sessions ADD CONSTRAINT fk_group_session_round1_question
            FOREIGN KEY (round1_question_id) REFERENCES group_questions(id) ON DELETE RESTRICT;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_group_session_round2_question') THEN
        ALTER TABLE group_interview_sessions ADD CONSTRAINT fk_group_session_round2_question
            FOREIGN KEY (round2_question_id) REFERENCES group_questions(id) ON DELETE RESTRICT;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_group_session_round3_question') THEN
        ALTER TABLE group_interview_sessions ADD CONSTRAINT fk_group_session_round3_question
            FOREIGN KEY (round3_question_id) REFERENCES group_questions(id) ON DELETE RESTRICT;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_group_session_round4_question') THEN
        ALTER TABLE group_interview_sessions ADD CONSTRAINT fk_group_session_round4_question
            FOREIGN KEY (round4_question_id) REFERENCES group_questions(id) ON DELETE RESTRICT;
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_group_answer_user_created_question
    ON group_interview_answers (participant_user_id, created_at DESC, group_question_id);
