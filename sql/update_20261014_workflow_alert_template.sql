-- Workflow 失败/超时对账站内告警模板。
-- 可重复执行：固定模板 ID，仅在模板不存在时插入。
INSERT INTO public.message_template
    (id, title, content, category, msg_level, valid_flag, del_flag,
     create_by, creator_id, create_time, update_by, updater_id, update_time, remark)
SELECT 9,
       'Workflow 运行告警',
       'Workflow「${workflowName}」（ID：${workflowId}）运行 #${runId} 已进入${status}。结束时间：${finishedTime}。原因：${errorMessage}。请打开 Workflow 运行记录核查。',
       0, 1, 1, 0, '系统', 1, CURRENT_TIMESTAMP, '系统', 1, CURRENT_TIMESTAMP,
       'Workflow 失败或需人工对账时发送给运行发起人'
WHERE NOT EXISTS (SELECT 1 FROM public.message_template WHERE id = 9);

-- 数据到达触发按 trigger_ref 查找已发布 Workflow，补齐复合索引避免逐条扫描。
CREATE INDEX IF NOT EXISTS idx_ont_action_workflow_trigger_ref
    ON public.ont_action_workflow (trigger_ref, status, enabled);
