-- PostgreSQL migration. Review and deploy explicitly; the application does not create tables.
-- MODEL / OpenAI-compatible endpoints only. DB-GPT models and credentials remain independently configured.
-- Required server secret: ai.model-profiles.encryption-key (AI_MODEL_PROFILES_ENCRYPTION_KEY),
-- Base64-encoded AES key of 16/24/32 bytes, supplied out-of-band; no default key or generated secret.
-- Back up the encryption key securely; rotating it requires re-entering/re-encrypting existing credentials.
-- HTTP and private/local endpoints are intentionally allowed for trusted platform operators.
-- Apply outbound network policy as appropriate: activation/save does not probe the endpoint.
-- Provision ai:model-config:list/add/edit/remove permissions via the platform's menu/role management.
-- Permissions alone do not grant management: the controller enforces platform admin/system ops identity.
CREATE TABLE IF NOT EXISTS ai_model_profile (
    id BIGSERIAL PRIMARY KEY,
    space_id BIGINT NOT NULL REFERENCES tax_space(id),
    space_code VARCHAR(128) NOT NULL,
    name VARCHAR(100) NOT NULL,
    provider VARCHAR(64) NOT NULL,
    model VARCHAR(100) NOT NULL,
    api_url VARCHAR(500) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    active BOOLEAN NOT NULL DEFAULT FALSE,
    api_key_ciphertext TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_ai_model_profile_space_name UNIQUE (space_id, name),
    CONSTRAINT ck_ai_model_profile_active_enabled CHECK (NOT active OR enabled),
    CONSTRAINT ck_ai_model_profile_provider CHECK (provider ~ '^[A-Z][A-Z0-9_]{0,63}$'),
    CONSTRAINT ck_ai_model_profile_ciphertext CHECK (api_key_ciphertext LIKE 'gcm1:%')
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_ai_model_profile_space_active
    ON ai_model_profile(space_id) WHERE active = TRUE;
CREATE INDEX IF NOT EXISTS idx_ai_model_profile_space_enabled
    ON ai_model_profile(space_id, enabled);
COMMENT ON TABLE ai_model_profile IS 'Per-space OpenAI-compatible MODEL profiles; encrypted credentials never exposed through management/member responses';
COMMENT ON COLUMN ai_model_profile.provider IS 'Provider label such as BAILIAN, OPENAI_COMPATIBLE, DEEPSEEK, OTHER; not a protocol adapter';
COMMENT ON COLUMN ai_model_profile.active IS 'At most one active/default profile per space; new profiles start inactive';
COMMENT ON COLUMN ai_model_profile.api_key_ciphertext IS 'Versioned AES-GCM ciphertext with per-space authenticated data; never store plaintext';
