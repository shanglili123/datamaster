-- PostgreSQL lineage v2. New isolated tables only; intentionally leaves legacy Neo4j untouched.
-- Apply explicitly after review. Runtime opt-in: datamaster.lineage.jdbc-enabled=true.
-- Collector outbox DDL is maintained in the companion collector migration.
CREATE TABLE IF NOT EXISTS cat_lineage_object (
    id varchar(80) PRIMARY KEY,
    kind varchar(16) NOT NULL CHECK (kind IN ('TABLE','COLUMN','FILE','STREAM','EXTERNAL')),
    datasource_id bigint NOT NULL,
    catalog_name varchar(512),
    schema_name varchar(512),
    object_name varchar(512),
    external_key varchar(64),
    column_name varchar(512),
    parent_id varchar(80) REFERENCES cat_lineage_object(id),
    create_time timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CHECK ((kind = 'COLUMN' AND parent_id IS NOT NULL AND column_name IS NOT NULL)
        OR (kind <> 'COLUMN' AND parent_id IS NULL))
);
CREATE INDEX IF NOT EXISTS idx_lineage_object_identity
    ON cat_lineage_object(datasource_id,catalog_name,schema_name,object_name) WHERE kind='TABLE';
CREATE INDEX IF NOT EXISTS idx_lineage_object_parent ON cat_lineage_object(parent_id) WHERE parent_id IS NOT NULL;

CREATE TABLE IF NOT EXISTS cat_lineage_producer (
    id varchar(80) PRIMARY KEY,
    task_id bigint NOT NULL UNIQUE,
    space_id bigint NOT NULL,
    space_code varchar(120) NOT NULL,
    task_name varchar(200),
    task_type varchar(80),
    task_code varchar(120),
    sequence bigint NOT NULL DEFAULT -1,
    version varchar(120),
    status varchar(16) NOT NULL CHECK (status IN ('EMPTY','COMPLETE','PARTIAL','FAILED','INACTIVE','DELETE')),
    error_code varchar(500),
    object_ids_json text NOT NULL DEFAULT '[]',
    active boolean NOT NULL DEFAULT false,
    deleted boolean NOT NULL DEFAULT false,
    stale boolean NOT NULL DEFAULT false,
    lastgood_sequence bigint,
    update_time timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CHECK (NOT deleted OR NOT active)
);
CREATE INDEX IF NOT EXISTS idx_lineage_producer_space ON cat_lineage_producer(space_id,space_code,active) WHERE NOT deleted;

CREATE TABLE IF NOT EXISTS cat_lineage_version (
    producer_id varchar(80) NOT NULL REFERENCES cat_lineage_producer(id),
    sequence bigint NOT NULL,
    version varchar(120),
    event_type varchar(16) NOT NULL CHECK (event_type IN ('UPSERT','ACTIVE','INACTIVE','DELETE')),
    status varchar(16) NOT NULL CHECK (status IN ('COMPLETE','PARTIAL','FAILED','INACTIVE','DELETE')),
    error_code varchar(500),
    create_time timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY(producer_id,sequence)
);

CREATE TABLE IF NOT EXISTS cat_lineage_edge (
    id varchar(80) PRIMARY KEY,
    producer_id varchar(80) NOT NULL REFERENCES cat_lineage_producer(id),
    sequence bigint NOT NULL,
    source_id varchar(80) NOT NULL REFERENCES cat_lineage_object(id),
    target_id varchar(80) NOT NULL REFERENCES cat_lineage_object(id),
    source_column_id varchar(80) REFERENCES cat_lineage_object(id),
    target_column_id varchar(80) REFERENCES cat_lineage_object(id),
    unit_key varchar(160) NOT NULL,
    route_key varchar(160) NOT NULL,
    kind varchar(16) NOT NULL CHECK (kind IN ('TABLE','FIELD')),
    confidence varchar(16) NOT NULL CHECK (confidence='EXACT'),
    CHECK ((kind='TABLE' AND source_column_id IS NULL AND target_column_id IS NULL)
        OR (kind='FIELD' AND source_column_id IS NOT NULL AND target_column_id IS NOT NULL))
);
CREATE INDEX IF NOT EXISTS idx_lineage_edge_source ON cat_lineage_edge(source_id,id);
CREATE INDEX IF NOT EXISTS idx_lineage_edge_target ON cat_lineage_edge(target_id,id);
CREATE INDEX IF NOT EXISTS idx_lineage_edge_producer ON cat_lineage_edge(producer_id);
