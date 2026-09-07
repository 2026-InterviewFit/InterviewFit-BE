ALTER TABLE eye_gaze_reports
ADD COLUMN IF NOT EXISTS interview_answer_id BIGINT;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_eye_gaze_reports_interview_answer'
    ) THEN
        ALTER TABLE eye_gaze_reports
        ADD CONSTRAINT fk_eye_gaze_reports_interview_answer
        FOREIGN KEY (interview_answer_id)
        REFERENCES interview_answers(id)
        ON DELETE CASCADE;
    END IF;
END $$;