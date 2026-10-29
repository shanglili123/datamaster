-- PostgreSQL：仅新增模型/DB-GPT调用元数据，不存提示词、回答正文、URL或凭据。
BEGIN;
CREATE TABLE IF NOT EXISTS public.ai_model_invocation (
    id varchar(64) PRIMARY KEY,
    kind varchar(16) NOT NULL CHECK (kind IN ('MODEL','DBGPT')),
    scene varchar(60) NOT NULL,
    model varchar(100),
    user_id bigint,
    space_id bigint,
    space_code varchar(128),
    operation_id varchar(64),
    streaming boolean NOT NULL,
    status varchar(16) NOT NULL CHECK (status IN ('SUCCESS','FAILURE','REJECTED')),
    duration_ms bigint NOT NULL CHECK (duration_ms >= 0),
    input_tokens bigint CHECK (input_tokens >= 0),
    output_tokens bigint CHECK (output_tokens >= 0),
    total_tokens bigint CHECK (total_tokens >= 0),
    usage_known boolean NOT NULL DEFAULT false,
    http_status integer,
    category varchar(32),
    created_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_ai_invocation_space_time ON public.ai_model_invocation(space_id, space_code, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_ai_invocation_operation ON public.ai_model_invocation(operation_id, created_at DESC);
COMMENT ON TABLE public.ai_model_invocation IS '平台逻辑调用统计；含内部重试，一次begin一条记录。DB-GPT不代表内部模型次数；usage未知为NULL，不以0替代';
COMMIT;
