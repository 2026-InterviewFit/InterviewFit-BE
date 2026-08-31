-- 로컬 개발 프로필에서만 실행되는 초기 데이터이다.
-- 운영에 필요한 직무와 면접 질문은 Flyway 마이그레이션으로 관리한다.

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
    (10, '디자인', '그래픽 디자이너'),
    (11, '기타', '기타')
ON CONFLICT (id) DO UPDATE
SET category = EXCLUDED.category,
    name = EXCLUDED.name@@

SELECT setval(
    pg_get_serial_sequence('job_roles', 'id'),
    COALESCE((SELECT MAX(id) FROM job_roles), 1),
    true
)@@
