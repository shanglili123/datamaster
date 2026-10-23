-- PostgreSQL only. Apply together with update_20261020_data_lineage.sql before LINEAGE_JDBC_ENABLED=true.
-- Minimal collector-local delivery log; no Neo4j reads, task execution or external DDL.
BEGIN;
-- Database ordering is stable across application nodes; gaps after rollbacks are harmless.
CREATE SEQUENCE IF NOT EXISTS col_lineage_event_seq AS bigint;
CREATE TABLE IF NOT EXISTS col_lineage_outbox (
    id bigint PRIMARY KEY,
    task_id bigint NOT NULL,
    sequence bigint NOT NULL UNIQUE,
    event_type varchar(20) NOT NULL,
    version varchar(128),
    payload text NOT NULL,
    status varchar(20) NOT NULL DEFAULT 'PENDING',
    attempts integer NOT NULL DEFAULT 0,
    next_attempt_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    lease_until timestamp,
    lease_token varchar(64),
    error_code varchar(80),
    create_time timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_col_lineage_outbox_due ON col_lineage_outbox(status, next_attempt_at);
CREATE INDEX IF NOT EXISTS idx_col_lineage_outbox_task ON col_lineage_outbox(task_id, sequence DESC);
COMMENT ON COLUMN col_lineage_outbox.payload IS 'Minimal published lineage snapshot; may contain SQL expressions but never connection credentials; not a public query API';
COMMIT;
