-- ============================================================================
-- 数据主数据 V1.5.0 → V1.6.0 升级脚本
-- 为所有模块表添加 project_id / project_code 字段，实现项目级数据隔离
-- ============================================================================

-- ============================================================================
-- 1. CATALOG 元数据采集模块
-- ============================================================================
ALTER TABLE public.cat_task ADD COLUMN IF NOT EXISTS project_id bigint;
COMMENT ON COLUMN public.cat_task.project_id IS '项目ID';

ALTER TABLE public.cat_task ADD COLUMN IF NOT EXISTS project_code varchar;
COMMENT ON COLUMN public.cat_task.project_code IS '项目编码';

ALTER TABLE public.cat_task_instance ADD COLUMN IF NOT EXISTS project_id bigint;
COMMENT ON COLUMN public.cat_task_instance.project_id IS '项目ID';

ALTER TABLE public.cat_task_instance ADD COLUMN IF NOT EXISTS project_code varchar;
COMMENT ON COLUMN public.cat_task_instance.project_code IS '项目编码';

ALTER TABLE public.cat_task_scheduler ADD COLUMN IF NOT EXISTS project_id bigint;
COMMENT ON COLUMN public.cat_task_scheduler.project_id IS '项目ID';

ALTER TABLE public.cat_task_scheduler ADD COLUMN IF NOT EXISTS project_code varchar;
COMMENT ON COLUMN public.cat_task_scheduler.project_code IS '项目编码';

ALTER TABLE public.cat_task_scope ADD COLUMN IF NOT EXISTS project_id bigint;
COMMENT ON COLUMN public.cat_task_scope.project_id IS '项目ID';

ALTER TABLE public.cat_task_scope ADD COLUMN IF NOT EXISTS project_code varchar;
COMMENT ON COLUMN public.cat_task_scope.project_code IS '项目编码';

ALTER TABLE public.cat_db ADD COLUMN IF NOT EXISTS project_id bigint;
COMMENT ON COLUMN public.cat_db.project_id IS '项目ID';

ALTER TABLE public.cat_db ADD COLUMN IF NOT EXISTS project_code varchar;
COMMENT ON COLUMN public.cat_db.project_code IS '项目编码';

ALTER TABLE public.cat_table ADD COLUMN IF NOT EXISTS project_id bigint;
COMMENT ON COLUMN public.cat_table.project_id IS '项目ID';

ALTER TABLE public.cat_table ADD COLUMN IF NOT EXISTS project_code varchar;
COMMENT ON COLUMN public.cat_table.project_code IS '项目编码';

ALTER TABLE public.cat_column ADD COLUMN IF NOT EXISTS project_id bigint;
COMMENT ON COLUMN public.cat_column.project_id IS '项目ID';

ALTER TABLE public.cat_column ADD COLUMN IF NOT EXISTS project_code varchar;
COMMENT ON COLUMN public.cat_column.project_code IS '项目编码';

ALTER TABLE public.cat_domain ADD COLUMN IF NOT EXISTS project_id bigint;
COMMENT ON COLUMN public.cat_domain.project_id IS '项目ID';

ALTER TABLE public.cat_domain ADD COLUMN IF NOT EXISTS project_code varchar;
COMMENT ON COLUMN public.cat_domain.project_code IS '项目编码';

-- ============================================================================
-- 2. SERVICE API 服务模块
-- ============================================================================
ALTER TABLE public.svc_api ADD COLUMN IF NOT EXISTS project_id bigint;
COMMENT ON COLUMN public.svc_api.project_id IS '项目ID';

ALTER TABLE public.svc_api ADD COLUMN IF NOT EXISTS project_code varchar;
COMMENT ON COLUMN public.svc_api.project_code IS '项目编码';

ALTER TABLE public.svc_api_apply ADD COLUMN IF NOT EXISTS project_id bigint;
COMMENT ON COLUMN public.svc_api_apply.project_id IS '项目ID';

ALTER TABLE public.svc_api_apply ADD COLUMN IF NOT EXISTS project_code varchar;
COMMENT ON COLUMN public.svc_api_apply.project_code IS '项目编码';

