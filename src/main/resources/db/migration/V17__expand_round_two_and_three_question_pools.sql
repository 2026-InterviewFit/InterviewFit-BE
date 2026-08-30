-- 2라운드는 경험·가치관, 3라운드는 압박·위기 대응에 집중하도록
-- 서로 겹치지 않는 공통 질문을 각각 10개씩 추가한다.

INSERT INTO group_questions (job_role_id, round_number, question_text, created_at) VALUES
    (NULL, 2, '새로운 환경이나 조직에 적응하기 위해 본인이 가장 먼저 했던 행동과 그 결과를 말씀해 주세요.', CURRENT_TIMESTAMP),
    (NULL, 2, '본인이 주도적으로 업무 방식을 개선해 팀의 효율을 높였던 경험을 구체적으로 설명해 주세요.', CURRENT_TIMESTAMP),
    (NULL, 2, '협업 과정에서 상대방의 강점을 활용해 더 좋은 결과를 만든 경험이 있다면 말씀해 주세요.', CURRENT_TIMESTAMP),
    (NULL, 2, '장기적인 목표를 세우고 꾸준히 실행하기 위해 사용한 방법과 성과를 말씀해 주세요.', CURRENT_TIMESTAMP),
    (NULL, 2, '업무를 수행할 때 가장 중요하게 생각하는 원칙과 실제로 그 원칙을 지킨 사례는 무엇입니까?', CURRENT_TIMESTAMP),
    (NULL, 2, '처음에는 관심이 없었지만 경험을 통해 흥미와 전문성을 키운 분야가 있다면 설명해 주세요.', CURRENT_TIMESTAMP),
    (NULL, 2, '다른 사람의 도움이나 조언을 받아 본인의 부족한 점을 개선했던 경험을 말씀해 주세요.', CURRENT_TIMESTAMP),
    (NULL, 2, '팀 목표와 개인 목표를 동시에 달성하기 위해 역할과 시간을 조정했던 경험을 말씀해 주세요.', CURRENT_TIMESTAMP),
    (NULL, 2, '복잡한 내용을 상대방이 이해하기 쉽게 설명하거나 공유했던 경험을 구체적으로 말씀해 주세요.', CURRENT_TIMESTAMP),
    (NULL, 2, '본인이 생각하는 좋은 동료의 기준과 그 기준을 직접 실천했던 사례를 말씀해 주세요.', CURRENT_TIMESTAMP)
ON CONFLICT DO NOTHING;

INSERT INTO group_questions (job_role_id, round_number, question_text, created_at) VALUES
    (NULL, 3, '중요한 의사결정을 내린 직후 전제가 잘못됐다는 사실이 밝혀진다면 어떤 순서로 대응하시겠습니까?', CURRENT_TIMESTAMP),
    (NULL, 3, '팀원이 퇴사해 핵심 업무를 갑자기 인수해야 하지만 기존 일정도 촉박하다면 어떻게 대처하시겠습니까?', CURRENT_TIMESTAMP),
    (NULL, 3, '고객에게 약속한 결과를 기한 내 제공하기 어렵다는 사실을 발견했다면 언제, 어떻게 알리시겠습니까?', CURRENT_TIMESTAMP),
    (NULL, 3, '본인의 제안으로 시작한 프로젝트가 예상보다 큰 손실을 내고 있다면 어떤 조치를 취하시겠습니까?', CURRENT_TIMESTAMP),
    (NULL, 3, '보안이나 윤리 문제를 발견했지만 즉시 공개하면 서비스에 큰 혼란이 생긴다면 어떻게 행동하시겠습니까?', CURRENT_TIMESTAMP),
    (NULL, 3, '팀 내 갈등으로 의사결정이 계속 지연되고 최종 기한이 다가온다면 어떻게 결론을 이끌어 내시겠습니까?', CURRENT_TIMESTAMP),
    (NULL, 3, '핵심 업무를 마친 직후 요구사항이 잘못 전달됐다는 사실을 알게 되었다면 어떻게 수습하시겠습니까?', CURRENT_TIMESTAMP),
    (NULL, 3, '성과 목표 달성이 어려운 상황에서 팀의 사기까지 크게 떨어졌다면 무엇을 우선하시겠습니까?', CURRENT_TIMESTAMP),
    (NULL, 3, '상사가 사실과 다른 내용을 보고하도록 요구한다면 관계와 업무 영향을 고려해 어떻게 대응하시겠습니까?', CURRENT_TIMESTAMP),
    (NULL, 3, '여러 긴급 장애가 동시에 발생했지만 대응 인력이 부족하다면 어떤 기준으로 우선순위를 정하시겠습니까?', CURRENT_TIMESTAMP)
ON CONFLICT DO NOTHING;
