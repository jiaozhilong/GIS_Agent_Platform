CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE platform_roles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(40) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500) NOT NULL DEFAULT '',
    built_in BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE platform_permissions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(80) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    module VARCHAR(60) NOT NULL,
    description VARCHAR(500) NOT NULL DEFAULT ''
);

CREATE TABLE platform_users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username VARCHAR(50) NOT NULL UNIQUE,
    display_name VARCHAR(100) NOT NULL,
    email VARCHAR(160) NOT NULL UNIQUE,
    phone VARCHAR(30),
    department VARCHAR(120),
    password_hash VARCHAR(100) NOT NULL,
    status VARCHAR(20) NOT NULL,
    last_login_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ck_platform_users_status CHECK (status IN ('ACTIVE', 'DISABLED', 'LOCKED'))
);

CREATE TABLE platform_user_roles (
    user_id UUID NOT NULL REFERENCES platform_users(id) ON DELETE CASCADE,
    role_id UUID NOT NULL REFERENCES platform_roles(id) ON DELETE RESTRICT,
    PRIMARY KEY (user_id, role_id)
);

CREATE TABLE platform_role_permissions (
    role_id UUID NOT NULL REFERENCES platform_roles(id) ON DELETE CASCADE,
    permission_id UUID NOT NULL REFERENCES platform_permissions(id) ON DELETE RESTRICT,
    PRIMARY KEY (role_id, permission_id)
);

CREATE TABLE platform_audit_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES platform_users(id) ON DELETE SET NULL,
    username VARCHAR(50),
    action VARCHAR(100) NOT NULL,
    target_type VARCHAR(80),
    target_id VARCHAR(100),
    detail_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    ip_address VARCHAR(64),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE solution_generation_runs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id VARCHAR(80) NOT NULL,
    requested_by UUID REFERENCES platform_users(id) ON DELETE SET NULL,
    status VARCHAR(20) NOT NULL,
    stage VARCHAR(30) NOT NULL,
    grounding_policy VARCHAR(20) NOT NULL,
    allow_model_supplement BOOLEAN NOT NULL DEFAULT TRUE,
    knowledge_base_ids JSONB NOT NULL DEFAULT '[]'::jsonb,
    output_formats JSONB NOT NULL DEFAULT '[]'::jsonb,
    ragflow_assistant_id VARCHAR(100),
    ragflow_session_id VARCHAR(100),
    model_name VARCHAR(160),
    evidence_coverage NUMERIC(5,4) NOT NULL DEFAULT 0,
    error_message VARCHAR(1000),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_solution_run_status CHECK (status IN ('PENDING', 'RUNNING', 'SUCCEEDED', 'FAILED', 'CANCELLED')),
    CONSTRAINT ck_solution_run_stage CHECK (stage IN ('PLANNING', 'GENERATING', 'REVIEWING', 'COMPLETED'))
);

CREATE TABLE solution_sections (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    run_id UUID NOT NULL REFERENCES solution_generation_runs(id) ON DELETE CASCADE,
    section_key VARCHAR(80) NOT NULL,
    title VARCHAR(300) NOT NULL,
    content TEXT NOT NULL DEFAULT '',
    source_type VARCHAR(30) NOT NULL,
    evidence_coverage NUMERIC(5,4) NOT NULL DEFAULT 0,
    confirmation_reason VARCHAR(1000),
    sort_order INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_solution_section UNIQUE (run_id, section_key),
    CONSTRAINT ck_solution_section_source CHECK (source_type IN ('KNOWLEDGE_BASE', 'MODEL_GENERATED', 'HYBRID', 'PENDING_CONFIRMATION'))
);

CREATE TABLE solution_citations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    section_id UUID NOT NULL REFERENCES solution_sections(id) ON DELETE CASCADE,
    ragflow_chunk_id VARCHAR(160),
    dataset_id VARCHAR(160),
    document_id VARCHAR(160),
    document_name VARCHAR(500),
    content_snapshot TEXT,
    similarity_score NUMERIC(8,6),
    page_number INTEGER,
    metadata_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_platform_users_status ON platform_users(status);
CREATE INDEX idx_platform_users_display_name ON platform_users(display_name);
CREATE INDEX idx_platform_audit_logs_created_at ON platform_audit_logs(created_at DESC);
CREATE INDEX idx_solution_runs_project_created ON solution_generation_runs(project_id, created_at DESC);
CREATE INDEX idx_solution_sections_run_sort ON solution_sections(run_id, sort_order);

INSERT INTO platform_roles (id, code, name, description, built_in) VALUES
('00000000-0000-0000-0000-000000000001', 'ADMIN', '系统管理员', '平台、用户、模型和全部业务管理权限', TRUE),
('00000000-0000-0000-0000-000000000002', 'CONSULTANT', '解决方案顾问', '项目分析、知识检索与方案生成权限', TRUE),
('00000000-0000-0000-0000-000000000003', 'REVIEWER', '方案审核员', '项目、方案和引用证据查看审核权限', TRUE)
ON CONFLICT (code) DO NOTHING;

INSERT INTO platform_permissions (code, name, module, description) VALUES
('dashboard:view', '查看总览', '总览', '查看平台运营与项目统计'),
('project:view', '查看项目', '项目', '查看项目及其工作台'),
('project:manage', '管理项目', '项目', '创建和修改项目'),
('agent:run', '运行智能体', '智能体', '运行需求分析和产品匹配'),
('knowledge:view', '查看知识库', '知识库', '查看知识库和引用'),
('knowledge:manage', '管理知识库', '知识库', '上传资料和调整知识库映射'),
('proposal:view', '查看方案', '方案', '查看方案正文和版本'),
('proposal:generate', '生成方案', '方案', '运行 RAGFlow 联合生成'),
('proposal:export', '导出方案', '方案', '导出 DOCX、PPTX 和 PDF'),
('model:view', '查看模型配置', '模型', '查看脱敏后的模型配置'),
('model:manage', '管理模型配置', '模型', '修改并测试 RAGFlow 和模型配置'),
('user:view', '查看用户', '用户', '查看平台用户'),
('user:manage', '管理用户', '用户', '创建、修改、停用和重置用户'),
('role:view', '查看角色', '角色', '查看角色与权限'),
('role:manage', '管理角色', '角色', '配置自定义角色权限'),
('audit:view', '查看审计日志', '审计', '查看关键操作审计日志')
ON CONFLICT (code) DO NOTHING;

INSERT INTO platform_role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM platform_roles r CROSS JOIN platform_permissions p WHERE r.code = 'ADMIN'
ON CONFLICT DO NOTHING;

INSERT INTO platform_role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM platform_roles r CROSS JOIN platform_permissions p
WHERE r.code = 'CONSULTANT' AND p.code IN (
  'dashboard:view', 'project:view', 'project:manage', 'agent:run', 'knowledge:view',
  'knowledge:manage', 'proposal:view', 'proposal:generate', 'proposal:export', 'model:view', 'role:view'
)
ON CONFLICT DO NOTHING;

INSERT INTO platform_role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM platform_roles r CROSS JOIN platform_permissions p
WHERE r.code = 'REVIEWER' AND p.code IN ('dashboard:view', 'project:view', 'knowledge:view', 'proposal:view', 'role:view')
ON CONFLICT DO NOTHING;
