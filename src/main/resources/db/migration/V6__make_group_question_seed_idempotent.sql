CREATE UNIQUE INDEX IF NOT EXISTS uk_group_question_seed
    ON group_questions (
        round_number,
        COALESCE(job_role_id, 0),
        question_text
    );
