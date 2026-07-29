CREATE OR REPLACE FUNCTION dm_rename_column_if_exists(p_table_name text, p_source_column text, p_target_column text)
RETURNS void AS $$
BEGIN
  IF EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = current_schema()
      AND table_name = p_table_name
      AND column_name = p_source_column
  ) THEN
    IF NOT EXISTS (
      SELECT 1 FROM information_schema.columns
      WHERE table_schema = current_schema()
        AND table_name = p_table_name
        AND column_name = p_target_column
    ) THEN
      EXECUTE format('ALTER TABLE %I RENAME COLUMN %I TO %I', p_table_name, p_source_column, p_target_column);
    ELSE
      EXECUTE format('ALTER TABLE %I DROP COLUMN %I', p_table_name, p_source_column);
    END IF;
  END IF;
END;
$$ LANGUAGE plpgsql;

SELECT dm_rename_column_if_exists('cat_db', 'project_id', 'space_id');
SELECT dm_rename_column_if_exists('cat_db', 'project_code', 'space_code');

ALTER TABLE IF EXISTS cat_db ADD COLUMN IF NOT EXISTS space_id bigint;
ALTER TABLE IF EXISTS cat_db ADD COLUMN IF NOT EXISTS space_code varchar(64);

UPDATE cat_db db
SET space_id = task.space_id
FROM cat_task task
WHERE db.task_id = task.id
  AND db.space_id IS NULL
  AND task.space_id IS NOT NULL;

UPDATE cat_db db
SET space_code = task.space_code
FROM cat_task task
WHERE db.task_id = task.id
  AND (db.space_code IS NULL OR db.space_code = '')
  AND task.space_code IS NOT NULL;

DROP FUNCTION IF EXISTS dm_rename_column_if_exists(text, text, text);
