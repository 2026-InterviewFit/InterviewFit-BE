CREATE TABLE IF NOT EXISTS group_interview_answers (
    id BIGSERIAL PRIMARY KEY,
    group_interview_id BIGINT NOT NULL,
    participant_user_id BIGINT NOT NULL,
    group_question_id BIGINT,
    round_number INTEGER NOT NULL,
    question_index INTEGER NOT NULL,
    answer_started_at TIMESTAMP WITH TIME ZONE NOT NULL,
    answer_completed_at TIMESTAMP WITH TIME ZONE,
    completion_type VARCHAR(20),
    video_object_key VARCHAR(255),
    video_original_file_name VARCHAR(255),
    video_content_type VARCHAR(255),
    video_file_size BIGINT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_group_answer_interview
        FOREIGN KEY (group_interview_id) REFERENCES group_interviews(id) ON DELETE CASCADE,
    CONSTRAINT fk_group_answer_question
        FOREIGN KEY (group_question_id) REFERENCES group_questions(id) ON DELETE SET NULL,
    CONSTRAINT uk_group_answer_turn
        UNIQUE (group_interview_id, participant_user_id, round_number, question_index),
    CONSTRAINT uk_group_answer_video_object_key UNIQUE (video_object_key),
    CONSTRAINT ck_group_answer_completion_type
        CHECK (completion_type IS NULL OR completion_type IN ('COMPLETED', 'TIMED_OUT'))
);

ALTER TABLE group_interview_sessions
    ADD COLUMN IF NOT EXISTS current_answer_id BIGINT;

CREATE INDEX IF NOT EXISTS idx_group_answers_room
    ON group_interview_answers (group_interview_id, id);
