-- DataMaster ingestion reliability migration
-- PostgreSQL 12+; safe to run repeatedly.

CREATE TABLE IF NOT EXISTS "ingestion_log" (
    "id" bigint NOT NULL,
    "topic" character varying(200) NOT NULL,
    "partition" integer,
    "msg_offset" bigint,
    "message_key" text,
    "target_table" character varying(200),
    "status" character varying(20) NOT NULL,
    "conflict_action" character varying(20),
    "key_value" text,
    "message_body" text,
    "message" text,
    "creator_id" bigint,
    "create_by" character varying(50),
    "create_time" timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    "updater_id" bigint,
    "update_by" character varying(50),
    "update_time" timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    "del_flag" smallint DEFAULT 0,
    CONSTRAINT ingestion_log_pkey PRIMARY KEY ("id")
);

CREATE INDEX IF NOT EXISTS idx_ing_log_status
    ON public.ingestion_log USING btree (status);

CREATE INDEX IF NOT EXISTS idx_ing_log_topic_partition_offset
    ON public.ingestion_log USING btree (topic, partition, msg_offset);

ALTER TABLE "ingestion_log"
    ADD COLUMN IF NOT EXISTS "message_key" text;

COMMENT ON TABLE "ingestion_log" IS '数据接收处理日志（Kafka消费、冲突检查、写入数仓和死信状态）';
COMMENT ON COLUMN "ingestion_log"."status" IS 'SUCCESS/CONFLICT_SKIP/CONFLICT_ERROR/FAILED/DEAD_LETTER/REPLAYING/REPLAYED';
COMMENT ON COLUMN "ingestion_log"."conflict_action" IS 'INSERT/UPSERT/DELETE/SKIP/ERROR';
COMMENT ON COLUMN "ingestion_log"."message_key" IS 'Kafka 原始消息 key，死信重放时恢复分区路由语义';

CREATE TABLE IF NOT EXISTS "ingestion_event" (
    "event_key" character varying(512) NOT NULL,
    "topic" character varying(200) NOT NULL,
    "partition" integer NOT NULL,
    "msg_offset" bigint NOT NULL,
    "status" character varying(20) NOT NULL,
    "owner" character varying(100),
    "lease_until" timestamp without time zone,
    "attempt_count" integer NOT NULL DEFAULT 0,
    "create_time" timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    "update_time" timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ingestion_event_pkey PRIMARY KEY ("event_key")
);

CREATE INDEX IF NOT EXISTS idx_ing_event_status_lease
    ON public.ingestion_event USING btree (status, lease_until);

COMMENT ON TABLE "ingestion_event" IS 'Kafka 接入事件跨实例幂等领取和处理状态';
COMMENT ON COLUMN "ingestion_event"."status" IS 'PROCESSING/FAILED/SUCCESS/DEAD_LETTER';

-- 人工编辑仲裁的数据域隔离：执行记录固化目标数据源，避免同名表跨数据源误匹配。
ALTER TABLE "ont_action_execution"
    ADD COLUMN IF NOT EXISTS "datasource_id" bigint;

COMMENT ON COLUMN "ont_action_execution"."datasource_id"
    IS '目标物理表所属数据源，供数据接入人工编辑仲裁做数据域隔离';

CREATE INDEX IF NOT EXISTS idx_ont_action_execution_datasource
    ON public.ont_action_execution USING btree (datasource_id);

-- 回填迁移前可由动作定义直接推导的数据源。
-- 普通动作取 concept_id；FUNCTION 优先取 output_concept_id，再取 source_concept_id。
-- COMPOSITE 的真实数据源由 execution_steps 决定，结构可能包含关系步骤，留空交由人工核查，避免错误回填。
UPDATE "ont_action_execution" e
SET "datasource_id" = candidate.datasource_id
FROM "ont_action" a
CROSS JOIN LATERAL (
    SELECT ct."datasource_id"
    FROM "ont_concept_table" ct
    WHERE ct."concept_id" = CASE
              WHEN UPPER(a."action_type") = 'FUNCTION'
                  THEN COALESCE(a."output_concept_id", a."source_concept_id")
              ELSE a."concept_id"
          END
      AND COALESCE(ct."del_flag", 0) = 0
    ORDER BY ct."id"
    LIMIT 1
) candidate
WHERE e."datasource_id" IS NULL
  AND e."action_id" = a."id"
  AND COALESCE(a."del_flag", 0) = 0
  AND UPPER(a."action_type") <> 'COMPOSITE';
