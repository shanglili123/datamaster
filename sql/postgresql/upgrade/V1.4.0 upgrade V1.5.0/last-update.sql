-- COL_ETL_TASK_EXT 新增 FLINKX_JOB_JSON 字段
ALTER TABLE public.col_etl_task_ext
    ADD COLUMN IF NOT EXISTS flinkx_job_json text;

COMMENT ON COLUMN public.col_etl_task_ext.flinkx_job_json IS 'FlinkX任务JSON（当执行引擎为FlinkX时使用）';
