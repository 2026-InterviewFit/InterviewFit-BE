-- 특정 압박 질문이 지나치게 자주 반복되지 않도록 3라운드 공통 질문 풀을 확장한다.
INSERT INTO group_questions (job_role_id, round_number, question_text, created_at) VALUES
    (NULL, 3, '중요한 업무에서 본인의 판단이 틀렸다는 사실을 뒤늦게 알게 되었다면 어떻게 수습하고 신뢰를 회복하시겠습니까?', CURRENT_TIMESTAMP),
    (NULL, 3, '상사와 고객이 서로 상반된 요구를 하면서 모두 즉시 처리해 달라고 한다면 어떤 기준으로 대응하시겠습니까?', CURRENT_TIMESTAMP),
    (NULL, 3, '팀의 성과가 좋지 않은 상황에서 본인이 맡지 않은 문제까지 책임져야 한다면 어떻게 행동하시겠습니까?', CURRENT_TIMESTAMP),
    (NULL, 3, '충분히 준비했다고 생각한 제안이 반복해서 거절된다면 원인을 어떻게 파악하고 다음 행동을 정하시겠습니까?', CURRENT_TIMESTAMP),
    (NULL, 3, '동료의 실수를 공개해야 조직의 손실을 막을 수 있지만 관계가 크게 악화될 수 있다면 어떻게 하시겠습니까?', CURRENT_TIMESTAMP),
    (NULL, 3, '업무 성과를 내기 위해 익숙하지 않은 역할을 갑자기 맡아야 한다면 무엇부터 준비하시겠습니까?', CURRENT_TIMESTAMP),
    (NULL, 3, '팀원 대부분이 동의한 결정에 심각한 위험이 있다고 판단했을 때 어떻게 의견을 제시하시겠습니까?', CURRENT_TIMESTAMP),
    (NULL, 3, '노력한 만큼 성과를 인정받지 못했다고 느낄 때 조직과 어떻게 소통하고 다음 목표를 세우시겠습니까?', CURRENT_TIMESTAMP),
    (NULL, 3, '제한된 시간 안에 품질과 속도 중 하나를 우선해야 한다면 어떤 근거로 결정하고 결과를 책임지시겠습니까?', CURRENT_TIMESTAMP),
    (NULL, 3, '입사 직후 팀의 기존 방식에 비효율이 보이지만 구성원들이 변화를 원하지 않는다면 어떻게 접근하시겠습니까?', CURRENT_TIMESTAMP)
ON CONFLICT DO NOTHING;
