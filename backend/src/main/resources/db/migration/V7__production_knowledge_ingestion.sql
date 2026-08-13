ALTER TABLE knowledge_assets DROP CONSTRAINT IF EXISTS ck_knowledge_asset_status;

ALTER TABLE knowledge_assets
    ADD COLUMN IF NOT EXISTS mime_type VARCHAR(160),
    ADD COLUMN IF NOT EXISTS sha256 VARCHAR(64),
    ADD COLUMN IF NOT EXISTS knowledge_type VARCHAR(30) NOT NULL DEFAULT 'SOLUTION',
    ADD COLUMN IF NOT EXISTS document_type VARCHAR(40) NOT NULL DEFAULT 'OTHER',
    ADD COLUMN IF NOT EXISTS gis_domain VARCHAR(160) NOT NULL DEFAULT '',
    ADD COLUMN IF NOT EXISTS product VARCHAR(160) NOT NULL DEFAULT '',
    ADD COLUMN IF NOT EXISTS product_version VARCHAR(80) NOT NULL DEFAULT '',
    ADD COLUMN IF NOT EXISTS project_type VARCHAR(160) NOT NULL DEFAULT '',
    ADD COLUMN IF NOT EXISTS region VARCHAR(160) NOT NULL DEFAULT '',
    ADD COLUMN IF NOT EXISTS document_year INTEGER,
    ADD COLUMN IF NOT EXISTS parser_strategy VARCHAR(30),
    ADD COLUMN IF NOT EXISTS parser_reason VARCHAR(500) NOT NULL DEFAULT '',
    ADD COLUMN IF NOT EXISTS parser_config JSONB NOT NULL DEFAULT '{}'::jsonb,
    ADD COLUMN IF NOT EXISTS parser_override BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS parse_status VARCHAR(40) NOT NULL DEFAULT 'UPLOADED',
    ADD COLUMN IF NOT EXISTS ragflow_status VARCHAR(40) NOT NULL DEFAULT 'PENDING',
    ADD COLUMN IF NOT EXISTS media_status VARCHAR(40) NOT NULL DEFAULT 'PENDING',
    ADD COLUMN IF NOT EXISTS chunk_count INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS token_count BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS error_code VARCHAR(80),
    ADD COLUMN IF NOT EXISTS error_message VARCHAR(2000),
    ADD COLUMN IF NOT EXISTS extra_metadata JSONB NOT NULL DEFAULT '{}'::jsonb;

UPDATE knowledge_assets
SET mime_type = media_type,
    parse_status = CASE status WHEN 'READY' THEN 'READY' WHEN 'ERROR' THEN 'FAILED' ELSE 'UPLOADED' END,
    ragflow_status = CASE WHEN ragflow_document_id IS NOT NULL THEN 'SYNCED' ELSE 'PENDING' END,
    media_status = CASE WHEN document_format = 'PPTX' AND page_count > 0 THEN 'COMPLETED' ELSE 'NOT_REQUIRED' END,
    knowledge_type = CASE
      WHEN lower(title || ' ' || industry) LIKE '%产品%' THEN 'PRODUCT'
      WHEN lower(title || ' ' || industry) LIKE '%案例%' THEN 'CASE'
      WHEN lower(title || ' ' || industry) LIKE '%故障%' THEN 'TROUBLESHOOTING'
      WHEN lower(title || ' ' || industry) LIKE '%模板%' THEN 'TEMPLATE'
      ELSE 'SOLUTION'
    END
WHERE mime_type IS NULL;

ALTER TABLE knowledge_assets
    ADD CONSTRAINT ck_knowledge_asset_status CHECK (status IN ('PROCESSING', 'READY', 'ERROR'));

CREATE TABLE knowledge_asset_parse_tasks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    asset_id UUID NOT NULL REFERENCES knowledge_assets(id) ON DELETE CASCADE,
    stage VARCHAR(40) NOT NULL,
    status VARCHAR(20) NOT NULL,
    progress INTEGER NOT NULL DEFAULT 0,
    attempt INTEGER NOT NULL DEFAULT 0,
    parser_strategy VARCHAR(30),
    error_code VARCHAR(80),
    error_message VARCHAR(2000),
    started_at TIMESTAMPTZ,
    finished_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_asset_parse_task_status CHECK (status IN ('PENDING', 'RUNNING', 'SUCCEEDED', 'FAILED')),
    CONSTRAINT ck_asset_parse_task_progress CHECK (progress BETWEEN 0 AND 100)
);

CREATE INDEX idx_knowledge_asset_parse_task_asset ON knowledge_asset_parse_tasks(asset_id, created_at DESC);
CREATE INDEX idx_knowledge_asset_parse_task_pending ON knowledge_asset_parse_tasks(status, created_at);
CREATE INDEX idx_knowledge_assets_metadata ON knowledge_assets(knowledge_type, document_type, industry, product);
CREATE INDEX idx_knowledge_assets_parser ON knowledge_assets(parser_strategy, parse_status);

ALTER TABLE knowledge_asset_media
    ADD COLUMN IF NOT EXISTS thumbnail_path VARCHAR(1000),
    ADD COLUMN IF NOT EXISTS width INTEGER,
    ADD COLUMN IF NOT EXISTS height INTEGER,
    ADD COLUMN IF NOT EXISTS duration_seconds NUMERIC(12,3),
    ADD COLUMN IF NOT EXISTS title VARCHAR(500) NOT NULL DEFAULT '',
    ADD COLUMN IF NOT EXISTS description TEXT NOT NULL DEFAULT '';

ALTER TABLE solution_sections
    ADD COLUMN IF NOT EXISTS purpose TEXT NOT NULL DEFAULT '',
    ADD COLUMN IF NOT EXISTS required_knowledge_types JSONB NOT NULL DEFAULT '[]'::jsonb,
    ADD COLUMN IF NOT EXISTS preferred_datasets JSONB NOT NULL DEFAULT '[]'::jsonb,
    ADD COLUMN IF NOT EXISTS retrieval_queries JSONB NOT NULL DEFAULT '[]'::jsonb,
    ADD COLUMN IF NOT EXISTS generation_requirements TEXT NOT NULL DEFAULT '',
    ADD COLUMN IF NOT EXISTS section_status VARCHAR(30) NOT NULL DEFAULT 'PLANNED',
    ADD COLUMN IF NOT EXISTS locked BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS key_points JSONB NOT NULL DEFAULT '[]'::jsonb,
    ADD COLUMN IF NOT EXISTS product_references JSONB NOT NULL DEFAULT '[]'::jsonb,
    ADD COLUMN IF NOT EXISTS case_references JSONB NOT NULL DEFAULT '[]'::jsonb,
    ADD COLUMN IF NOT EXISTS suggested_images JSONB NOT NULL DEFAULT '[]'::jsonb,
    ADD COLUMN IF NOT EXISTS suggested_slides JSONB NOT NULL DEFAULT '[]'::jsonb;

ALTER TABLE solution_citations
    ADD COLUMN IF NOT EXISTS asset_id UUID REFERENCES knowledge_assets(id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS slide_number INTEGER,
    ADD COLUMN IF NOT EXISTS evidence_id VARCHAR(100);