ALTER TABLE public.svc_api_log ADD COLUMN IF NOT EXISTS project_id bigint;
COMMENT ON COLUMN public.svc_api_log.project_id IS '项目ID';

ALTER TABLE public.svc_api_log ADD COLUMN IF NOT EXISTS project_code varchar;
COMMENT ON COLUMN public.svc_api_log.project_code IS '项目编码';

-- ============================================================================
-- 3. STANDARDS 数据标准模块
-- ============================================================================
ALTER TABLE public.std_code_map ADD COLUMN IF NOT EXISTS project_id bigint;
COMMENT ON COLUMN public.std_code_map.project_id IS '项目ID';
ALTER TABLE public.std_code_map ADD COLUMN IF NOT EXISTS project_code varchar;
COMMENT ON COLUMN public.std_code_map.project_code IS '项目编码';

ALTER TABLE public.std_data_elem ADD COLUMN IF NOT EXISTS project_id bigint;
COMMENT ON COLUMN public.std_data_elem.project_id IS '项目ID';
ALTER TABLE public.std_data_elem ADD COLUMN IF NOT EXISTS project_code varchar;
COMMENT ON COLUMN public.std_data_elem.project_code IS '项目编码';

ALTER TABLE public.std_data_elem_asset_rel ADD COLUMN IF NOT EXISTS project_id bigint;
COMMENT ON COLUMN public.std_data_elem_asset_rel.project_id IS '项目ID';
ALTER TABLE public.std_data_elem_asset_rel ADD COLUMN IF NOT EXISTS project_code varchar;
COMMENT ON COLUMN public.std_data_elem_asset_rel.project_code IS '项目编码';

ALTER TABLE public.std_data_elem_code ADD COLUMN IF NOT EXISTS project_id bigint;
COMMENT ON COLUMN public.std_data_elem_code.project_id IS '项目ID';
ALTER TABLE public.std_data_elem_code ADD COLUMN IF NOT EXISTS project_code varchar;
COMMENT ON COLUMN public.std_data_elem_code.project_code IS '项目编码';

ALTER TABLE public.std_data_elem_rule_rel ADD COLUMN IF NOT EXISTS project_id bigint;
COMMENT ON COLUMN public.std_data_elem_rule_rel.project_id IS '项目ID';
ALTER TABLE public.std_data_elem_rule_rel ADD COLUMN IF NOT EXISTS project_code varchar;
COMMENT ON COLUMN public.std_data_elem_rule_rel.project_code IS '项目编码';

ALTER TABLE public.std_document ADD COLUMN IF NOT EXISTS project_id bigint;
COMMENT ON COLUMN public.std_document.project_id IS '项目ID';
ALTER TABLE public.std_document ADD COLUMN IF NOT EXISTS project_code varchar;
COMMENT ON COLUMN public.std_document.project_code IS '项目编码';

ALTER TABLE public.std_model ADD COLUMN IF NOT EXISTS project_id bigint;
COMMENT ON COLUMN public.std_model.project_id IS '项目ID';
ALTER TABLE public.std_model ADD COLUMN IF NOT EXISTS project_code varchar;
COMMENT ON COLUMN public.std_model.project_code IS '项目编码';

ALTER TABLE public.std_model_column ADD COLUMN IF NOT EXISTS project_id bigint;
COMMENT ON COLUMN public.std_model_column.project_id IS '项目ID';
ALTER TABLE public.std_model_column ADD COLUMN IF NOT EXISTS project_code varchar;
COMMENT ON COLUMN public.std_model_column.project_code IS '项目编码';

ALTER TABLE public.std_model_materialized ADD COLUMN IF NOT EXISTS project_id bigint;
COMMENT ON COLUMN public.std_model_materialized.project_id IS '项目ID';
ALTER TABLE public.std_model_materialized ADD COLUMN IF NOT EXISTS project_code varchar;
COMMENT ON COLUMN public.std_model_materialized.project_code IS '项目编码';

