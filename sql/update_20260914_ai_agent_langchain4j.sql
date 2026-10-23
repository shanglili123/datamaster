-- LangChain4j 平台 Agent 流程与审计/记忆表结构
-- 仅为迁移脚本；实际执行请通过项目数据库变更 Python 脚本完成。

CREATE TABLE IF NOT EXISTS ai_agent_operation (
    id varchar(64) PRIMARY KEY,
    goal text,
    datasource_id bigint,
    datasource_name varchar(256),
    database_name varchar(256),
    flow_key varchar(320),
    space_id bigint,
    space_code varchar(128),
    request_json text,
    status varchar(32),
    progress integer,
    current_step varchar(256),
    message text,
    output_json text,
    steps_json text,
    result_json text,
    required_fields_json text,
    asset_sync_message text,
    catalog_task_id bigint,
    confirmation_type varchar(64),
    confirmation_message text,
    ontology_id bigint,
    action_count integer,
    creator_id bigint,
    create_by varchar(128),
    create_time timestamp,
    updater_id bigint,
    update_by varchar(128),
    update_time timestamp,
    del_flag boolean DEFAULT false
);

ALTER TABLE IF EXISTS ai_agent_operation
    ADD COLUMN IF NOT EXISTS database_name varchar(256),
    ADD COLUMN IF NOT EXISTS flow_key varchar(320),
    ADD COLUMN IF NOT EXISTS datasource_id bigint,
    ADD COLUMN IF NOT EXISTS datasource_name varchar(256),
    ADD COLUMN IF NOT EXISTS space_id bigint,
    ADD COLUMN IF NOT EXISTS space_code varchar(128),
    ADD COLUMN IF NOT EXISTS request_json text,
    ADD COLUMN IF NOT EXISTS status varchar(32),
    ADD COLUMN IF NOT EXISTS progress integer,
    ADD COLUMN IF NOT EXISTS current_step varchar(256),
    ADD COLUMN IF NOT EXISTS message text,
    ADD COLUMN IF NOT EXISTS output_json text,
    ADD COLUMN IF NOT EXISTS steps_json text,
    ADD COLUMN IF NOT EXISTS result_json text,
    ADD COLUMN IF NOT EXISTS required_fields_json text,
    ADD COLUMN IF NOT EXISTS asset_sync_message text,
    ADD COLUMN IF NOT EXISTS ontology_id bigint,
    ADD COLUMN IF NOT EXISTS action_count integer,
    ADD COLUMN IF NOT EXISTS catalog_task_id bigint,
    ADD COLUMN IF NOT EXISTS confirmation_type varchar(64),
    ADD COLUMN IF NOT EXISTS confirmation_message text,
    ADD COLUMN IF NOT EXISTS creator_id bigint,
    ADD COLUMN IF NOT EXISTS create_by varchar(128),
    ADD COLUMN IF NOT EXISTS create_time timestamp,
    ADD COLUMN IF NOT EXISTS updater_id bigint,
    ADD COLUMN IF NOT EXISTS update_by varchar(128),
    ADD COLUMN IF NOT EXISTS update_time timestamp,
    ADD COLUMN IF NOT EXISTS del_flag boolean DEFAULT false;

CREATE INDEX IF NOT EXISTS idx_ai_agent_operation_database
    ON ai_agent_operation (database_name, update_time);

-- 新流程按“空间 + 数据源”复用同一主记录。历史记录的 flow_key 保持为空，
-- 不强行合并历史重复流程；新建/更新流程会由服务自动写入稳定标志。
CREATE UNIQUE INDEX IF NOT EXISTS uk_ai_agent_operation_flow_key
    ON ai_agent_operation (creator_id, space_id, space_code, flow_key)
    WHERE del_flag = false AND flow_key IS NOT NULL;

CREATE TABLE IF NOT EXISTS ai_agent_chat_memory (
    memory_id varchar(128) PRIMARY KEY,
    messages_json text NOT NULL,
    update_time timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS ai_agent_tool_call (
    id varchar(64) PRIMARY KEY,
    operation_id varchar(64),
    tool_name varchar(128) NOT NULL,
    user_id bigint,
    space_id bigint,
    space_code varchar(128),
    arguments_json text,
    status varchar(32),
    result_json text,
    duration_ms bigint,
    create_time timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_ai_agent_tool_operation
    ON ai_agent_tool_call (operation_id, create_time);

ALTER TABLE IF EXISTS ai_agent_tool_call
    ADD COLUMN IF NOT EXISTS space_id bigint,
    ADD COLUMN IF NOT EXISTS space_code varchar(128);
