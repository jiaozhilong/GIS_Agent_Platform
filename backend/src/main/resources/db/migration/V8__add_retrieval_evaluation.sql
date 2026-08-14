create table if not exists retrieval_evaluation_cases (
    id uuid primary key default gen_random_uuid(),
    query varchar(2000) not null unique,
    expected_dataset_keyword varchar(200),
    expected_document_keyword varchar(500),
    expected_section varchar(500),
    expected_page integer,
    expected_slide integer,
    expected_knowledge_type varchar(80),
    active boolean not null default true,
    created_at timestamptz not null default current_timestamp,
    updated_at timestamptz not null default current_timestamp
);

create table if not exists retrieval_evaluation_runs (
    id uuid primary key default gen_random_uuid(),
    status varchar(30) not null,
    total_cases integer not null default 0,
    completed_cases integer not null default 0,
    metrics jsonb not null default '{}'::jsonb,
    error_message varchar(2000),
    started_at timestamptz,
    finished_at timestamptz,
    created_at timestamptz not null default current_timestamp,
    updated_at timestamptz not null default current_timestamp
);

create table if not exists retrieval_evaluation_results (
    id uuid primary key default gen_random_uuid(),
    run_id uuid not null references retrieval_evaluation_runs(id) on delete cascade,
    case_id uuid not null references retrieval_evaluation_cases(id),
    duration_ms bigint not null default 0,
    dataset_hit_rank integer,
    document_hit_rank integer,
    slide_hit_rank integer,
    top_hits jsonb not null default '[]'::jsonb,
    error_message varchar(2000),
    created_at timestamptz not null default current_timestamp,
    unique(run_id, case_id)
);

insert into retrieval_evaluation_cases(query, expected_dataset_keyword, expected_document_keyword, expected_knowledge_type) values
('自然资源一张图总体架构怎么设计？','行业解决方案',null,'SOLUTION'),
('有没有自然资源一张图类似项目？','历史项目',null,'CASE'),
('iServer支持哪些地图服务？','产品技术','iServer','PRODUCT'),
('iServer有哪些空间分析能力？','产品技术','iServer','PRODUCT'),
('三维服务对应什么SuperMap产品？','产品技术',null,'PRODUCT'),
('找一张自然资源数据架构图。','行业解决方案',null,'SOLUTION'),
('有没有水利数字孪生解决方案？','行业解决方案',null,'SOLUTION'),
('找一个自然资源历史项目案例。','历史项目',null,'CASE'),
('iPortal有哪些门户共享能力？','产品技术','iPortal','PRODUCT'),
('iDesktopX支持哪些空间数据处理能力？','产品技术','iDesktop','PRODUCT'),
('SuperMap ImageX Enterprise有哪些遥感能力？','产品技术','ImageX','PRODUCT'),
('如何建设二三维一体化GIS平台？','行业解决方案',null,'SOLUTION'),
('数字孪生流域需要哪些GIS能力？','行业解决方案',null,'SOLUTION'),
('城市信息模型CIM平台总体架构是什么？','行业解决方案',null,'SOLUTION'),
('国土空间规划一张图需要哪些功能？','行业解决方案',null,'SOLUTION'),
('时空大数据平台如何设计数据资源体系？','行业解决方案',null,'SOLUTION'),
('iServer如何发布三维服务？','产品技术','iServer','PRODUCT'),
('iServer是否支持REST接口？','产品技术','iServer','PRODUCT'),
('iPortal如何管理地图和服务资源？','产品技术','iPortal','PRODUCT'),
('SuperMap GIS支持国产化环境吗？','产品技术',null,'PRODUCT'),
('找一个水利行业历史项目案例。','历史项目',null,'CASE'),
('找一个智慧城市GIS建设案例。','历史项目',null,'CASE'),
('自然资源一张图项目有哪些建设成效？','历史项目',null,'CASE'),
('GIS平台高可用部署怎么设计？','行业解决方案',null,'SOLUTION'),
('GIS平台安全体系包括哪些内容？','行业解决方案',null,'SOLUTION'),
('海量空间数据如何进行分布式管理？','产品技术',null,'PRODUCT'),
('SuperMap有哪些地理空间AI能力？','产品技术',null,'PRODUCT'),
('如何选择桌面GIS、服务器GIS和门户产品？','产品技术',null,'PRODUCT'),
('方案PPT通常包含哪些章节？','模板资料',null,'TEMPLATE'),
('GIS服务发布失败有哪些排查步骤？','故障排查',null,'TROUBLESHOOTING')
on conflict(query) do nothing;