-- ============================================================================
-- 4. MODELING 数据建模模块
-- ============================================================================
ALTER TABLE public.mdl_business_category ADD COLUMN IF NOT EXISTS project_id bigint;
COMMENT ON COLUMN public.mdl_business_category.project_id IS '项目ID';
ALTER TABLE public.mdl_business_category ADD COLUMN IF NOT EXISTS project_code varchar;
COMMENT ON COLUMN public.mdl_business_category.project_code IS '项目编码';

ALTER TABLE public.mdl_data_domain ADD COLUMN IF NOT EXISTS project_id bigint;
COMMENT ON COLUMN public.mdl_data_domain.project_id IS '项目ID';
ALTER TABLE public.mdl_data_domain ADD COLUMN IF NOT EXISTS project_code varchar;
COMMENT ON COLUMN public.mdl_data_domain.project_code IS '项目编码';

ALTER TABLE public.mdl_data_layer ADD COLUMN IF NOT EXISTS project_id bigint;
COMMENT ON COLUMN public.mdl_data_layer.project_id IS '项目ID';
ALTER TABLE public.mdl_data_layer ADD COLUMN IF NOT EXISTS project_code varchar;
COMMENT ON COLUMN public.mdl_data_layer.project_code IS '项目编码';

ALTER TABLE public.mdl_data_layer_specification ADD COLUMN IF NOT EXISTS project_id bigint;
COMMENT ON COLUMN public.mdl_data_layer_specification.project_id IS '项目ID';
ALTER TABLE public.mdl_data_layer_specification ADD COLUMN IF NOT EXISTS project_code varchar;
COMMENT ON COLUMN public.mdl_data_layer_specification.project_code IS '项目编码';

ALTER TABLE public.mdl_theme_domain ADD COLUMN IF NOT EXISTS project_id bigint;
COMMENT ON COLUMN public.mdl_theme_domain.project_id IS '项目ID';
ALTER TABLE public.mdl_theme_domain ADD COLUMN IF NOT EXISTS project_code varchar;
COMMENT ON COLUMN public.mdl_theme_domain.project_code IS '项目编码';

-- ============================================================================
-- 5. TAXONOMY 标签管理模块（补充已有 project_id 的表的 project_code）
-- ============================================================================

-- 已有 project_id 的表补充 project_code
ALTER TABLE public.tax_task_cat ADD COLUMN IF NOT EXISTS project_code varchar;
COMMENT ON COLUMN public.tax_task_cat.project_code IS '项目编码';

ALTER TABLE public.tax_data_dev_cat ADD COLUMN IF NOT EXISTS project_code varchar;
COMMENT ON COLUMN public.tax_data_dev_cat.project_code IS '项目编码';

ALTER TABLE public.tax_job_cat ADD COLUMN IF NOT EXISTS project_code varchar;
COMMENT ON COLUMN public.tax_job_cat.project_code IS '项目编码';

-- 分类表补充 project_id
ALTER TABLE public.tax_api_cat ADD COLUMN IF NOT EXISTS project_id bigint;
COMMENT ON COLUMN public.tax_api_cat.project_id IS '项目ID';
ALTER TABLE public.tax_api_cat ADD COLUMN IF NOT EXISTS project_code varchar;
COMMENT ON COLUMN public.tax_api_cat.project_code IS '项目编码';

ALTER TABLE public.tax_asset_cat ADD COLUMN IF NOT EXISTS project_id bigint;
COMMENT ON COLUMN public.tax_asset_cat.project_id IS '项目ID';
ALTER TABLE public.tax_asset_cat ADD COLUMN IF NOT EXISTS project_code varchar;
COMMENT ON COLUMN public.tax_asset_cat.project_code IS '项目编码';

ALTER TABLE public.tax_clean_cat ADD COLUMN IF NOT EXISTS project_id bigint;
COMMENT ON COLUMN public.tax_clean_cat.project_id IS '项目ID';
ALTER TABLE public.tax_clean_cat ADD COLUMN IF NOT EXISTS project_code varchar;
COMMENT ON COLUMN public.tax_clean_cat.project_code IS '项目编码';

ALTER TABLE public.tax_data_elem_cat ADD COLUMN IF NOT EXISTS project_id bigint;
COMMENT ON COLUMN public.tax_data_elem_cat.project_id IS '项目ID';
ALTER TABLE public.tax_data_elem_cat ADD COLUMN IF NOT EXISTS project_code varchar;
COMMENT ON COLUMN public.tax_data_elem_cat.project_code IS '项目编码';

