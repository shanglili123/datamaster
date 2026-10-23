-- 为自主 Agent 流程增加稳定标志：同一操作者、空间、数据源复用一个流程。
ALTER TABLE IF EXISTS ai_agent_operation
    ADD COLUMN IF NOT EXISTS flow_key varchar(320);

CREATE UNIQUE INDEX IF NOT EXISTS uk_ai_agent_operation_flow_key
    ON ai_agent_operation (creator_id, space_id, space_code, flow_key)
    WHERE del_flag = false AND flow_key IS NOT NULL;

COMMENT ON COLUMN ai_agent_operation.flow_key IS
    '按空间、数据源和 operationKey/conversationId/goal 的 SHA-256 生成；兼容旧版 DATASOURCE:space:datasource 键';
