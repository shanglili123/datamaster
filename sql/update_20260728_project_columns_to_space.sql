-- 2026-07-28: rename Project physical tables/columns to Space.
-- PostgreSQL migration. This intentionally does not keep compatibility columns/views.

CREATE OR REPLACE FUNCTION dm_rename_column_if_exists(p_table_name text, p_source_column text, p_target_column text)
RETURNS void AS $$
BEGIN
  IF to_regclass(p_table_name) IS NOT NULL
     AND EXISTS (
       SELECT 1 FROM information_schema.columns
       WHERE table_schema = current_schema()
         AND table_name = p_table_name
         AND column_name = p_source_column
     )
     AND NOT EXISTS (
       SELECT 1 FROM information_schema.columns
       WHERE table_schema = current_schema()
         AND table_name = p_table_name
         AND column_name = p_target_column
     ) THEN
    EXECUTE format('ALTER TABLE %I RENAME COLUMN %I TO %I', p_table_name, p_source_column, p_target_column);
  END IF;
END;
$$ LANGUAGE plpgsql;

DO $$
BEGIN
  IF to_regclass('tax_project') IS NOT NULL AND to_regclass('tax_space') IS NULL THEN
    ALTER TABLE tax_project RENAME TO tax_space;
  END IF;

  IF to_regclass('tax_project_user_rel') IS NOT NULL AND to_regclass('tax_space_user_rel') IS NULL THEN
    ALTER TABLE tax_project_user_rel RENAME TO tax_space_user_rel;
  END IF;

  IF to_regclass('ast_datasource_project_rel') IS NOT NULL AND to_regclass('ast_datasource_space_rel') IS NULL THEN
    ALTER TABLE ast_datasource_project_rel RENAME TO ast_datasource_space_rel;
  END IF;
END $$;

SELECT dm_rename_column_if_exists('tax_space_user_rel', 'project_id', 'space_id');
SELECT dm_rename_column_if_exists('ast_datasource_space_rel', 'project_id', 'space_id');
SELECT dm_rename_column_if_exists('ast_datasource_space_rel', 'project_code', 'space_code');

ALTER TABLE IF EXISTS ast_asset_project_rel RENAME TO ast_asset_space_rel;
SELECT dm_rename_column_if_exists('ast_asset_space_rel', 'project_id', 'space_id');
SELECT dm_rename_column_if_exists('ast_asset_space_rel', 'project_code', 'space_code');

ALTER TABLE IF EXISTS ast_asset_column_project_rel RENAME TO ast_asset_column_space_rel;
SELECT dm_rename_column_if_exists('ast_asset_column_space_rel', 'project_id', 'space_id');
SELECT dm_rename_column_if_exists('ast_asset_column_space_rel', 'project_code', 'space_code');

SELECT dm_rename_column_if_exists('ast_asset_apply', 'project_id', 'space_id');
SELECT dm_rename_column_if_exists('ast_asset_apply', 'project_code', 'space_code');
SELECT dm_rename_column_if_exists('ast_discovery_task', 'project_id', 'space_id');
SELECT dm_rename_column_if_exists('ast_discovery_task', 'project_code', 'space_code');

SELECT dm_rename_column_if_exists('col_etl_task', 'project_id', 'space_id');
SELECT dm_rename_column_if_exists('col_etl_task', 'project_code', 'space_code');
SELECT dm_rename_column_if_exists('col_etl_task_log', 'project_id', 'space_id');
SELECT dm_rename_column_if_exists('col_etl_task_log', 'project_code', 'space_code');
SELECT dm_rename_column_if_exists('col_etl_node', 'project_id', 'space_id');
SELECT dm_rename_column_if_exists('col_etl_node', 'project_code', 'space_code');
SELECT dm_rename_column_if_exists('col_etl_node_log', 'project_id', 'space_id');
SELECT dm_rename_column_if_exists('col_etl_node_log', 'project_code', 'space_code');
SELECT dm_rename_column_if_exists('col_etl_task_instance', 'project_id', 'space_id');
SELECT dm_rename_column_if_exists('col_etl_task_instance', 'project_code', 'space_code');
SELECT dm_rename_column_if_exists('col_etl_node_instance', 'project_id', 'space_id');
SELECT dm_rename_column_if_exists('col_etl_node_instance', 'project_code', 'space_code');
SELECT dm_rename_column_if_exists('col_etl_task_node_rel', 'project_id', 'space_id');
SELECT dm_rename_column_if_exists('col_etl_task_node_rel', 'project_code', 'space_code');
SELECT dm_rename_column_if_exists('col_etl_task_node_rel_log', 'project_id', 'space_id');
SELECT dm_rename_column_if_exists('col_etl_task_node_rel_log', 'project_code', 'space_code');
SELECT dm_rename_column_if_exists('col_quality_task', 'project_id', 'space_id');
SELECT dm_rename_column_if_exists('col_quality_task', 'project_code', 'space_code');

SELECT dm_rename_column_if_exists('system_role', 'project_id', 'space_id');
SELECT dm_rename_column_if_exists('system_role_menu', 'project_id', 'space_id');

