WITH ranked_participants AS (
    SELECT id,
           ROW_NUMBER() OVER (
               PARTITION BY room_id, user_id
               ORDER BY id
           ) AS row_number
    FROM group_participants
)
DELETE FROM group_participants participant
USING ranked_participants ranked
WHERE participant.id = ranked.id
  AND ranked.row_number > 1;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'uk_group_participants_room_user'
          AND conrelid = 'group_participants'::regclass
    ) THEN
        ALTER TABLE group_participants
            ADD CONSTRAINT uk_group_participants_room_user
            UNIQUE (room_id, user_id);
    END IF;
END
$$;
