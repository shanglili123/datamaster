-- PostgreSQL only. Review and execute manually; this script never renames physical tables.
BEGIN;

ALTER TABLE std_model ADD COLUMN IF NOT EXISTS table_type varchar(1);
ALTER TABLE std_model ADD COLUMN IF NOT EXISTS data_layer_id bigint;
ALTER TABLE std_model ADD COLUMN IF NOT EXISTS business_category_id bigint;
ALTER TABLE std_model ADD COLUMN IF NOT EXISTS business_category_code varchar(256);
ALTER TABLE std_model ADD COLUMN IF NOT EXISTS data_domain_id bigint;
ALTER TABLE std_model ADD COLUMN IF NOT EXISTS theme_domain_id bigint;
ALTER TABLE std_model ADD COLUMN IF NOT EXISTS theme_domain_code varchar(256);
ALTER TABLE std_model ADD COLUMN IF NOT EXISTS table_case varchar(1);
ALTER TABLE std_model ADD COLUMN IF NOT EXISTS physical_table_name varchar(256);
ALTER TABLE std_model ADD COLUMN IF NOT EXISTS naming_version varchar(32);
ALTER TABLE std_model_materialized ADD COLUMN IF NOT EXISTS table_name varchar(256);

COMMENT ON COLUMN std_model.physical_table_name IS 'Frozen naming target; never recompute on dictionary/category edits';
COMMENT ON COLUMN std_model.naming_version IS 'LEGACY or GOVERNED_V1; upgrade requires explicit confirmation';
COMMENT ON COLUMN std_model_materialized.table_name IS 'Actual physical table snapshot for this datasource/materialization';

-- Label historical rows only. Do NOT guess governance IDs or compose new names for existing tables.
UPDATE std_model SET naming_version = 'LEGACY'
WHERE naming_version IS NULL OR btrim(naming_version) = '';
ALTER TABLE std_model ALTER COLUMN naming_version SET DEFAULT 'LEGACY';

-- Only an explicit asset link plus identical connection and space is sufficient evidence.
-- No matching by model_name/Chinese labels, no parsing historical SQL, no bulk renaming.
UPDATE std_model_materialized m
SET table_name = a.table_name
FROM ast_asset a
WHERE m.asset_id = a.id
  AND m.datasource_id IS NOT NULL
  AND m.datasource_id = a.datasource_id
  AND to_jsonb(m)->>'space_id' IS NOT DISTINCT FROM to_jsonb(a)->>'space_id'
  AND to_jsonb(m)->>'space_code' IS NOT DISTINCT FROM to_jsonb(a)->>'space_code'
  AND m.del_flag::text IN ('0', 'false')
  AND a.del_flag::text IN ('0', 'false')
  AND a.table_name IS NOT NULL AND btrim(a.table_name) <> ''
  AND (m.table_name IS NULL OR btrim(m.table_name) = '');

-- A model snapshot is safe only if every known linked materialization agrees on one target.
-- Multi-connection models with differing targets deliberately remain LEGACY without a model snapshot.
WITH confirmed_targets AS (
    SELECT m.model_id, min(m.table_name) AS table_name
    FROM std_model_materialized m
    JOIN ast_asset a ON a.id = m.asset_id AND a.datasource_id = m.datasource_id
                   AND to_jsonb(a)->>'space_id' IS NOT DISTINCT FROM to_jsonb(m)->>'space_id'
                   AND to_jsonb(a)->>'space_code' IS NOT DISTINCT FROM to_jsonb(m)->>'space_code'
    WHERE m.del_flag::text IN ('0', 'false') AND a.del_flag::text IN ('0', 'false')
      AND m.table_name = a.table_name AND btrim(m.table_name) <> ''
    GROUP BY m.model_id
    HAVING count(DISTINCT m.table_name) = 1
)
UPDATE std_model model
SET physical_table_name = target.table_name
FROM confirmed_targets target
WHERE model.id = target.model_id
  AND model.naming_version = 'LEGACY'
  AND (model.physical_table_name IS NULL OR btrim(model.physical_table_name) = '')
  AND NOT EXISTS (
      SELECT 1 FROM std_model_materialized other
      LEFT JOIN ast_asset evidence ON evidence.id = other.asset_id AND evidence.datasource_id = other.datasource_id
      WHERE other.model_id = model.id AND other.del_flag::text IN ('0', 'false')
        AND (other.table_name IS NULL OR btrim(other.table_name) = '' OR other.table_name <> target.table_name
             OR evidence.id IS NULL OR evidence.del_flag::text NOT IN ('0', 'false')
             OR other.table_name IS DISTINCT FROM evidence.table_name
             OR to_jsonb(other)->>'space_id' IS DISTINCT FROM to_jsonb(evidence)->>'space_id'
             OR to_jsonb(other)->>'space_code' IS DISTINCT FROM to_jsonb(evidence)->>'space_code')
  )
  AND NOT EXISTS (
      SELECT 1 FROM std_model_materialized linked
      JOIN ast_asset a ON a.id = linked.asset_id AND a.datasource_id = linked.datasource_id
      WHERE linked.model_id = model.id AND linked.del_flag::text IN ('0', 'false')
        AND (to_jsonb(a)->>'space_id' IS DISTINCT FROM to_jsonb(model)->>'space_id'
             OR to_jsonb(a)->>'space_code' IS DISTINCT FROM to_jsonb(model)->>'space_code')
  );

COMMIT;