SELECT dm_rename_column_if_exists('svc_api', 'project_id', 'space_id');
SELECT dm_rename_column_if_exists('svc_api', 'project_code', 'space_code');
SELECT dm_rename_column_if_exists('svc_api_log', 'project_id', 'space_id');
SELECT dm_rename_column_if_exists('svc_api_log', 'project_code', 'space_code');

SELECT dm_rename_column_if_exists('cat_task', 'project_id', 'space_id');
SELECT dm_rename_column_if_exists('cat_task', 'project_code', 'space_code');
SELECT dm_rename_column_if_exists('cat_db', 'project_id', 'space_id');
SELECT dm_rename_column_if_exists('cat_db', 'project_code', 'space_code');
SELECT dm_rename_column_if_exists('cat_task_instance', 'project_id', 'space_id');
SELECT dm_rename_column_if_exists('cat_task_instance', 'project_code', 'space_code');
SELECT dm_rename_column_if_exists('cat_task_instance_log', 'project_id', 'space_id');
SELECT dm_rename_column_if_exists('cat_task_instance_log', 'project_code', 'space_code');
SELECT dm_rename_column_if_exists('cat_table', 'project_id', 'space_id');
SELECT dm_rename_column_if_exists('cat_table', 'project_code', 'space_code');

SELECT dm_rename_column_if_exists('cat_column', 'project_id', 'space_id');
SELECT dm_rename_column_if_exists('cat_column', 'project_code', 'space_code');

SELECT dm_rename_column_if_exists('std_data_elem', 'project_id', 'space_id');
SELECT dm_rename_column_if_exists('std_data_elem', 'project_code', 'space_code');
SELECT dm_rename_column_if_exists('std_model', 'project_id', 'space_id');
SELECT dm_rename_column_if_exists('std_model', 'project_code', 'space_code');
SELECT dm_rename_column_if_exists('std_document', 'project_id', 'space_id');
SELECT dm_rename_column_if_exists('std_document', 'project_code', 'space_code');

SELECT dm_rename_column_if_exists('mdl_business_category', 'project_id', 'space_id');
SELECT dm_rename_column_if_exists('mdl_business_category', 'project_code', 'space_code');
SELECT dm_rename_column_if_exists('mdl_data_domain', 'project_id', 'space_id');
SELECT dm_rename_column_if_exists('mdl_data_domain', 'project_code', 'space_code');
SELECT dm_rename_column_if_exists('mdl_data_layer', 'project_id', 'space_id');
SELECT dm_rename_column_if_exists('mdl_data_layer', 'project_code', 'space_code');
SELECT dm_rename_column_if_exists('mdl_data_layer_specification', 'project_id', 'space_id');
SELECT dm_rename_column_if_exists('mdl_data_layer_specification', 'project_code', 'space_code');
SELECT dm_rename_column_if_exists('mdl_theme_domain', 'project_id', 'space_id');
SELECT dm_rename_column_if_exists('mdl_theme_domain', 'project_code', 'space_code');

SELECT dm_rename_column_if_exists('tax_tag', 'project_id', 'space_id');
SELECT dm_rename_column_if_exists('tax_tag', 'project_code', 'space_code');
SELECT dm_rename_column_if_exists('tax_asset_cat', 'project_id', 'space_id');
SELECT dm_rename_column_if_exists('tax_asset_cat', 'project_code', 'space_code');
SELECT dm_rename_column_if_exists('tax_api_cat', 'project_id', 'space_id');
SELECT dm_rename_column_if_exists('tax_api_cat', 'project_code', 'space_code');
SELECT dm_rename_column_if_exists('tax_clean_cat', 'project_id', 'space_id');
SELECT dm_rename_column_if_exists('tax_clean_cat', 'project_code', 'space_code');
SELECT dm_rename_column_if_exists('tax_data_dev_cat', 'project_id', 'space_id');
SELECT dm_rename_column_if_exists('tax_data_dev_cat', 'project_code', 'space_code');
SELECT dm_rename_column_if_exists('tax_data_elem_cat', 'project_id', 'space_id');
SELECT dm_rename_column_if_exists('tax_data_elem_cat', 'project_code', 'space_code');
SELECT dm_rename_column_if_exists('tax_task_cat', 'project_id', 'space_id');
SELECT dm_rename_column_if_exists('tax_task_cat', 'project_code', 'space_code');
SELECT dm_rename_column_if_exists('tax_tag_cat', 'project_id', 'space_id');
SELECT dm_rename_column_if_exists('tax_tag_cat', 'project_code', 'space_code');
SELECT dm_rename_column_if_exists('tax_model_cat', 'project_id', 'space_id');
SELECT dm_rename_column_if_exists('tax_model_cat', 'project_code', 'space_code');
SELECT dm_rename_column_if_exists('tax_quality_cat', 'project_id', 'space_id');
SELECT dm_rename_column_if_exists('tax_quality_cat', 'project_code', 'space_code');
SELECT dm_rename_column_if_exists('tax_document_cat', 'project_id', 'space_id');
SELECT dm_rename_column_if_exists('tax_document_cat', 'project_code', 'space_code');
SELECT dm_rename_column_if_exists('tax_source_system', 'project_id', 'space_id');
SELECT dm_rename_column_if_exists('tax_source_system', 'project_code', 'space_code');
SELECT dm_rename_column_if_exists('tax_audit_rule', 'project_id', 'space_id');
SELECT dm_rename_column_if_exists('tax_audit_rule', 'project_code', 'space_code');
SELECT dm_rename_column_if_exists('tax_clean_rule', 'project_id', 'space_id');
SELECT dm_rename_column_if_exists('tax_clean_rule', 'project_code', 'space_code');
SELECT dm_rename_column_if_exists('tax_client', 'project_id', 'space_id');
SELECT dm_rename_column_if_exists('tax_client', 'project_code', 'space_code');
SELECT dm_rename_column_if_exists('tax_client_api_rel', 'project_id', 'space_id');
SELECT dm_rename_column_if_exists('tax_client_api_rel', 'project_code', 'space_code');
SELECT dm_rename_column_if_exists('tax_tag_asset_rel', 'project_id', 'space_id');
SELECT dm_rename_column_if_exists('tax_tag_asset_rel', 'project_code', 'space_code');
SELECT dm_rename_column_if_exists('tax_discover_task_cat', 'project_id', 'space_id');
SELECT dm_rename_column_if_exists('tax_discover_task_cat', 'project_code', 'space_code');
SELECT dm_rename_column_if_exists('tax_job_cat', 'project_id', 'space_id');
SELECT dm_rename_column_if_exists('tax_job_cat', 'project_code', 'space_code');
SELECT dm_rename_column_if_exists('tax_theme', 'project_id', 'space_id');
SELECT dm_rename_column_if_exists('tax_theme', 'project_code', 'space_code');

