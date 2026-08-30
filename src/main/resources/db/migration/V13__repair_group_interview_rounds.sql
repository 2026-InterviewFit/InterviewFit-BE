-- 과거 data.sql이 마이그레이션 적용 후 다시 실행된 DB의 라운드 데이터를 복구한다.

INSERT INTO group_questions (job_role_id, round_number, question_text, created_at)
VALUES (NULL, 1, '간단하게 자기소개를 해주세요.', CURRENT_TIMESTAMP)
ON CONFLICT DO NOTHING;

-- 1라운드에 남은 기존 공통 질문을 2라운드로 옮긴다.
DELETE FROM group_questions source
USING group_questions target
WHERE source.round_number = 1
  AND source.question_text <> '간단하게 자기소개를 해주세요.'
  AND target.round_number = 2
  AND COALESCE(target.job_role_id, 0) = COALESCE(source.job_role_id, 0)
  AND target.question_text = source.question_text;

UPDATE group_questions
SET round_number = 2
WHERE round_number = 1
  AND question_text <> '간단하게 자기소개를 해주세요.';

-- 구 data.sql이 2라운드에 다시 넣은 압박 질문을 3라운드로 옮긴다.
DELETE FROM group_questions source
USING group_questions target
WHERE source.round_number = 2
  AND source.question_text IN (
      '만약 입사 후 본인이 원하지 않는 부서나 직무에 배치된다면 어떻게 하겠습니까?',
      '프로젝트 마감이 내일 모레인데 핵심 기능에 치명적인 에러가 발생했습니다. 팀원들과 의견이 갈릴 때 본인은 어떻게 리드하시겠습니까?'
  )
  AND target.round_number = 3
  AND COALESCE(target.job_role_id, 0) = COALESCE(source.job_role_id, 0)
  AND target.question_text = source.question_text;

UPDATE group_questions
SET round_number = 3
WHERE round_number = 2
  AND question_text IN (
      '만약 입사 후 본인이 원하지 않는 부서나 직무에 배치된다면 어떻게 하겠습니까?',
      '프로젝트 마감이 내일 모레인데 핵심 기능에 치명적인 에러가 발생했습니다. 팀원들과 의견이 갈릴 때 본인은 어떻게 리드하시겠습니까?'
  );

-- 3라운드에 남은 직무별 질문을 4라운드로 옮긴다.
DELETE FROM group_questions source
USING group_questions target
WHERE source.round_number = 3
  AND source.job_role_id IS NOT NULL
  AND target.round_number = 4
  AND target.job_role_id = source.job_role_id
  AND target.question_text = source.question_text;

UPDATE group_questions
SET round_number = 4
WHERE round_number = 3
  AND job_role_id IS NOT NULL;
