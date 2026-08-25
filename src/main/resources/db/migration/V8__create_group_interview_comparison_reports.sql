CREATE TABLE IF NOT EXISTS group_interview_comparison_reports (
    id BIGSERIAL PRIMARY KEY,
    room_id BIGINT NOT NULL,
    participant_user_id BIGINT NOT NULL,
    scope VARCHAR(20) NOT NULL,
    question_index INTEGER,
    content_score NUMERIC(5,2),
    speech_score NUMERIC(5,2),
    gaze_score NUMERIC(5,2),
    pose_score NUMERIC(5,2),
    blink_score NUMERIC(5,2),
    total_score NUMERIC(5,2),
    rank_number INTEGER,
    participant_count INTEGER NOT NULL,
    strength_summary TEXT,
    improvement_summary TEXT,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT fk_group_comparison_room FOREIGN KEY (room_id)
        REFERENCES group_interviews(id) ON DELETE CASCADE,
    CONSTRAINT ck_group_comparison_scope CHECK (scope IN ('QUESTION', 'OVERALL')),
    CONSTRAINT ck_group_comparison_status CHECK (status IN ('DONE', 'PARTIAL')),
    CONSTRAINT ck_group_comparison_question CHECK (
        (scope = 'QUESTION' AND question_index BETWEEN 0 AND 2)
        OR (scope = 'OVERALL' AND question_index IS NULL)
    )
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_group_comparison_question
    ON group_interview_comparison_reports
       (room_id, participant_user_id, question_index)
    WHERE scope = 'QUESTION';

CREATE UNIQUE INDEX IF NOT EXISTS uk_group_comparison_overall
    ON group_interview_comparison_reports (room_id, participant_user_id)
    WHERE scope = 'OVERALL';