ALTER INDEX IF EXISTS idx_ai_ask_session_user_project_time RENAME TO idx_ai_ask_session_user_space_time;
SELECT dm_rename_column_if_exists('ai_ask_session', 'project_id', 'space_id');
SELECT dm_rename_column_if_exists('ai_ask_session', 'project_code', 'space_code');
SELECT dm_rename_column_if_exists('ai_ask_message', 'project_id', 'space_id');
SELECT dm_rename_column_if_exists('ai_ask_message', 'project_code', 'space_code');

-- Drop legacy remark columns after Project -> Space rename. Do not migrate old remark data.
DO $$
DECLARE
  description_table_names text[] := ARRAY[
    'tax_space',
    'tax_space_user_rel',
    'tax_api_cat',
    'tax_asset_cat',
    'tax_data_dev_cat',
    'tax_data_elem_cat',
    'tax_document_cat',
    'tax_model_cat',
    'tax_task_cat',
    'tax_theme',
    'tax_audit_rule',
    'tax_clean_rule',
    'tax_client',
    'tax_client_api_rel',
    'ast_asset',
    'ast_asset_column',
    'ast_datasource',
    'ast_datasource_space_rel',
    'ast_asset_space_rel',
    'ast_asset_column_space_rel',
    'ast_sensitive_level',
    'ast_asset_api_param',
    'ast_asset_audit_rule'
  ];
  drop_remark_table_names text[] := ARRAY[
    'ast_asset_apply',
    'ast_discovery_table',
    'ast_asset_api',
    'ast_asset_gis',
    'ast_asset_geo',
    'ast_asset_video',
    'ast_asset_audit_alert',
    'ast_asset_audit_schedule',
    'ast_asset_operate_apply',
    'ast_asset_operate_log',
    'ast_asset_theme_rel'
  ];
  rel_name text;
  has_remark boolean;
  has_description boolean;
BEGIN
  FOREACH rel_name IN ARRAY description_table_names LOOP
    SELECT EXISTS (
      SELECT 1 FROM information_schema.columns
      WHERE table_schema = current_schema()
        AND table_name = rel_name
        AND column_name = 'remark'
    ) INTO has_remark;

    SELECT EXISTS (
      SELECT 1 FROM information_schema.columns
      WHERE table_schema = current_schema()
        AND table_name = rel_name
        AND column_name = 'description'
    ) INTO has_description;

    IF has_remark THEN
      IF NOT has_description THEN
        EXECUTE format('ALTER TABLE %I ADD COLUMN description varchar(256)', rel_name);
      END IF;
      EXECUTE format('ALTER TABLE %I DROP COLUMN remark', rel_name);
    END IF;
  END LOOP;

  FOREACH rel_name IN ARRAY drop_remark_table_names LOOP
    SELECT EXISTS (
      SELECT 1 FROM information_schema.columns
      WHERE table_schema = current_schema()
        AND table_name = rel_name
        AND column_name = 'remark'
    ) INTO has_remark;

    IF has_remark THEN
      EXECUTE format('ALTER TABLE %I DROP COLUMN remark', rel_name);
    END IF;
  END LOOP;
END $$;

DROP FUNCTION IF EXISTS dm_rename_column_if_exists(text, text, text);
