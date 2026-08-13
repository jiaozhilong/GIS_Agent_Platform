CREATE TABLE knowledge_assets (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ragflow_dataset_id VARCHAR(160) NOT NULL,
    ragflow_document_id VARCHAR(160),
    asset_type VARCHAR(20) NOT NULL,
    document_format VARCHAR(20),
    title VARCHAR(300) NOT NULL,
    original_filename VARCHAR(500) NOT NULL,
    media_type VARCHAR(160) NOT NULL,
    description TEXT NOT NULL DEFAULT '',
    industry VARCHAR(120) NOT NULL DEFAULT '',
    tags JSONB NOT NULL DEFAULT '[]'::jsonb,
    status VARCHAR(30) NOT NULL,
    status_message VARCHAR(1000) NOT NULL DEFAULT '',
    storage_path VARCHAR(1000) NOT NULL,
    file_size BIGINT NOT NULL DEFAULT 0,
    page_count INTEGER NOT NULL DEFAULT 0,
    extracted_image_count INTEGER NOT NULL DEFAULT 0,
    extracted_video_count INTEGER NOT NULL DEFAULT 0,
    created_by VARCHAR(100) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_knowledge_asset_type CHECK (asset_type IN ('DOCUMENT', 'IMAGE', 'VIDEO')),
    CONSTRAINT ck_knowledge_asset_status CHECK (status IN ('PROCESSING', 'READY', 'ERROR'))
);

CREATE TABLE knowledge_asset_pages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    asset_id UUID NOT NULL REFERENCES knowledge_assets(id) ON DELETE CASCADE,
    page_number INTEGER NOT NULL,
    title VARCHAR(500) NOT NULL DEFAULT '',
    text_content TEXT NOT NULL DEFAULT '',
    preview_path VARCHAR(1000),
    image_count INTEGER NOT NULL DEFAULT 0,
    video_count INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_knowledge_asset_page UNIQUE (asset_id, page_number)
);

CREATE TABLE knowledge_asset_media (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    asset_id UUID NOT NULL REFERENCES knowledge_assets(id) ON DELETE CASCADE,
    page_number INTEGER,
    media_kind VARCHAR(20) NOT NULL,
    filename VARCHAR(500) NOT NULL,
    media_type VARCHAR(160) NOT NULL,
    storage_path VARCHAR(1000),
    external_url VARCHAR(2000),
    file_size BIGINT NOT NULL DEFAULT 0,
    source_kind VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_knowledge_asset_media_kind CHECK (media_kind IN ('IMAGE', 'VIDEO')),
    CONSTRAINT ck_knowledge_asset_media_source CHECK (source_kind IN ('EXTRACTED', 'UPLOADED', 'LINKED'))
);

CREATE INDEX idx_knowledge_assets_dataset ON knowledge_assets(ragflow_dataset_id);
CREATE INDEX idx_knowledge_assets_document ON knowledge_assets(ragflow_document_id);
CREATE INDEX idx_knowledge_assets_type_status ON knowledge_assets(asset_type, status);
CREATE INDEX idx_knowledge_asset_pages_asset ON knowledge_asset_pages(asset_id, page_number);
CREATE INDEX idx_knowledge_asset_media_asset_page ON knowledge_asset_media(asset_id, page_number);
