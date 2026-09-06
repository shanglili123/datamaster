-- =============================================
-- 对象绑定动作执行：加 提交判定/多级审批 字段 + 审批三表 (PostgreSQL)
-- 涉及模块:
--   datamaster-ontology-core : ActionDO(Action) 加 submission_criteria / approval_levels
--                              ActionExecutionDO(ONT_ACTION_EXECUTION) 加 object_key / criteria_result / current_stage
--                              新 ONT_ACTION_REQUEST / ONT_ACTION_TASK / ONT_ACTION_REVIEWER 三表
-- 背景: Palantir 模型——决策执行在「每一个对象」上，Action 四部件(Submission Criteria / Rules /
--       Approvals+Checkpoint / 副作用)声明式挂在 Action 上，提交→判定→(多级审批)→调用→落库→副作用。
--       本迁移为对象绑定提供: ① 声明式条件配置(submission_criteria) + 审批级数(approval_levels)；
--                           ② 执行记录的对象/判定回执(object_key, criteria_result)；
--                           ③ 请求-任务-多审阅人状态机 + 强制理由 + 永久审计(三表)。
-- 执行前请备份数据库。必须用应用连接账号执行(dev 为 datamaster)。
-- 本文件为 UTF-8 编码，执行前保证客户端按 UTF-8 读取（psql: \encoding UTF8）。
-- =============================================
SET client_encoding = 'UTF8';

-- 1. Action: 声明式提交判定 + 审批级数
ALTER TABLE ONT_ACTION ADD COLUMN IF NOT EXISTS SUBMISSION_CRITERIA JSONB;
ALTER TABLE ONT_ACTION ADD COLUMN IF NOT EXISTS APPROVAL_LEVELS INT DEFAULT 1;
COMMENT ON COLUMN ONT_ACTION.SUBMISSION_CRITERIA IS '提交时判定条件表达式 JSONB（不带 = 免判定；带则提交时对对象状态+参数求值）';
COMMENT ON COLUMN ONT_ACTION.APPROVAL_LEVELS IS '多级审批关卡数，1=单级，>1=多级（每级一名或多位审阅人）';

-- 1.5 Action: 数据到达触发标识（非空=该动作启用数据到达自动触发，匹配 triggerRef 回调时自动提交）
ALTER TABLE ONT_ACTION ADD COLUMN IF NOT EXISTS TRIGGER_REF VARCHAR(200);
COMMENT ON COLUMN ONT_ACTION.TRIGGER_REF IS '数据到达触发标识（非空=数据到达自动触发；接收工程按 triggerRef 匹配回调自动提交本动作）';

-- 2. ActionExecution: 对象绑定 + 判定回执 + 审批阶段
ALTER TABLE ONT_ACTION_EXECUTION ADD COLUMN IF NOT EXISTS OBJECT_KEY VARCHAR(500);
ALTER TABLE ONT_ACTION_EXECUTION ADD COLUMN IF NOT EXISTS CRITERIA_RESULT JSONB;
ALTER TABLE ONT_ACTION_EXECUTION ADD COLUMN IF NOT EXISTS CURRENT_STAGE INT DEFAULT 0;
COMMENT ON COLUMN ONT_ACTION_EXECUTION.OBJECT_KEY IS '目标对象主键（对象实例级决策载体；空=批处理未膨胀成逐对象）';
COMMENT ON COLUMN ONT_ACTION_EXECUTION.CRITERIA_RESULT IS '①提交判定结果 JSONB {passed:boolean, decision:PASS/REJECT/NEED_APPROVAL, detail:[...]}';
COMMENT ON COLUMN ONT_ACTION_EXECUTION.CURRENT_STAGE IS '多级审批当前关卡（0=未提交审批，>=1 表示已推进到的关卡）';

