-- Apply manually after reviewing/backup. No grants, data rewrite or runtime DDL.
BEGIN;
ALTER TABLE public.ai_decision_dispatch ADD COLUMN IF NOT EXISTS effect_observation_json text;
COMMENT ON COLUMN public.ai_decision_dispatch.effect_observation_json IS 'Bounded deterministic metric observation; not causal attribution or source freshness proof';
CREATE INDEX IF NOT EXISTS idx_ai_decision_latest_owner_space
    ON public.ai_decision_proposal(owner_id, space_id, space_code, decision_id, revision DESC);
COMMIT;
