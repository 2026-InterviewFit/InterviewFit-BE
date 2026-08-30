-- 운영 환경에서 data.sql을 실행하지 않아도 직무와 면접 질문 풀이 완전하도록 보장한다.

INSERT INTO job_roles (id, category, name)
    OVERRIDING SYSTEM VALUE
VALUES
    (1, 'IT/개발', '프론트엔드'),
    (2, 'IT/개발', '백엔드'),
    (3, 'IT/개발', 'AI'),
    (4, 'IT/개발', '데이터분석'),
    (5, '비즈니스', '마케팅'),
    (6, '비즈니스', '인사'),
    (7, '비즈니스', '재무·회계'),
    (8, '디자인', 'UX/UI 디자이너'),
    (9, '디자인', '영상 제작'),
    (10, '디자인', '그래픽 디자이너'),
    (11, '기타', '기타')
ON CONFLICT (id) DO UPDATE
SET category = EXCLUDED.category,
    name = EXCLUDED.name;

SELECT setval(
    pg_get_serial_sequence('job_roles', 'id'),
    COALESCE((SELECT MAX(id) FROM job_roles), 1),
    true
);

INSERT INTO group_questions (job_role_id, round_number, question_text, created_at) VALUES
    (NULL, 2, '본인이 생각하는 자신의 가장 큰 강점과, 그것이 해당 직무에 어떻게 기여할 수 있는지 말씀해 주세요.', CURRENT_TIMESTAMP),
    (NULL, 2, '팀 프로젝트나 조직 생활 중 가장 갈등이 심했던 경험과, 이를 해결하기 위해 본인이 취한 행동은 무엇입니까?', CURRENT_TIMESTAMP),
    (NULL, 3, '만약 입사 후 본인이 원하지 않는 부서나 직무에 배치된다면 어떻게 하겠습니까?', CURRENT_TIMESTAMP),
    (NULL, 3, '프로젝트 마감이 내일 모레인데 핵심 기능에 치명적인 에러가 발생했습니다. 팀원들과 의견이 갈릴 때 본인은 어떻게 리드하시겠습니까?', CURRENT_TIMESTAMP),
    (1, 4, '최근 트래픽이 급증하여 프론트엔드 렌더링 성능 저하가 발생했습니다. 클라이언트 측에서 우선적으로 고려해야 할 최적화 방안은 무엇입니까?', CURRENT_TIMESTAMP),
    (2, 4, '최근 트래픽이 급증하여 서버 장애가 발생했습니다. 원인을 분석하고 해결하기 위해 가장 먼저 어떤 조치를 취하시겠습니까?', CURRENT_TIMESTAMP),
    (3, 4, '실제 운영 환경(Production)에서 모델의 예측 지연 시간(Latency)이 급격히 증가했습니다. 이를 개선하기 위한 아키텍처적 접근은 무엇입니까?', CURRENT_TIMESTAMP),
    (4, 4, '핵심 데이터에 결측치와 이상치가 다수 발견됐지만 의사결정 일정은 미룰 수 없습니다. 분석 신뢰성을 확보하면서 결과를 제시하기 위해 어떤 판단과 조치를 하시겠습니까?', CURRENT_TIMESTAMP),
    (5, 4, '진행 중인 마케팅 캠페인의 성과가 예상보다 크게 낮고 남은 예산도 제한적입니다. 타깃과 채널별 반응을 어떻게 분석하고 예산을 재배분하시겠습니까?', CURRENT_TIMESTAMP),
    (6, 4, '핵심 구성원의 이탈이 이어지고 팀 내 갈등도 커지고 있습니다. 원인을 파악하고 조직 안정화를 위해 어떤 조치를 우선하시겠습니까?', CURRENT_TIMESTAMP),
    (7, 4, '결산 직전에 주요 계정의 오류와 예산 초과가 동시에 발견됐습니다. 재무 리스크를 통제하면서 보고 일정을 지키기 위해 어떻게 대응하시겠습니까?', CURRENT_TIMESTAMP),
    (8, 4, '신규 서비스에서 특정 단계의 사용자 이탈이 높지만 개발 일정상 모든 개선안을 반영할 수 없습니다. 어떤 근거로 디자인 우선순위를 정하고 개발팀과 합의하시겠습니까?', CURRENT_TIMESTAMP),
    (9, 4, '핵심 촬영 일정에 차질이 생기고 편집 방향에 대한 이해관계자의 의견도 엇갈리고 있습니다. 제한된 시간과 예산 안에서 콘텐츠 품질을 지키기 위해 어떻게 조정하시겠습니까?', CURRENT_TIMESTAMP),
    (10, 4, '마감이 임박한 상황에서 이해관계자의 수정 요청이 브랜드 가이드와 충돌합니다. 시각적 일관성과 요구사항을 함께 만족시키기 위해 어떤 기준으로 조율하시겠습니까?', CURRENT_TIMESTAMP),
    (11, 4, '여러 부서가 동시에 긴급한 업무를 요청하지만 시간과 인력이 부족합니다. 우선순위를 어떤 기준으로 결정하고 관련자들과 어떻게 협업하시겠습니까?', CURRENT_TIMESTAMP)
ON CONFLICT DO NOTHING;
