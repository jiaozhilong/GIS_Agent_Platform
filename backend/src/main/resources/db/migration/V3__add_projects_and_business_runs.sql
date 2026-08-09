CREATE TABLE platform_projects (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_code VARCHAR(60) NOT NULL UNIQUE,
    name VARCHAR(240) NOT NULL,
    customer_name VARCHAR(200) NOT NULL,
    industry VARCHAR(100) NOT NULL,
    region VARCHAR(120) NOT NULL DEFAULT '',
    background TEXT NOT NULL DEFAULT '',
    raw_demand TEXT NOT NULL,
    goals JSONB NOT NULL DEFAULT '[]'::jsonb,
    delivery_deadline DATE,
    knowledge_base_ids JSONB NOT NULL DEFAULT '[]'::jsonb,
    collaborator_names JSONB NOT NULL DEFAULT '[]'::jsonb,
    stage VARCHAR(40) NOT NULL DEFAULT 'DRAFT',
    progress INTEGER NOT NULL DEFAULT 5,
    owner_id UUID REFERENCES platform_users(id) ON DELETE SET NULL,
    owner_name VARCHAR(100) NOT NULL DEFAULT '',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_platform_project_stage CHECK (stage IN ('DRAFT', 'REQUIREMENT_ANALYSIS', 'PRODUCT_MATCH', 'KNOWLEDGE_RETRIEVAL', 'PROPOSAL_GENERATION', 'REVIEW', 'DELIVERED')),
    CONSTRAINT ck_platform_project_progress CHECK (progress BETWEEN 0 AND 100)
);

CREATE TABLE requirement_analysis_runs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL REFERENCES platform_projects(id) ON DELETE CASCADE,
    result_json JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE product_match_runs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL REFERENCES platform_projects(id) ON DELETE CASCADE,
    result_json JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE retrieval_runs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL REFERENCES platform_projects(id) ON DELETE CASCADE,
    query TEXT NOT NULL,
    knowledge_base_ids JSONB NOT NULL,
    top_k INTEGER NOT NULL,
    similarity_threshold NUMERIC(5,4) NOT NULL,
    duration_ms BIGINT NOT NULL,
    result_json JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_platform_projects_updated ON platform_projects(updated_at DESC);
CREATE INDEX idx_requirement_project_created ON requirement_analysis_runs(project_id, created_at DESC);
CREATE INDEX idx_product_match_project_created ON product_match_runs(project_id, created_at DESC);
CREATE INDEX idx_retrieval_project_created ON retrieval_runs(project_id, created_at DESC);

INSERT INTO platform_projects (
    id, project_code, name, customer_name, industry, region, background, raw_demand,
    goals, delivery_deadline, knowledge_base_ids, collaborator_names, stage, progress,
    owner_id, owner_name
)
SELECT
    '10000000-0000-0000-0000-000000000001',
    'GIS-TEST-2026-001',
    '自然资源一张图智能化升级测试项目',
    'GIS Agent 联调测试单位',
    '自然资源',
    '北京市',
    '现有多个 GIS 与自然资源业务系统，空间数据标准不统一、服务分散，需要验证平台从需求分析到联合方案生成的完整链路。',
    '整合现有 GIS 平台、国土空间规划、不动产与审批数据，建设统一时空信息底座和一张图应用；支持二三维一体化、专题图层管理、空间分析、服务共享、权限审计，并使用知识库与大模型联合生成可追溯方案。',
    '["统一多源空间数据标准", "建设二三维一体化一张图", "形成可追溯的智能方案生成流程", "验证 RAGFlow、BGE-M3 与 DeepSeek 联合能力"]'::jsonb,
    CURRENT_DATE + 60,
    '["16fa00148e3611f182738369d8cbce44"]'::jsonb,
    '["系统管理员", "方案顾问", "GIS 工程师"]'::jsonb,
    'DRAFT',
    10,
    u.id,
    u.display_name
FROM platform_users u
WHERE lower(u.username) = 'admin'
ON CONFLICT (project_code) DO NOTHING;
