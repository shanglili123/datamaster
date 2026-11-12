-- PostgreSQL, manual upgrade only. Review schema/search_path and back up before execution.
-- Run before deploying the ontology-aware session service against an existing table.
-- Old sessions intentionally remain NULL; never infer an ontology from recent selections.
ALTER TABLE ai_ask_session ADD COLUMN IF NOT EXISTS ontology_id BIGINT;
COMMENT ON COLUMN ai_ask_session.ontology_id IS 'Explicit conversation ontology selection; NULL means no ontology';
