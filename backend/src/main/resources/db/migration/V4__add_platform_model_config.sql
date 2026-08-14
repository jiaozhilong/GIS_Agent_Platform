CREATE TABLE platform_model_configs (
    provider VARCHAR(40) PRIMARY KEY,
    base_url VARCHAR(500) NOT NULL DEFAULT '',
    model_name VARCHAR(160) NOT NULL DEFAULT '',
    api_key_ciphertext TEXT NOT NULL DEFAULT '',
    enabled BOOLEAN NOT NULL DEFAULT FALSE,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_platform_model_provider CHECK (provider IN ('PLATFORM_LLM'))
);

INSERT INTO platform_model_configs(provider, base_url, model_name, enabled)
VALUES ('PLATFORM_LLM', '', '', FALSE)
ON CONFLICT (provider) DO NOTHING;
