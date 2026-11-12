-- PostgreSQL. Review and apply manually before deploying space-flow code. No automatic grants.
BEGIN;
CREATE TABLE gov_space_flow (
 space_id bigint PRIMARY KEY REFERENCES tax_space(id), revision integer NOT NULL,
 draft_json text NOT NULL, published_version integer NOT NULL DEFAULT 0, published_json text,
 updated_by bigint NOT NULL, updated_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE gov_space_flow_version (
 space_id bigint NOT NULL REFERENCES tax_space(id), version integer NOT NULL,
 definition_json text NOT NULL, published_by bigint NOT NULL, published_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
 PRIMARY KEY(space_id,version)
);
CREATE TABLE gov_space_quality_admission (
 space_id bigint NOT NULL REFERENCES tax_space(id), datasource_id bigint NOT NULL, table_name varchar(200) NOT NULL,
 flow_version integer NOT NULL, available boolean NOT NULL, basis varchar(40) NOT NULL,
 quality_log_id bigint, reason varchar(1000), confirmed_by bigint NOT NULL, confirmed_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
 PRIMARY KEY(space_id,datasource_id,table_name)
);
CREATE TABLE gov_space_quality_admission_event (
 id bigserial PRIMARY KEY, space_id bigint NOT NULL, datasource_id bigint NOT NULL, table_name varchar(200) NOT NULL,
 flow_version integer NOT NULL, basis varchar(40) NOT NULL, quality_log_id bigint, reason varchar(1000),
 confirmed_by bigint NOT NULL, confirmed_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_gov_space_quality_event_scope_time
 ON gov_space_quality_admission_event(space_id,datasource_id,table_name,confirmed_at DESC,id DESC);
COMMENT ON TABLE gov_space_flow IS 'Space capability workflow: absent publication preserves legacy; configured prerequisites cannot fall back';
COMMENT ON TABLE gov_space_quality_admission_event IS 'Explicit quality availability evidence; never changes original check results or grants access';
COMMIT;
