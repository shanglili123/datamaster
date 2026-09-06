-- =============================================
-- 拆除独立「决策(Decision)」实体，折叠进对象绑定动作执行(ActionExecution)
-- 涉及模块:
--   datamaster-ontology-core : 删除 ONT_DECISION / ONT_DECISION_EXECUTION 表
--                             删除 ONT_WEBHOOK_LOG.DECISION_EXECUTION_ID 列
--                             删除 system_menu 中「决策管理」菜单及其 ont:decision:* 权限
-- 背景: Palantir 模型下决策执行在「每一个对象」上，粒度是主键一条。
--       独立 Decision 壳(ONT_DECISION -> 绑定动作 -> ONT_DECISION_EXECUTION 委托副本)
--       与 ActionExecution 重叠，属多余抽象，折叠进 ActionExecution 并在该对象级记录上
--       叠加 提交判定 / 多级审批 / 回执 四部件。
-- 执行前请备份数据库。
-- 注意: 必须用应用连接账号执行 (dev 为 datamaster)，否则权限错误。
-- 本文件为 UTF-8 编码，执行前保证客户端按 UTF-8 读取（psql: \encoding UTF8）。
-- =============================================
SET client_encoding = 'UTF8';

-- 1. 删除决策执行记录表（委托动作执行副本，字段与 ONT_ACTION_EXECUTION 重叠）
DROP TABLE IF EXISTS ONT_DECISION_EXECUTION;

-- 2. 删除决策定义表（独立壳：绑定动作 + 数据到达触发）
DROP TABLE IF EXISTS ONT_DECISION;

-- 3. Webhook 日志去除决策执行 ID 列（副作用只收编动作执行）
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_schema = current_schema() AND table_name = 'ONT_WEBHOOK_LOG'
                 AND column_name = 'DECISION_EXECUTION_ID') THEN
        ALTER TABLE ONT_WEBHOOK_LOG DROP COLUMN DECISION_EXECUTION_ID;
    END IF;
END $$;

-- 4. 删除「决策管理」菜单及其子按钮权限（component=ont/decision/index, perms=ont:decision:*）
DELETE FROM system_menu
 WHERE perms IN ('ont:decision:query', 'ont:decision:add', 'ont:decision:edit', 'ont:decision:remove');

DELETE FROM system_menu
 WHERE perms = 'ont:decision:list'
    OR component = 'ont/decision/index';