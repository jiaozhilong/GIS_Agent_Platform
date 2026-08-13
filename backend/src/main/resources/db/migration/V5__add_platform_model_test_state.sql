ALTER TABLE platform_model_configs
    ADD COLUMN last_test_status VARCHAR(20) NOT NULL DEFAULT 'UNCONFIGURED',
    ADD COLUMN last_test_message VARCHAR(1000) NOT NULL DEFAULT '',
    ADD COLUMN last_tested_at TIMESTAMPTZ;

ALTER TABLE platform_model_configs
    ADD CONSTRAINT ck_platform_model_test_status
        CHECK (last_test_status IN ('HEALTHY', 'UNAVAILABLE', 'UNCONFIGURED'));
