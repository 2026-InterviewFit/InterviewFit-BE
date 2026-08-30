ALTER TABLE group_interview_answers
    DROP CONSTRAINT IF EXISTS ck_group_answer_completion_type;

ALTER TABLE group_interview_answers
    ADD CONSTRAINT ck_group_answer_completion_type
        CHECK (completion_type IS NULL OR completion_type IN (
            'COMPLETED',
            'TIMED_OUT',
            'SKIPPED',
            'RECORDING_FAILED'
        ));
