-- PostgreSQL. Manual migration; does not register any remote model or grant permissions.
BEGIN;
ALTER TABLE public.ai_model_profile ADD COLUMN config_revision bigint NOT NULL DEFAULT 1 CHECK(config_revision>0);
CREATE TABLE public.ai_dbgpt_model_sync (
    id bigserial PRIMARY KEY,
    profile_id bigint NOT NULL REFERENCES public.ai_model_profile(id) ON DELETE CASCADE,
    space_id bigint NOT NULL,
    config_revision bigint NOT NULL,
    target_id varchar(64) NOT NULL,
    remote_alias varchar(180) NOT NULL,
    status varchar(20) NOT NULL CHECK(status IN ('PENDING','SYNCING','READY','FAILED','DISABLED')),
    attempts integer NOT NULL DEFAULT 0,
    last_error varchar(160),
    lease_token varchar(36),
    lease_until timestamp,
    next_attempt_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_attempt_at timestamp,
    updated_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(profile_id,config_revision,target_id),
    UNIQUE(target_id,remote_alias)
);
CREATE INDEX idx_ai_dbgpt_sync_pending ON public.ai_dbgpt_model_sync(status,next_attempt_at);
COMMENT ON TABLE public.ai_dbgpt_model_sync IS 'Versioned remote model registration queue/status; no credentials, raw HTTP bodies or model prompts';
COMMIT;