ALTER TABLE public.tax_discover_task_cat ADD COLUMN IF NOT EXISTS project_id bigint;
COMMENT ON COLUMN public.tax_discover_task_cat.project_id IS '项目ID';
ALTER TABLE public.tax_discover_task_cat ADD COLUMN IF NOT EXISTS project_code varchar;
COMMENT ON COLUMN public.tax_discover_task_cat.project_code IS '项目编码';

ALTER TABLE public.tax_doc_cat ADD COLUMN IF NOT EXISTS project_id bigint;
COMMENT ON COLUMN public.tax_doc_cat.project_id IS '项目ID';
ALTER TABLE public.tax_doc_cat ADD COLUMN IF NOT EXISTS project_code varchar;
COMMENT ON COLUMN public.tax_doc_cat.project_code IS '项目编码';

ALTER TABLE public.tax_document_cat ADD COLUMN IF NOT EXISTS project_id bigint;
COMMENT ON COLUMN public.tax_document_cat.project_id IS '项目ID';
ALTER TABLE public.tax_document_cat ADD COLUMN IF NOT EXISTS project_code varchar;
COMMENT ON COLUMN public.tax_document_cat.project_code IS '项目编码';

ALTER TABLE public.tax_model_cat ADD COLUMN IF NOT EXISTS project_id bigint;
COMMENT ON COLUMN public.tax_model_cat.project_id IS '项目ID';
ALTER TABLE public.tax_model_cat ADD COLUMN IF NOT EXISTS project_code varchar;
COMMENT ON COLUMN public.tax_model_cat.project_code IS '项目编码';

ALTER TABLE public.tax_quality_cat ADD COLUMN IF NOT EXISTS project_id bigint;
COMMENT ON COLUMN public.tax_quality_cat.project_id IS '项目ID';
ALTER TABLE public.tax_quality_cat ADD COLUMN IF NOT EXISTS project_code varchar;
COMMENT ON COLUMN public.tax_quality_cat.project_code IS '项目编码';

ALTER TABLE public.tax_tag_cat ADD COLUMN IF NOT EXISTS project_id bigint;
COMMENT ON COLUMN public.tax_tag_cat.project_id IS '项目ID';
ALTER TABLE public.tax_tag_cat ADD COLUMN IF NOT EXISTS project_code varchar;
COMMENT ON COLUMN public.tax_tag_cat.project_code IS '项目编码';

-- 标签、主题等核心表
ALTER TABLE public.tax_tag ADD COLUMN IF NOT EXISTS project_id bigint;
COMMENT ON COLUMN public.tax_tag.project_id IS '项目ID';
ALTER TABLE public.tax_tag ADD COLUMN IF NOT EXISTS project_code varchar;
COMMENT ON COLUMN public.tax_tag.project_code IS '项目编码';

ALTER TABLE public.tax_tag_asset_rel ADD COLUMN IF NOT EXISTS project_id bigint;
COMMENT ON COLUMN public.tax_tag_asset_rel.project_id IS '项目ID';
ALTER TABLE public.tax_tag_asset_rel ADD COLUMN IF NOT EXISTS project_code varchar;
COMMENT ON COLUMN public.tax_tag_asset_rel.project_code IS '项目编码';

ALTER TABLE public.tax_theme ADD COLUMN IF NOT EXISTS project_id bigint;
COMMENT ON COLUMN public.tax_theme.project_id IS '项目ID';
ALTER TABLE public.tax_theme ADD COLUMN IF NOT EXISTS project_code varchar;
COMMENT ON COLUMN public.tax_theme.project_code IS '项目编码';

ALTER TABLE public.tax_source_system ADD COLUMN IF NOT EXISTS project_id bigint;
COMMENT ON COLUMN public.tax_source_system.project_id IS '项目ID';
ALTER TABLE public.tax_source_system ADD COLUMN IF NOT EXISTS project_code varchar;
COMMENT ON COLUMN public.tax_source_system.project_code IS '项目编码';

