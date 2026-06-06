-- ============================================================================
-- 数据主数据 V1.5.0 → V1.6.0 种子数据项目关联脚本
-- 将所有现有种子数据关联到"基础数据组"（tax_project.id = 1）
-- 目录类表直接关联，业务表通过目录间接关联
-- ============================================================================

-- ============================================================================
-- Phase 1: 更新所有目录/分类表（"目录"类）
-- 这些表是其他业务表的外键参照目标，需要先设置 project_id
-- ============================================================================

-- 标准分类目录
UPDATE public.tax_document_cat SET project_id = 1, project_code = '152317790975712' WHERE project_id IS NULL;

-- 模型分类目录
UPDATE public.tax_model_cat SET project_id = 1, project_code = '152317790975712' WHERE project_id IS NULL;

-- 数据元分类目录
UPDATE public.tax_data_elem_cat SET project_id = 1, project_code = '152317790975712' WHERE project_id IS NULL;

-- API 分类目录
UPDATE public.tax_api_cat SET project_id = 1, project_code = '152317790975712' WHERE project_id IS NULL;

-- 资产分类目录
UPDATE public.tax_asset_cat SET project_id = 1, project_code = '152317790975712' WHERE project_id IS NULL;

-- 清洗分类目录
UPDATE public.tax_clean_cat SET project_id = 1, project_code = '152317790975712' WHERE project_id IS NULL;

-- 标签分类目录
UPDATE public.tax_tag_cat SET project_id = 1, project_code = '152317790975712' WHERE project_id IS NULL;

-- 质量分类目录
UPDATE public.tax_quality_cat SET project_id = 1, project_code = '152317790975712' WHERE project_id IS NULL;

-- 文档分类目录
UPDATE public.tax_doc_cat SET project_id = 1, project_code = '152317790975712' WHERE project_id IS NULL;

-- 发现任务分类目录
UPDATE public.tax_discover_task_cat SET project_id = 1, project_code = '152317790975712' WHERE project_id IS NULL;

-- 标签
UPDATE public.tax_tag SET project_id = 1, project_code = '152317790975712' WHERE project_id IS NULL;

-- 标签-资产关联
UPDATE public.tax_tag_asset_rel SET project_id = 1, project_code = '152317790975712' WHERE project_id IS NULL;

-- 主题
UPDATE public.tax_theme SET project_id = 1, project_code = '152317790975712' WHERE project_id IS NULL;

-- 来源系统
UPDATE public.tax_source_system SET project_id = 1, project_code = '152317790975712' WHERE project_id IS NULL;

-- 清洗规则
UPDATE public.tax_clean_rule SET project_id = 1, project_code = '152317790975712' WHERE project_id IS NULL;

-- 稽核规则
UPDATE public.tax_audit_rule SET project_id = 1, project_code = '152317790975712' WHERE project_id IS NULL;

-- 客户端
UPDATE public.tax_client SET project_id = 1, project_code = '152317790975712' WHERE project_id IS NULL;

-- 客户端-API 关联
UPDATE public.tax_client_api_rel SET project_id = 1, project_code = '152317790975712' WHERE project_id IS NULL;

-- ============================================================================
-- Phase 2: 通过目录关联更新业务表（"有目录的通过目录关联"）
-- 设置 project_id 为所引用目录的 project_id
-- ============================================================================

-- 标准登记表 → 通过 cat_code 关联到标准分类目录
UPDATE public.std_document t
SET project_id = c.project_id,
    project_code = c.project_code
FROM public.tax_document_cat c
WHERE t.cat_code = c.code
  AND t.project_id IS NULL;

-- 数据元表 → 通过 cat_code 关联到数据元分类目录
UPDATE public.std_data_elem t
SET project_id = c.project_id,
    project_code = c.project_code
FROM public.tax_data_elem_cat c
WHERE t.cat_code = c.code
  AND t.project_id IS NULL;

-- 逻辑模型表 → 通过 cat_code 关联到模型分类目录
UPDATE public.std_model t
SET project_id = c.project_id,
    project_code = c.project_code
FROM public.tax_model_cat c
WHERE t.cat_code = c.code
  AND t.project_id IS NULL;

