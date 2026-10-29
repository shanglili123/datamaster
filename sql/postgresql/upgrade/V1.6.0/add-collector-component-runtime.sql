-- New component workflow persistence on existing task/instance/node tables.
-- Review only; do not execute automatically. PostgreSQL only.
BEGIN;

ALTER TABLE col_etl_task ADD COLUMN IF NOT EXISTS active_run_id BIGINT;
ALTER TABLE col_etl_task ADD COLUMN IF NOT EXISTS run_workflow_instance_id BIGINT;
ALTER TABLE col_etl_task ADD COLUMN IF NOT EXISTS run_workflow_code BIGINT;
ALTER TABLE col_etl_task ADD COLUMN IF NOT EXISTS run_owner_token VARCHAR(128);
ALTER TABLE col_etl_task ADD COLUMN IF NOT EXISTS run_guard_status VARCHAR(16);
ALTER TABLE col_etl_task ADD COLUMN IF NOT EXISTS run_resources_json TEXT;
ALTER TABLE col_etl_task ADD COLUMN IF NOT EXISTS run_lease_updated_at TIMESTAMP;

ALTER TABLE col_etl_task_instance ADD COLUMN IF NOT EXISTS run_owner_token VARCHAR(128);
ALTER TABLE col_etl_task_instance ADD COLUMN IF NOT EXISTS run_guard_status VARCHAR(16);
ALTER TABLE col_etl_task_instance ADD COLUMN IF NOT EXISTS run_lease_updated_at TIMESTAMP;

ALTER TABLE col_etl_node_instance ADD COLUMN IF NOT EXISTS stable_stage_key VARCHAR(128);
ALTER TABLE col_etl_node_instance ADD COLUMN IF NOT EXISTS incremental_start_value TEXT;
ALTER TABLE col_etl_node_instance ADD COLUMN IF NOT EXISTS incremental_end_value TEXT;
ALTER TABLE col_etl_node_instance ADD COLUMN IF NOT EXISTS incremental_window_empty BOOLEAN;
ALTER TABLE col_etl_node_instance ADD COLUMN IF NOT EXISTS incremental_config_hash VARCHAR(128);
ALTER TABLE col_etl_node_instance ADD COLUMN IF NOT EXISTS incremental_window_status VARCHAR(16);
ALTER TABLE col_etl_node_instance ADD COLUMN IF NOT EXISTS incremental_successful_value TEXT;
ALTER TABLE col_etl_node_instance ADD COLUMN IF NOT EXISTS incremental_replay_from_id BIGINT;
ALTER TABLE col_etl_node_instance ADD COLUMN IF NOT EXISTS incremental_sources_json TEXT;

-- prepare creates a stage before DS assigns its task-instance ID. Do not invent an ID.
ALTER TABLE col_etl_node_instance ALTER COLUMN ds_id DROP NOT NULL;

CREATE INDEX IF NOT EXISTS idx_col_etl_task_active_run ON col_etl_task(active_run_id);
CREATE INDEX IF NOT EXISTS idx_col_etl_task_instance_guard ON col_etl_task_instance(task_id, run_guard_status, ds_id);
CREATE UNIQUE INDEX IF NOT EXISTS uq_col_etl_node_instance_stage
    ON col_etl_node_instance(task_instance_id, stable_stage_key)
    WHERE stable_stage_key IS NOT NULL AND del_flag='0';
CREATE INDEX IF NOT EXISTS idx_col_etl_task_instance_stage_lookup
    ON col_etl_task_instance(task_id, ds_id DESC);

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname='ck_col_etl_task_run_guard_status' AND conrelid='col_etl_task'::regclass) THEN
        ALTER TABLE col_etl_task ADD CONSTRAINT ck_col_etl_task_run_guard_status
            CHECK (run_guard_status IS NULL OR run_guard_status IN ('ACTIVE','UNCERTAIN','PAUSED')) NOT VALID;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname='ck_col_etl_task_instance_run_guard_status' AND conrelid='col_etl_task_instance'::regclass) THEN
        ALTER TABLE col_etl_task_instance ADD CONSTRAINT ck_col_etl_task_instance_run_guard_status
            CHECK (run_guard_status IS NULL OR run_guard_status IN ('ACTIVE','UNCERTAIN','PAUSED','SUCCESS','FAILURE','STOP')) NOT VALID;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname='ck_col_etl_node_instance_incremental_status' AND conrelid='col_etl_node_instance'::regclass) THEN
        ALTER TABLE col_etl_node_instance ADD CONSTRAINT ck_col_etl_node_instance_incremental_status
            CHECK (incremental_window_status IS NULL OR incremental_window_status IN ('PREPARED','SUCCESS','FAILURE')) NOT VALID;
    END IF;
END $$;

COMMENT ON COLUMN col_etl_task.run_resources_json IS '新组件当前运行规范化资源键；不含凭据，不做跨任务目标表独占';
COMMENT ON COLUMN col_etl_node_instance.incremental_start_value IS 'Multi-source cursor start JSON for this stage/run';
COMMENT ON COLUMN col_etl_node_instance.incremental_end_value IS 'Multi-source cursor end JSON for this stage/run';

COMMIT;