ALTER TABLE public.tax_clean_rule ADD COLUMN IF NOT EXISTS project_id bigint;
COMMENT ON COLUMN public.tax_clean_rule.project_id IS '项目ID';
ALTER TABLE public.tax_clean_rule ADD COLUMN IF NOT EXISTS project_code varchar;
COMMENT ON COLUMN public.tax_clean_rule.project_code IS '项目编码';

ALTER TABLE public.tax_audit_rule ADD COLUMN IF NOT EXISTS project_id bigint;
COMMENT ON COLUMN public.tax_audit_rule.project_id IS '项目ID';
ALTER TABLE public.tax_audit_rule ADD COLUMN IF NOT EXISTS project_code varchar;
COMMENT ON COLUMN public.tax_audit_rule.project_code IS '项目编码';

ALTER TABLE public.tax_client ADD COLUMN IF NOT EXISTS project_id bigint;
COMMENT ON COLUMN public.tax_client.project_id IS '项目ID';
ALTER TABLE public.tax_client ADD COLUMN IF NOT EXISTS project_code varchar;
COMMENT ON COLUMN public.tax_client.project_code IS '项目编码';

ALTER TABLE public.tax_client_api_rel ADD COLUMN IF NOT EXISTS project_id bigint;
COMMENT ON COLUMN public.tax_client_api_rel.project_id IS '项目ID';
ALTER TABLE public.tax_client_api_rel ADD COLUMN IF NOT EXISTS project_code varchar;
COMMENT ON COLUMN public.tax_client_api_rel.project_code IS '项目编码';

-- ============================================================================
-- 6. CATALOG 日志表（补充）
-- ============================================================================
ALTER TABLE public.cat_table_log ADD COLUMN IF NOT EXISTS project_id bigint;
COMMENT ON COLUMN public.cat_table_log.project_id IS '项目ID';
ALTER TABLE public.cat_table_log ADD COLUMN IF NOT EXISTS project_code varchar;
COMMENT ON COLUMN public.cat_table_log.project_code IS '项目编码';

ALTER TABLE public.cat_column_log ADD COLUMN IF NOT EXISTS project_id bigint;
COMMENT ON COLUMN public.cat_column_log.project_id IS '项目ID';
ALTER TABLE public.cat_column_log ADD COLUMN IF NOT EXISTS project_code varchar;
COMMENT ON COLUMN public.cat_column_log.project_code IS '项目编码';

ALTER TABLE public.cat_table_column_rel_log ADD COLUMN IF NOT EXISTS project_id bigint;
COMMENT ON COLUMN public.cat_table_column_rel_log.project_id IS '项目ID';
ALTER TABLE public.cat_table_column_rel_log ADD COLUMN IF NOT EXISTS project_code varchar;
COMMENT ON COLUMN public.cat_table_column_rel_log.project_code IS '项目编码';

ALTER TABLE public.cat_task_instance_log ADD COLUMN IF NOT EXISTS project_id bigint;
COMMENT ON COLUMN public.cat_task_instance_log.project_id IS '项目ID';
ALTER TABLE public.cat_task_instance_log ADD COLUMN IF NOT EXISTS project_code varchar;
COMMENT ON COLUMN public.cat_task_instance_log.project_code IS '项目编码';

-- ============================================================================
-- 7. COLLECTOR 数据采集模块
-- ============================================================================
ALTER TABLE public.col_quality_task ADD COLUMN IF NOT EXISTS project_id bigint;
COMMENT ON COLUMN public.col_quality_task.project_id IS '项目ID';
ALTER TABLE public.col_quality_task ADD COLUMN IF NOT EXISTS project_code varchar;
COMMENT ON COLUMN public.col_quality_task.project_code IS '项目编码';

ALTER TABLE public.ast_discovery_task ADD COLUMN IF NOT EXISTS project_id bigint;
COMMENT ON COLUMN public.ast_discovery_task.project_id IS '项目ID';
ALTER TABLE public.ast_discovery_task ADD COLUMN IF NOT EXISTS project_code varchar;
COMMENT ON COLUMN public.ast_discovery_task.project_code IS '项目编码';