-- API 服务表 → 通过 cat_code / cat_id 关联到 API 分类目录
UPDATE public.svc_api t
SET project_id = c.project_id,
    project_code = c.project_code
FROM public.tax_api_cat c
WHERE (t.cat_code = c.code OR t.cat_id = c.id)
  AND t.project_id IS NULL;

-- ============================================================================
-- Phase 3: 无目录关联的表，直接关联到基础数据组
-- ============================================================================

-- 标准模块
UPDATE public.std_code_map SET project_id = 1, project_code = '152317790975712' WHERE project_id IS NULL;
UPDATE public.std_data_elem_asset_rel SET project_id = 1, project_code = '152317790975712' WHERE project_id IS NULL;
UPDATE public.std_data_elem_code SET project_id = 1, project_code = '152317790975712' WHERE project_id IS NULL;
UPDATE public.std_data_elem_rule_rel SET project_id = 1, project_code = '152317790975712' WHERE project_id IS NULL;
UPDATE public.std_model_column SET project_id = 1, project_code = '152317790975712' WHERE project_id IS NULL;
UPDATE public.std_model_materialized SET project_id = 1, project_code = '152317790975712' WHERE project_id IS NULL;

-- 数据建模模块
UPDATE public.mdl_business_category SET project_id = 1, project_code = '152317790975712' WHERE project_id IS NULL;
UPDATE public.mdl_data_domain SET project_id = 1, project_code = '152317790975712' WHERE project_id IS NULL;
UPDATE public.mdl_data_layer SET project_id = 1, project_code = '152317790975712' WHERE project_id IS NULL;
UPDATE public.mdl_data_layer_specification SET project_id = 1, project_code = '152317790975712' WHERE project_id IS NULL;
UPDATE public.mdl_theme_domain SET project_id = 1, project_code = '152317790975712' WHERE project_id IS NULL;

-- API 服务模块
UPDATE public.svc_api_apply SET project_id = 1, project_code = '152317790975712' WHERE project_id IS NULL;
UPDATE public.svc_api_log SET project_id = 1, project_code = '152317790975712' WHERE project_id IS NULL;

-- 元数据采集模块
UPDATE public.cat_task SET project_id = 1, project_code = '152317790975712' WHERE project_id IS NULL;
UPDATE public.cat_task_instance SET project_id = 1, project_code = '152317790975712' WHERE project_id IS NULL;
UPDATE public.cat_task_scheduler SET project_id = 1, project_code = '152317790975712' WHERE project_id IS NULL;
UPDATE public.cat_task_scope SET project_id = 1, project_code = '152317790975712' WHERE project_id IS NULL;
UPDATE public.cat_db SET project_id = 1, project_code = '152317790975712' WHERE project_id IS NULL;
UPDATE public.cat_table SET project_id = 1, project_code = '152317790975712' WHERE project_id IS NULL;
UPDATE public.cat_column SET project_id = 1, project_code = '152317790975712' WHERE project_id IS NULL;
UPDATE public.cat_domain SET project_id = 1, project_code = '152317790975712' WHERE project_id IS NULL;
UPDATE public.cat_table_log SET project_id = 1, project_code = '152317790975712' WHERE project_id IS NULL;
UPDATE public.cat_column_log SET project_id = 1, project_code = '152317790975712' WHERE project_id IS NULL;
UPDATE public.cat_table_column_rel_log SET project_id = 1, project_code = '152317790975712' WHERE project_id IS NULL;
UPDATE public.cat_task_instance_log SET project_id = 1, project_code = '152317790975712' WHERE project_id IS NULL;

-- ============================================================================
-- Phase 4: 补充 Phase 2 中可能漏掉的记录（cat_code 为 NULL 或未匹配到目录的记录）
-- 兜底：将所有仍为 NULL 的记录设置为基础数据组
-- ============================================================================

UPDATE public.std_document SET project_id = 1, project_code = '152317790975712' WHERE project_id IS NULL;
UPDATE public.std_data_elem SET project_id = 1, project_code = '152317790975712' WHERE project_id IS NULL;
UPDATE public.std_model SET project_id = 1, project_code = '152317790975712' WHERE project_id IS NULL;
UPDATE public.svc_api SET project_id = 1, project_code = '152317790975712' WHERE project_id IS NULL;
