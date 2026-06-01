-- COL_ETL_TASK_EXT 新增 FLINKX_JOB_JSON 字段
ALTER TABLE public.col_etl_task_ext
    ADD COLUMN IF NOT EXISTS flinkx_job_json text;

COMMENT ON COLUMN public.col_etl_task_ext.flinkx_job_json IS 'FlinkX任务JSON（当执行引擎为FlinkX时使用）';

-- TAX_PROJECT 保存 DS 项目专属工作组
ALTER TABLE public.tax_project
    ADD COLUMN IF NOT EXISTS worker_group_id integer;

ALTER TABLE public.tax_project
    ADD COLUMN IF NOT EXISTS worker_group varchar;

COMMENT ON COLUMN public.tax_project.worker_group_id IS 'DS项目专属工作组ID';
COMMENT ON COLUMN public.tax_project.worker_group IS 'DS项目专属工作组名称';
