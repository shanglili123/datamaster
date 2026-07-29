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

SELECT dm_rename_column_if_exists('col_quality_task', 'project_id', 'space_id');
SELECT dm_rename_column_if_exists('col_quality_task', 'project_code', 'space_code');

ALTER TABLE IF EXISTS col_quality_task ADD COLUMN IF NOT EXISTS space_id bigint;
ALTER TABLE IF EXISTS col_quality_task ADD COLUMN IF NOT EXISTS space_code varchar(64);

UPDATE col_quality_task t
SET space_id = c.space_id
FROM tax_quality_cat c
WHERE t.cat_code = c.code
  AND c.del_flag = '0'
  AND t.space_id IS NULL
  AND c.space_id IS NOT NULL;

UPDATE col_quality_task t
SET space_code = c.space_code
FROM tax_quality_cat c
WHERE t.cat_code = c.code
  AND c.del_flag = '0'
  AND (t.space_code IS NULL OR t.space_code = '')
  AND c.space_code IS NOT NULL;

DROP FUNCTION IF EXISTS dm_rename_column_if_exists(text, text, text);
