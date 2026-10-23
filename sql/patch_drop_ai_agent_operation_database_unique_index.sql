-- AI_AGENT_OPERATION 允许同一数据库在同一空间产生多个自主 Agent 会话。
-- 流程复用由 datasource + space + flow_key 负责，database_name 不能作为全局唯一键。
DROP INDEX IF EXISTS public.uk_ai_agent_operation_database;
