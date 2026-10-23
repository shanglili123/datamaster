-- 数据资产操作申请审批/执行状态机。
-- 可重复执行：仅新增列，不修改既有申请数据。
ALTER TABLE IF EXISTS public.ast_asset_operate_apply
    ADD COLUMN IF NOT EXISTS approval_status varchar(32) NOT NULL DEFAULT 'DRAFT';

ALTER TABLE IF EXISTS public.ast_asset_operate_apply
    ADD COLUMN IF NOT EXISTS approval_comment text;

ALTER TABLE IF EXISTS public.ast_asset_operate_apply
    ADD COLUMN IF NOT EXISTS approved_by varchar(128);

ALTER TABLE IF EXISTS public.ast_asset_operate_apply
    ADD COLUMN IF NOT EXISTS approved_id bigint;

ALTER TABLE IF EXISTS public.ast_asset_operate_apply
    ADD COLUMN IF NOT EXISTS approved_time timestamp without time zone;

ALTER TABLE IF EXISTS public.ast_asset_operate_apply
    ADD COLUMN IF NOT EXISTS execution_message text;

-- 兼容历史“是否已执行”语义，避免已执行记录被显示为可审批草稿。
UPDATE public.ast_asset_operate_apply
   SET approval_status = 'EXECUTED'
 WHERE approval_status = 'DRAFT'
   AND execute_flag = '1'
   AND execute_time IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_ast_asset_operate_apply_approval_status
    ON public.ast_asset_operate_apply (approval_status, create_time DESC);

COMMENT ON COLUMN public.ast_asset_operate_apply.approval_status IS
    'DRAFT/PENDING_APPROVAL/APPROVED/REJECTED/EXECUTING/EXECUTED/FAILED';

-- Workflow Runtime 运行步骤与底层动作执行记录的可追溯关联。
-- 旧库通常已经有这三个预留字段，增量迁移保持幂等兼容。
ALTER TABLE IF EXISTS public.ont_action_execution
    ADD COLUMN IF NOT EXISTS workflow_run_id bigint,
    ADD COLUMN IF NOT EXISTS workflow_step_id bigint,
    ADD COLUMN IF NOT EXISTS parent_execution_id bigint;

CREATE INDEX IF NOT EXISTS idx_ont_action_execution_workflow_run
    ON public.ont_action_execution (workflow_run_id, workflow_step_id);

-- 为新增页面和接口补齐菜单权限。固定 ID + NOT EXISTS 使迁移可重复执行。
INSERT INTO public.system_menu
    (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache,
     menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 4140, '资产操作申请', 2314, 5, 'assetOperateApply', 'ast/assetOperateApply/index',
       1, 0, 'C', '0', '0', 'ast:assetOperateApply:list', 'edit-box-line', 'system', CURRENT_TIMESTAMP,
       '数据资产操作申请审批与执行'
WHERE NOT EXISTS (SELECT 1 FROM public.system_menu WHERE menu_id = 4140);

INSERT INTO public.system_menu
    (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache,
     menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT v.menu_id, v.menu_name, 4140, v.order_num, '#', NULL, 1, 0, 'F', '0', '0', v.perms, '#', 'system', CURRENT_TIMESTAMP, NULL
FROM (VALUES
    (4141, '申请查询', 1, 'ast:assetOperateApply:query'),
    (4142, '申请新增', 2, 'ast:assetOperateApply:add'),
    (4143, '申请修改', 3, 'ast:assetOperateApply:edit'),
    (4144, '申请删除', 4, 'ast:assetOperateApply:remove'),
    (4145, '申请导出', 5, 'ast:assetOperateApply:export'),
    (4146, '申请导入', 6, 'ast:assetOperateApply:import'),
    (4147, '申请执行', 7, 'ast:assetOperateApply:execute')
) AS v(menu_id, menu_name, order_num, perms)
WHERE NOT EXISTS (SELECT 1 FROM public.system_menu m WHERE m.menu_id = v.menu_id);

-- 超级管理员和平台管理员默认获得新权限；其他角色仍由管理员按职责授权。
INSERT INTO public.system_role_menu (role_id, menu_id, space_id)
SELECT r.role_id, m.menu_id, 0
FROM public.system_role r
JOIN public.system_menu m ON m.menu_id BETWEEN 4140 AND 4147
WHERE r.role_id IN (1, 2)
  AND NOT EXISTS (
      SELECT 1 FROM public.system_role_menu rm
      WHERE rm.role_id = r.role_id AND rm.menu_id = m.menu_id AND COALESCE(rm.space_id, 0) = 0
  );

-- Workflow V2 运行实例和节点步骤。运行时支持 QUEUED/RUNNING ACTION DAG、结构化条件分支、
-- 并行拆分/汇聚、节点失败重试和安全超时对账；人工补偿动作复用动作执行记录。
CREATE TABLE IF NOT EXISTS public.ont_action_workflow_run (
    id bigint PRIMARY KEY,
    workflow_id bigint NOT NULL,
    idempotency_key varchar(256),
    status varchar(32) NOT NULL,
    input_params text,
    output_context text,
    current_node_key varchar(128),
    error_message text,
    started_time timestamp without time zone,
    finished_time timestamp without time zone,
    cancel_requested smallint NOT NULL DEFAULT 0,
    creator_id bigint,
    create_by varchar(128),
    create_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updater_id bigint,
    update_by varchar(128),
    update_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE IF EXISTS public.ont_action_workflow_run
    ADD COLUMN IF NOT EXISTS idempotency_key varchar(256);

CREATE UNIQUE INDEX IF NOT EXISTS uk_ont_workflow_run_idempotency
    ON public.ont_action_workflow_run (workflow_id, idempotency_key)
    WHERE idempotency_key IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_ont_workflow_run_workflow
    ON public.ont_action_workflow_run (workflow_id, id DESC);
CREATE INDEX IF NOT EXISTS idx_ont_workflow_run_status
    ON public.ont_action_workflow_run (status, id DESC);
CREATE INDEX IF NOT EXISTS idx_ont_workflow_run_lease
    ON public.ont_action_workflow_run (status, update_time);

CREATE TABLE IF NOT EXISTS public.ont_action_workflow_step (
    id bigint PRIMARY KEY,
    run_id bigint NOT NULL,
    node_key varchar(128) NOT NULL,
    node_type varchar(32) NOT NULL,
    action_id bigint,
    execution_id bigint,
    status varchar(32) NOT NULL,
    input_params text,
    output_json text,
    error_message text,
    started_time timestamp without time zone,
    finished_time timestamp without time zone,
    attempt_no integer NOT NULL DEFAULT 1,
    creator_id bigint,
    create_by varchar(128),
    create_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updater_id bigint,
    update_by varchar(128),
    update_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_ont_workflow_step_run
    ON public.ont_action_workflow_step (run_id, id);
