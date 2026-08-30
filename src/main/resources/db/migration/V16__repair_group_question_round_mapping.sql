-- 과거 3라운드 구조의 질문이 남아 있어도 현재 4라운드 구조에 맞게 재배치한다.
-- 공통/압박 질문은 job_role_id가 없고, 직무 질문은 job_role_id를 가진다는 규칙을 강제한다.

DROP INDEX IF EXISTS uk_group_question_seed;

UPDATE group_questions
SET round_number = 1,
    job_role_id = NULL
WHERE question_text = '간단하게 자기소개를 해주세요.';

UPDATE group_questions
SET round_number = 4
WHERE job_role_id IS NOT NULL;

UPDATE group_questions
SET round_number = 2
WHERE job_role_id IS NULL
  AND question_text <> '간단하게 자기소개를 해주세요.';

UPDATE group_questions
SET round_number = 3
WHERE job_role_id IS NULL
  AND question_text IN (
      '만약 입사 후 본인이 원하지 않는 부서나 직무에 배치된다면 어떻게 하겠습니까?',
      '프로젝트 마감이 내일 모레인데 핵심 기능에 치명적인 에러가 발생했습니다. 팀원들과 의견이 갈릴 때 본인은 어떻게 리드하시겠습니까?',
      '중요한 업무에서 본인의 판단이 틀렸다는 사실을 뒤늦게 알게 되었다면 어떻게 수습하고 신뢰를 회복하시겠습니까?',
      '상사와 고객이 서로 상반된 요구를 하면서 모두 즉시 처리해 달라고 한다면 어떤 기준으로 대응하시겠습니까?',
      '팀의 성과가 좋지 않은 상황에서 본인이 맡지 않은 문제까지 책임져야 한다면 어떻게 행동하시겠습니까?',
      '충분히 준비했다고 생각한 제안이 반복해서 거절된다면 원인을 어떻게 파악하고 다음 행동을 정하시겠습니까?',
      '동료의 실수를 공개해야 조직의 손실을 막을 수 있지만 관계가 크게 악화될 수 있다면 어떻게 하시겠습니까?',
      '업무 성과를 내기 위해 익숙하지 않은 역할을 갑자기 맡아야 한다면 무엇부터 준비하시겠습니까?',
      '팀원 대부분이 동의한 결정에 심각한 위험이 있다고 판단했을 때 어떻게 의견을 제시하시겠습니까?',
      '노력한 만큼 성과를 인정받지 못했다고 느낄 때 조직과 어떻게 소통하고 다음 목표를 세우시겠습니까?',
      '제한된 시간 안에 품질과 속도 중 하나를 우선해야 한다면 어떤 근거로 결정하고 결과를 책임지시겠습니까?',
      '입사 직후 팀의 기존 방식에 비효율이 보이지만 구성원들이 변화를 원하지 않는다면 어떻게 접근하시겠습니까?'
  );

DELETE FROM group_questions duplicate
USING group_questions original
WHERE duplicate.id > original.id
  AND duplicate.round_number = original.round_number
  AND COALESCE(duplicate.job_role_id, 0) = COALESCE(original.job_role_id, 0)
  AND duplicate.question_text = original.question_text;

CREATE UNIQUE INDEX uk_group_question_seed
    ON group_questions (
        round_number,
        COALESCE(job_role_id, 0),
        question_text
    );
