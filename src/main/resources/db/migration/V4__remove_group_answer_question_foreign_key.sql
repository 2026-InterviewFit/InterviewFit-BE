-- group_questions는 기존 data.sql에서 매 시작 시 재구성된다.
-- 답변에는 round/question_index 스냅샷이 있으므로 물리 FK 없이 선택적 question id만 보관한다.
ALTER TABLE group_interview_answers
    DROP CONSTRAINT IF EXISTS fk_group_answer_question;
