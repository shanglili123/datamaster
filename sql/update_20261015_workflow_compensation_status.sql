-- Workflow 人工补偿计划状态字段（NONE/REQUESTED/PARTIAL/COMPENSATED/FAILED）。
-- 可重复执行；补偿动作仍复用 ONT_ACTION_EXECUTION，不新增补偿明细表。
ALTER TABLE IF EXISTS public.ont_action_workflow_run
    ADD COLUMN IF NOT EXISTS compensation_status varchar(32) DEFAULT 'NONE';

ALTER TABLE IF EXISTS public.ont_action_workflow_run
    ADD COLUMN IF NOT EXISTS compensation_requested_time timestamp without time zone;

ALTER TABLE IF EXISTS public.ont_action_workflow_run
    ADD COLUMN IF NOT EXISTS compensation_finished_time timestamp without time zone;

ALTER TABLE IF EXISTS public.ont_action_workflow_run
    ADD COLUMN IF NOT EXISTS compensation_error text;

DO $$
BEGIN
    IF to_regclass('public.ont_action_workflow_run') IS NOT NULL THEN
        UPDATE public.ont_action_workflow_run
        SET compensation_status = 'NONE'
        WHERE compensation_status IS NULL;
        EXECUTE 'CREATE INDEX IF NOT EXISTS idx_ont_workflow_run_compensation_status
                 ON public.ont_action_workflow_run (compensation_status, update_time)';
    END IF;
END $$;
