CREATE TABLE IF NOT EXISTS group_interview_answer_analyses (
    id BIGSERIAL PRIMARY KEY,
    group_interview_answer_id BIGINT NOT NULL UNIQUE,
    status VARCHAR(20) NOT NULL DEFAULT 'UPLOADED',
    speech_text TEXT,
    speech_result_json TEXT,
    ai_evaluation_json TEXT,
    eye_blinking_result_json TEXT,
    eye_gaze_result_json TEXT,
    pose_result_json TEXT,
    error_message TEXT,
    analysis_started_at TIMESTAMP WITH TIME ZONE,
    analysis_completed_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_group_answer_analysis_answer
        FOREIGN KEY (group_interview_answer_id)
        REFERENCES group_interview_answers(id)
        ON DELETE CASCADE,
    CONSTRAINT ck_group_answer_analysis_status
        CHECK (status IN ('UPLOADED', 'RUNNING', 'DONE', 'FAILED'))
);
