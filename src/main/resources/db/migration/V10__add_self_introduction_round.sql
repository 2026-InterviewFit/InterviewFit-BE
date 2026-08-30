-- 기존 질문 풀을 한 라운드씩 뒤로 이동하고 1라운드에 고정 자기소개 질문을 추가한다.
-- 과거 면접 답변의 round_number는 당시 기준을 유지하기 위해 변경하지 않는다.

UPDATE group_questions SET round_number = 4 WHERE round_number = 3;
UPDATE group_questions SET round_number = 3 WHERE round_number = 2;
UPDATE group_questions SET round_number = 2 WHERE round_number = 1;

INSERT INTO group_questions (job_role_id, round_number, question_text, created_at)
VALUES (NULL, 1, '간단하게 자기소개를 해주세요.', CURRENT_TIMESTAMP)
ON CONFLICT DO NOTHING;
