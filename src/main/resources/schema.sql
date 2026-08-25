DO $$
DECLARE
    legacy_fk RECORD;
BEGIN
    IF to_regclass('public.job_roles') IS NULL THEN
        RAISE EXCEPTION 'job_roles table is required before consolidating job role references';
    END IF;

    IF to_regclass('public.job_role') IS NOT NULL THEN
        IF EXISTS (
            SELECT 1
            FROM job_role legacy
            LEFT JOIN job_roles canonical_role ON canonical_role.id = legacy.id
            WHERE canonical_role.id IS NULL
               OR canonical_role.category IS DISTINCT FROM legacy.category
               OR canonical_role.name IS DISTINCT FROM legacy.name
        ) THEN
            RAISE EXCEPTION 'job_role and job_roles contain missing or mismatched rows';
        END IF;

        IF EXISTS (
            SELECT 1
            FROM (
                SELECT job_role_id FROM group_questions WHERE job_role_id IS NOT NULL
                UNION ALL
                SELECT job_role_id FROM interviews WHERE job_role_id IS NOT NULL
                UNION ALL
                SELECT job_role_id FROM questions WHERE job_role_id IS NOT NULL
                UNION ALL
                SELECT job_role_id FROM recommended_jobs WHERE job_role_id IS NOT NULL
                UNION ALL
                SELECT job_role_id FROM group_interviews WHERE job_role_id IS NOT NULL
            ) referenced_role
            LEFT JOIN job_roles canonical_role ON canonical_role.id = referenced_role.job_role_id
            WHERE canonical_role.id IS NULL
        ) THEN
            RAISE EXCEPTION 'a job_role reference does not exist in job_roles';
        END IF;

        FOR legacy_fk IN
            SELECT constraint_table.relname AS table_name,
                   constraint_info.conname AS constraint_name
            FROM pg_constraint constraint_info
            JOIN pg_class constraint_table
              ON constraint_table.oid = constraint_info.conrelid
            WHERE constraint_info.contype = 'f'
              AND constraint_info.confrelid = 'public.job_role'::regclass
              AND constraint_table.relname IN (
                  'group_questions',
                  'interviews',
                  'questions',
                  'recommended_jobs'
              )
        LOOP
            EXECUTE format(
                'ALTER TABLE %I DROP CONSTRAINT %I',
                legacy_fk.table_name,
                legacy_fk.constraint_name
            );
        END LOOP;

        IF EXISTS (
            SELECT 1
            FROM pg_constraint
            WHERE contype = 'f'
              AND confrelid = 'public.job_role'::regclass
        ) THEN
            RAISE EXCEPTION 'job_role is still referenced by a foreign key';
        END IF;

        IF EXISTS (
            SELECT 1
            FROM (
                SELECT job_role_id FROM group_questions WHERE job_role_id IS NOT NULL
                UNION ALL
                SELECT job_role_id FROM interviews WHERE job_role_id IS NOT NULL
                UNION ALL
                SELECT job_role_id FROM questions WHERE job_role_id IS NOT NULL
                UNION ALL
                SELECT job_role_id FROM recommended_jobs WHERE job_role_id IS NOT NULL
                UNION ALL
                SELECT job_role_id FROM group_interviews WHERE job_role_id IS NOT NULL
            ) referenced_role
            LEFT JOIN job_roles canonical_role ON canonical_role.id = referenced_role.job_role_id
            WHERE canonical_role.id IS NULL
        ) THEN
            RAISE EXCEPTION 'consolidating job roles would create orphan references';
        END IF;

        DROP TABLE job_role;
    END IF;

    IF to_regclass('public.group_interviews') IS NOT NULL THEN
        IF EXISTS (
            SELECT 1
            FROM group_interviews interview
            LEFT JOIN job_roles canonical_role ON canonical_role.id = interview.job_role_id
            WHERE interview.job_role_id IS NOT NULL
              AND canonical_role.id IS NULL
        ) THEN
            RAISE EXCEPTION 'group_interviews contains a job_role_id absent from job_roles';
        END IF;

        IF NOT EXISTS (
            SELECT 1
            FROM pg_constraint
            WHERE contype = 'f'
              AND conrelid = 'public.group_interviews'::regclass
              AND confrelid = 'public.job_roles'::regclass
        ) THEN
            ALTER TABLE group_interviews
                ADD CONSTRAINT fk_group_interviews_job_roles
                FOREIGN KEY (job_role_id) REFERENCES job_roles(id);
        END IF;
    END IF;
END
$$@@
