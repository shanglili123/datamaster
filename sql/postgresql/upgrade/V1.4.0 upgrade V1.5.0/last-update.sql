-- COL_ETL_TASK_EXT 新增 FLINKX_JOB_JSON 字段
ALTER TABLE public.col_etl_task_ext
    ADD COLUMN IF NOT EXISTS flinkx_job_json text;

COMMENT ON COLUMN public.col_etl_task_ext.flinkx_job_json IS 'FlinkX任务JSON（当执行引擎为FlinkX时使用）';

ALTER TABLE public.col_etl_task_ext
    ADD COLUMN IF NOT EXISTS flinkx_job_template_json text,
    ADD COLUMN IF NOT EXISTS incremental_type varchar(16),
    ADD COLUMN IF NOT EXISTS source_datasource_id bigint,
    ADD COLUMN IF NOT EXISTS target_datasource_id bigint,
    ADD COLUMN IF NOT EXISTS source_table_name varchar(512),
    ADD COLUMN IF NOT EXISTS target_table_name varchar(512),
    ADD COLUMN IF NOT EXISTS source_increment_column varchar(256),
    ADD COLUMN IF NOT EXISTS target_increment_column varchar(256),
    ADD COLUMN IF NOT EXISTS incremental_initial_value varchar(256),
    ADD COLUMN IF NOT EXISTS incremental_start_value varchar(256),
    ADD COLUMN IF NOT EXISTS incremental_end_value varchar(256),
    ADD COLUMN IF NOT EXISTS prepare_node_id bigint,
    ADD COLUMN IF NOT EXISTS prepare_node_name varchar(256),
    ADD COLUMN IF NOT EXISTS prepare_node_code varchar(256),
    ADD COLUMN IF NOT EXISTS prepare_node_version integer,
    ADD COLUMN IF NOT EXISTS prepare_relation_id bigint,
    ADD COLUMN IF NOT EXISTS complete_node_id bigint,
    ADD COLUMN IF NOT EXISTS complete_node_name varchar(256),
    ADD COLUMN IF NOT EXISTS complete_node_code varchar(256),
    ADD COLUMN IF NOT EXISTS complete_node_version integer,
    ADD COLUMN IF NOT EXISTS complete_relation_id bigint;

COMMENT ON COLUMN public.col_etl_task_ext.flinkx_job_template_json IS 'FlinkX基础任务JSON模板';
COMMENT ON COLUMN public.col_etl_task_ext.incremental_type IS '增量类型：ID、TIME';
COMMENT ON COLUMN public.col_etl_task_ext.incremental_initial_value IS '首次增量同步初始游标';
COMMENT ON COLUMN public.col_etl_task_ext.incremental_start_value IS '本次增量同步起始值';
COMMENT ON COLUMN public.col_etl_task_ext.incremental_end_value IS '本次增量同步结束值';

-- TAX_PROJECT 保存 DS 项目专属工作组
ALTER TABLE public.tax_project
    ADD COLUMN IF NOT EXISTS worker_group_id integer;

ALTER TABLE public.tax_project
    ADD COLUMN IF NOT EXISTS worker_group varchar;

COMMENT ON COLUMN public.tax_project.worker_group_id IS 'DS项目专属工作组ID';
COMMENT ON COLUMN public.tax_project.worker_group IS 'DS项目专属工作组名称';

-- 元数据采集页面已迁移到 CAT 模块目录
UPDATE public.system_menu
SET component = 'cat/task/structured/index'
WHERE menu_id = 2725
  AND component = 'mc/task/structured/index';

UPDATE public.system_menu
SET component = 'cat/instance/structured/index'
WHERE menu_id = 2726
  AND component = 'mc/instance/structured/index';

-- 避免与数据研发模块的 task、instance 路由重名
UPDATE public.system_menu
SET route_name = 'CatTask'
WHERE menu_id = 2725
  AND COALESCE(route_name, '') = '';

UPDATE public.system_menu
SET route_name = 'CatInstance'
WHERE menu_id = 2726
  AND COALESCE(route_name, '') = '';
