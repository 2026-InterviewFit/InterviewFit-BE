ALTER TABLE speech_report
    ADD COLUMN IF NOT EXISTS speech_score_status VARCHAR(30),
    ADD COLUMN IF NOT EXISTS speech_score_reason TEXT;

ALTER TABLE group_interview_answer_analyses
    ADD COLUMN IF NOT EXISTS speech_score INTEGER,
    ADD COLUMN IF NOT EXISTS speech_score_status VARCHAR(30),
    ADD COLUMN IF NOT EXISTS speech_score_reason TEXT;

ALTER TABLE speech_report
    ADD CONSTRAINT ck_speech_report_score_status
        CHECK (speech_score_status IS NULL OR speech_score_status IN ('SCORED', 'INSUFFICIENT_DATA'));

ALTER TABLE group_interview_answer_analyses
    ADD CONSTRAINT ck_group_answer_speech_score
        CHECK (speech_score IS NULL OR (speech_score >= 0 AND speech_score <= 100)),
    ADD CONSTRAINT ck_group_answer_speech_score_status
        CHECK (speech_score_status IS NULL OR speech_score_status IN ('SCORED', 'INSUFFICIENT_DATA'));
