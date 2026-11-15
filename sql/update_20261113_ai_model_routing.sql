-- PostgreSQL. Explicit deployment only; no model calls, credentials or automatic role grants.
BEGIN;
CREATE TABLE IF NOT EXISTS public.ai_model_routing (
    space_id bigint PRIMARY KEY REFERENCES public.tax_space(id),
    space_code varchar(128) NOT NULL,
    enabled boolean NOT NULL DEFAULT false,
    economy_profile_id bigint REFERENCES public.ai_model_profile(id),
    advanced_profile_id bigint REFERENCES public.ai_model_profile(id),
    semantic_decision_enabled boolean NOT NULL DEFAULT false,
    decision_profile_id bigint REFERENCES public.ai_model_profile(id),
    strict_json_schema boolean NOT NULL DEFAULT false,
    complex_input_characters integer NOT NULL DEFAULT 12000 CHECK (complex_input_characters BETWEEN 1 AND 16000),
    revision integer NOT NULL DEFAULT 1 CHECK (revision > 0),
    updated_by bigint NOT NULL,
    updated_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CHECK (NOT enabled OR (economy_profile_id IS NOT NULL AND advanced_profile_id IS NOT NULL))
);
COMMENT ON TABLE public.ai_model_routing IS 'Per-space model routing override; absent row uses deployment defaults, disabled row explicitly disables routing';
COMMIT;
