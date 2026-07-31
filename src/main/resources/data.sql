-- =====================================================================
-- 0. 다대일 질문 초기화
-- =====================================================================

TRUNCATE TABLE group_questions RESTART IDENTITY;

-- =====================================================================
-- 1. 직무(job_roles) 초기 데이터 삽입
-- =====================================================================

INSERT INTO job_roles (
    id,
    category,
    name
)
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
    (10, '디자인', '그래픽 디자이너')
    ON CONFLICT (id) DO UPDATE
                            SET
                                category = EXCLUDED.category,
                            name = EXCLUDED.name;

-- ID를 직접 넣었으므로 다음 자동 생성 ID를 맞춘다.
SELECT setval(
               pg_get_serial_sequence(
                       'job_roles',
                       'id'
               ),
               COALESCE(
                       (
                           SELECT MAX(id)
                           FROM job_roles
                       ),
                       1
               ),
               true
       );

-- =====================================================================
-- 2. 다대일 면접 질문 초기 데이터 삽입
-- =====================================================================

-- [1라운드] 공통 역량 및 가치관 질문
INSERT INTO group_questions (
    job_role_id,
    round_number,
    question_text,
    created_at
)
VALUES
    (
        NULL,
        1,
        '본인이 생각하는 자신의 가장 큰 강점과, 그것이 해당 직무에 어떻게 기여할 수 있는지 말씀해 주세요.',
        CURRENT_TIMESTAMP
    ),
    (
        NULL,
        1,
        '팀 프로젝트나 조직 생활 중 가장 갈등이 심했던 경험과, 이를 해결하기 위해 본인이 취한 행동은 무엇입니까?',
        CURRENT_TIMESTAMP
    );

-- [2라운드] 돌발 압박 및 위기 대응 질문
INSERT INTO group_questions (
    job_role_id,
    round_number,
    question_text,
    created_at
)
VALUES
    (
        NULL,
        2,
        '만약 입사 후 본인이 원하지 않는 부서나 직무에 배치된다면 어떻게 하겠습니까?',
        CURRENT_TIMESTAMP
    ),
    (
        NULL,
        2,
        '프로젝트 마감이 내일 모레인데 핵심 기능에 치명적인 에러가 발생했습니다. 팀원들과 의견이 갈릴 때 본인은 어떻게 리드하시겠습니까?',
        CURRENT_TIMESTAMP
    );

-- [3라운드] 직무별 토의 질문
INSERT INTO group_questions (
    job_role_id,
    round_number,
    question_text,
    created_at
)
VALUES
    (
        1,
        3,
        '최근 트래픽이 급증하여 프론트엔드 렌더링 성능 저하가 발생했습니다. 클라이언트 측에서 우선적으로 고려해야 할 최적화 방안은 무엇입니까?',
        CURRENT_TIMESTAMP
    ),
    (
        2,
        3,
        '최근 트래픽이 급증하여 서버 장애가 발생했습니다. 원인을 분석하고 해결하기 위해 가장 먼저 어떤 조치를 취하시겠습니까?',
        CURRENT_TIMESTAMP
    ),
    (
        3,
        3,
        '실제 운영 환경(Production)에서 모델의 예측 지연 시간(Latency)이 급격히 증가했습니다. 이를 개선하기 위한 아키텍처적 접근은 무엇입니까?',
        CURRENT_TIMESTAMP
    );