ALTER TABLE ai_evaluation
    ADD COLUMN IF NOT EXISTS external_ai_analysis_json TEXT;

ALTER TABLE group_interview_answer_analyses
    ADD COLUMN IF NOT EXISTS external_ai_analysis_json TEXT;
