-- ============================================================
-- Project -> Space 字段规范化:project_id/project_code -> space_id/space_code
-- 日期:2026-08-10
-- 数据库:PostgreSQL(datamaster)
--
-- 背景:
--   代码已完成 project -> space 规范化改造,相关 DO 中为 spaceId/spaceCode,
--   MyBatis-Plus 查询会生成 space_id / space_code 列;
--   但以下表的列仍是 project_id / project_code,导致执行报错:
--   "ERROR: column t.space_id does not exist"
--
-- 已确认需迁移的表(DO 已重构为 spaceId/spaceCode,共 13 张):
--   cat_task_scope / cat_task_scheduler / cat_column_log / cat_domain
--   cat_table_column_rel_log / cat_table_log
--   std_code_map / std_data_elem_asset_rel / std_data_elem_code
--   std_data_elem_rule_rel / std_desensitize_assetcolumn
--   std_model_column / std_model_materialized
--
-- 说明:
--   1. 执行前请先备份数据库。
--   2. RENAME COLUMN 保留原数据(旧 project_id/project_code 值即空间值,
--      直接更名为 space_id/space_code)。
--   3. 本脚本已于 2026-08-10 对 192.168.93.174 的 datamaster 库执行。
-- ============================================================

-- 1. 元数据采集范围/调度
ALTER TABLE cat_task_scope RENAME COLUMN project_id TO space_id;
ALTER TABLE cat_task_scope RENAME COLUMN project_code TO space_code;
COMMENT ON COLUMN cat_task_scope.space_id IS '空间ID';
COMMENT ON COLUMN cat_task_scope.space_code IS '空间编码';

ALTER TABLE cat_task_scheduler RENAME COLUMN project_id TO space_id;
ALTER TABLE cat_task_scheduler RENAME COLUMN project_code TO space_code;
COMMENT ON COLUMN cat_task_scheduler.space_id IS '空间ID';
COMMENT ON COLUMN cat_task_scheduler.space_code IS '空间编码';

-- 2. 元数据目录日志/域
ALTER TABLE cat_column_log RENAME COLUMN project_id TO space_id;
ALTER TABLE cat_column_log RENAME COLUMN project_code TO space_code;
COMMENT ON COLUMN cat_column_log.space_id IS '空间ID';
COMMENT ON COLUMN cat_column_log.space_code IS '空间编码';

ALTER TABLE cat_domain RENAME COLUMN project_id TO space_id;
ALTER TABLE cat_domain RENAME COLUMN project_code TO space_code;
COMMENT ON COLUMN cat_domain.space_id IS '空间ID';
COMMENT ON COLUMN cat_domain.space_code IS '空间编码';

ALTER TABLE cat_table_column_rel_log RENAME COLUMN project_id TO space_id;
ALTER TABLE cat_table_column_rel_log RENAME COLUMN project_code TO space_code;
COMMENT ON COLUMN cat_table_column_rel_log.space_id IS '空间ID';
COMMENT ON COLUMN cat_table_column_rel_log.space_code IS '空间编码';

ALTER TABLE cat_table_log RENAME COLUMN project_id TO space_id;
ALTER TABLE cat_table_log RENAME COLUMN project_code TO space_code;
COMMENT ON COLUMN cat_table_log.space_id IS '空间ID';
COMMENT ON COLUMN cat_table_log.space_code IS '空间编码';

-- 3. 标准/数据元相关
ALTER TABLE std_code_map RENAME COLUMN project_id TO space_id;
ALTER TABLE std_code_map RENAME COLUMN project_code TO space_code;
COMMENT ON COLUMN std_code_map.space_id IS '空间ID';
COMMENT ON COLUMN std_code_map.space_code IS '空间编码';

ALTER TABLE std_data_elem_asset_rel RENAME COLUMN project_id TO space_id;
ALTER TABLE std_data_elem_asset_rel RENAME COLUMN project_code TO space_code;
COMMENT ON COLUMN std_data_elem_asset_rel.space_id IS '空间ID';
COMMENT ON COLUMN std_data_elem_asset_rel.space_code IS '空间编码';

ALTER TABLE std_data_elem_code RENAME COLUMN project_id TO space_id;
ALTER TABLE std_data_elem_code RENAME COLUMN project_code TO space_code;
COMMENT ON COLUMN std_data_elem_code.space_id IS '空间ID';
COMMENT ON COLUMN std_data_elem_code.space_code IS '空间编码';

ALTER TABLE std_data_elem_rule_rel RENAME COLUMN project_id TO space_id;
ALTER TABLE std_data_elem_rule_rel RENAME COLUMN project_code TO space_code;
COMMENT ON COLUMN std_data_elem_rule_rel.space_id IS '空间ID';
COMMENT ON COLUMN std_data_elem_rule_rel.space_code IS '空间编码';

ALTER TABLE std_desensitize_assetcolumn RENAME COLUMN project_id TO space_id;
ALTER TABLE std_desensitize_assetcolumn RENAME COLUMN project_code TO space_code;
COMMENT ON COLUMN std_desensitize_assetcolumn.space_id IS '空间ID';
COMMENT ON COLUMN std_desensitize_assetcolumn.space_code IS '空间编码';

ALTER TABLE std_model_column RENAME COLUMN project_id TO space_id;
ALTER TABLE std_model_column RENAME COLUMN project_code TO space_code;
COMMENT ON COLUMN std_model_column.space_id IS '空间ID';
COMMENT ON COLUMN std_model_column.space_code IS '空间编码';

ALTER TABLE std_model_materialized RENAME COLUMN project_id TO space_id;
ALTER TABLE std_model_materialized RENAME COLUMN project_code TO space_code;
COMMENT ON COLUMN std_model_materialized.space_id IS '空间ID';
COMMENT ON COLUMN std_model_materialized.space_code IS '空间编码';
