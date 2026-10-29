-- Apply manually to the DataMaster metadata database. No runtime DDL is performed.
CREATE TABLE IF NOT EXISTS ai_decision_proposal (
    decision_id varchar(64) NOT NULL,
    revision integer NOT NULL,
    owner_id bigint NOT NULL,
    space_id bigint,
    space_code varchar(128),
    session_id bigint,
    conversation_id varchar(256) NOT NULL,
    ontology_id bigint NOT NULL,
    operation_id varchar(128),
    status varchar(40) NOT NULL,
    payload text NOT NULL,
    expires_at timestamp NOT NULL,
    lock_version integer NOT NULL DEFAULT 0,
    created_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (decision_id, revision)
);
CREATE INDEX IF NOT EXISTS idx_ai_decision_owner ON ai_decision_proposal(owner_id, space_id, created_at);
CREATE INDEX IF NOT EXISTS idx_ai_decision_session ON ai_decision_proposal(session_id, created_at);
CREATE INDEX IF NOT EXISTS idx_ai_decision_expiry ON ai_decision_proposal(expires_at);

CREATE TABLE IF NOT EXISTS ai_decision_dispatch (
    decision_id varchar(64) NOT NULL,
    revision integer NOT NULL,
    idempotency_key varchar(128) NOT NULL,
    binding_type varchar(24) NOT NULL,
    target_id bigint NOT NULL,
    status varchar(40) NOT NULL,
    execution_id bigint,
    run_id bigint,
    execution_status varchar(40),
    message varchar(1000),
    effect_status varchar(40) NOT NULL DEFAULT 'UNKNOWN',
    effect_message varchar(1000),
    lock_version integer NOT NULL DEFAULT 0,
    created_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (decision_id, revision),
    CONSTRAINT uk_ai_decision_dispatch_key UNIQUE (idempotency_key),
    CONSTRAINT uk_ai_decision_dispatch_once UNIQUE (decision_id),
    CONSTRAINT fk_ai_decision_revision FOREIGN KEY (decision_id, revision)
        REFERENCES ai_decision_proposal(decision_id, revision)
);
CREATE INDEX IF NOT EXISTS idx_ai_decision_dispatch_status ON ai_decision_dispatch(status, updated_at);
COMMENT ON TABLE ai_decision_proposal IS 'Decision owner binding and immutable prepared revision payload; analysis draft may finalize once';
COMMENT ON TABLE ai_decision_dispatch IS 'At-most-once dispatch claim; unknown submissions are reconciled, never blindly replayed';
