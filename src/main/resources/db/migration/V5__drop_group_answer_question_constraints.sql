-- 과거 Hibernate schema update가 자동 생성한 이름의 FK까지 제거한다.
DO $$
DECLARE
    constraint_name TEXT;
BEGIN
    FOR constraint_name IN
        SELECT c.conname
        FROM pg_constraint c
        WHERE c.conrelid = 'group_interview_answers'::regclass
          AND c.confrelid = 'group_questions'::regclass
          AND c.contype = 'f'
    LOOP
        EXECUTE format(
                'ALTER TABLE group_interview_answers DROP CONSTRAINT %I',
                constraint_name
        );
    END LOOP;
END $$;
