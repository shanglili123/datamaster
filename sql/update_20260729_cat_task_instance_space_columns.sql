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

SELECT dm_rename_column_if_exists('cat_task_instance', 'project_id', 'space_id');
SELECT dm_rename_column_if_exists('cat_task_instance', 'project_code', 'space_code');
SELECT dm_rename_column_if_exists('cat_task_instance_log', 'project_id', 'space_id');
SELECT dm_rename_column_if_exists('cat_task_instance_log', 'project_code', 'space_code');

ALTER TABLE IF EXISTS cat_task_instance ADD COLUMN IF NOT EXISTS space_id bigint;
ALTER TABLE IF EXISTS cat_task_instance ADD COLUMN IF NOT EXISTS space_code varchar(64);
ALTER TABLE IF EXISTS cat_task_instance_log ADD COLUMN IF NOT EXISTS space_id bigint;
ALTER TABLE IF EXISTS cat_task_instance_log ADD COLUMN IF NOT EXISTS space_code varchar(64);

DROP FUNCTION IF EXISTS dm_rename_column_if_exists(text, text, text);