-- 3. 审批请求
CREATE TABLE IF NOT EXISTS ONT_ACTION_REQUEST (
    ID                  BIGINT       PRIMARY KEY,
    ACTION_EXECUTION_ID BIGINT       NOT NULL,
    ONTOLOGY_ID         BIGINT,
    ACTION_ID           BIGINT,
    OBJECT_KEY          VARCHAR(500),
    TOTAL_STAGES        INT          DEFAULT 1,
    CURRENT_STAGE       INT          DEFAULT 0,
    STATUS              VARCHAR(20)  DEFAULT 'PENDING',
    REQUEST_BY          BIGINT,
    REQUEST_TIME        TIMESTAMP,
    REMARK              TEXT,
    CREATOR_ID          BIGINT,
    CREATE_BY           VARCHAR(50),
    CREATE_TIME         TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    UPDATER_ID          BIGINT,
    UPDATE_BY           VARCHAR(50),
    UPDATE_TIME         TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    DEL_FLAG            SMALLINT     DEFAULT 0
);
COMMENT ON TABLE ONT_ACTION_REQUEST IS '审批请求：一条执行记录(目标对象)对应一个审批流转';
COMMENT ON COLUMN ONT_ACTION_REQUEST.STATUS IS 'PENDING/APPROVED/REJECTED/CANCELLED';
COMMENT ON COLUMN ONT_ACTION_REQUEST.CURRENT_STAGE IS '已推进到的关卡(1..TOTAL_STAGES)';
CREATE INDEX IF NOT EXISTS IDX_ONT_ACTREQ_AEXEC ON ONT_ACTION_REQUEST(ACTION_EXECUTION_ID);
CREATE INDEX IF NOT EXISTS IDX_ONT_ACTREQ_STATUS ON ONT_ACTION_REQUEST(STATUS);

-- 4. 审批关卡任务
CREATE TABLE IF NOT EXISTS ONT_ACTION_TASK (
    ID                BIGINT       PRIMARY KEY,
    REQUEST_ID        BIGINT       NOT NULL,
    STAGE_NO          INT          NOT NULL,
    STATUS            VARCHAR(20)  DEFAULT 'PENDING',
    REQUIRED_APPROVALS INT         DEFAULT 1,
    APPROVED_COUNT    INT          DEFAULT 0,
    CREATOR_ID        BIGINT,
    CREATE_BY         VARCHAR(50),
    CREATE_TIME       TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    UPDATER_ID        BIGINT,
    UPDATE_BY         VARCHAR(50),
    UPDATE_TIME       TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    DEL_FLAG          SMALLINT     DEFAULT 0
);
COMMENT ON TABLE ONT_ACTION_TASK IS '审批关卡：请求下按关卡拆出的任务（多级审批）';
COMMENT ON COLUMN ONT_ACTION_TASK.STATUS IS 'PENDING/APPROVED/REJECTED';
COMMENT ON COLUMN ONT_ACTION_TASK.REQUIRED_APPROVALS IS '本关卡需几位审阅人同意方可进入下一关';
CREATE INDEX IF NOT EXISTS IDX_ONT_ACTTASK_REQ ON ONT_ACTION_TASK(REQUEST_ID);

-- 5. 审阅人决策（永久审计：决策后不再更新，记录谁+何时+强制理由）
CREATE TABLE IF NOT EXISTS ONT_ACTION_REVIEWER (
    ID            BIGINT       PRIMARY KEY,
    TASK_ID       BIGINT       NOT NULL,
    REQUEST_ID    BIGINT,
    REVIEWER_ID   BIGINT       NOT NULL,
    REVIEWER_NAME VARCHAR(50),
    DECISION      VARCHAR(20),
    REASON        TEXT         NOT NULL,
    DECIDE_TIME   TIMESTAMP,
    CREATOR_ID    BIGINT,
    CREATE_BY     VARCHAR(50),
    CREATE_TIME   TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);
COMMENT ON TABLE ONT_ACTION_REVIEWER IS '审阅人决策审计：强制理由 + 永久留痕(append-only)';
COMMENT ON COLUMN ONT_ACTION_REVIEWER.DECISION IS 'APPROVE/REJECT';
COMMENT ON COLUMN ONT_ACTION_REVIEWER.REASON IS '审批理由（必填）';
CREATE INDEX IF NOT EXISTS IDX_ONT_ACTREV_TASK ON ONT_ACTION_REVIEWER(TASK_ID);
CREATE INDEX IF NOT EXISTS IDX_ONT_ACTREV_REVIEWER ON ONT_ACTION_REVIEWER(REVIEWER_ID);
