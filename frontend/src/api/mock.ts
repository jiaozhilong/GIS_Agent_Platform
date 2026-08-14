import type {
  ApiResponse, DashboardSummary, KnowledgeBase, LoginResult, ModelConfig, ProductMatch,
  PageResponse, PermissionItem, ProjectDetail, ProjectSummary, ProposalDocument, RequirementAnalysis,
  RetrievalResult, RoleSummary, SystemUser
} from './contracts'

const now = () => new Date().toISOString()
const ok = <T>(data: T): ApiResponse<T> => ({ code: 'OK', message: 'success', requestId: crypto.randomUUID(), timestamp: now(), data })
const wait = (ms = 320) => new Promise((resolve) => setTimeout(resolve, ms))

export const projects: ProjectSummary[] = [
  { id: 'prj-001', name: 'XX市国土空间基础信息平台建设项目', customerName: 'XX市自然资源和规划局', industry: '自然资源', stage: 'PROPOSAL_GENERATION', progress: 68, ownerName: '张文博', updatedAt: '2026-08-02T10:28:00+08:00' },
  { id: 'prj-002', name: '水利数字孪生流域平台', customerName: 'XX省水利厅', industry: '水利', stage: 'PRODUCT_MATCH', progress: 42, ownerName: '李然', updatedAt: '2026-08-02T09:45:00+08:00' },
  { id: 'prj-003', name: '城市运行一网统管升级', customerName: 'XX市大数据局', industry: '智慧城市', stage: 'KNOWLEDGE_RETRIEVAL', progress: 55, ownerName: '王玉', updatedAt: '2026-08-01T17:30:00+08:00' }
]

export const projectDetail: ProjectDetail = {
  ...projects[0], projectCode: 'GIS-2026-0802-001', region: 'XX市', deliveryDeadline: '2026-09-30',
  background: '整合现有自然资源业务系统与空间数据，建设统一时空底座和国土空间“一张图”。',
  rawDemand: '支持二三维一体化展示、专题图层管理、空间分析、规划审批联动与智能辅助决策。',
  goals: ['统一空间数据底座', '形成一张图应用体系', '接入AI智能分析能力', '建立数据治理闭环'],
  knowledgeBaseIds: ['kb-01', 'kb-02', 'kb-03'], collaboratorNames: ['陈云', '李园', '王玉', '林木']
}

export const knowledgeBases: KnowledgeBase[] = [
  { id: 'kb-01', name: '01-产品技术知识库', knowledgeType: 'PRODUCT_TECH', description: 'GIS平台、产品能力与技术架构资料', documentCount: 28, chunkCount: 642, embeddingModel: 'BGE-M3 / 1024维', chunkMethod: 'Manual', status: 'READY', updatedAt: '2026-08-02T08:50:00+08:00' },
  { id: 'kb-02', name: '02-行业解决方案库', knowledgeType: 'INDUSTRY_SOLUTION', description: '自然资源、水利、规划等行业方案', documentCount: 36, chunkCount: 728, embeddingModel: 'BGE-M3 / 1024维', chunkMethod: 'Manual', status: 'READY', updatedAt: '2026-08-02T08:42:00+08:00' },
  { id: 'kb-03', name: '03-历史项目案例库', knowledgeType: 'PROJECT_CASE', description: '历史项目建设内容、难点与成效', documentCount: 19, chunkCount: 383, embeddingModel: 'BGE-M3 / 1024维', chunkMethod: 'Manual', status: 'READY', updatedAt: '2026-08-01T18:30:00+08:00' },
  { id: 'kb-04', name: '04-故障排查知识库', knowledgeType: 'TROUBLESHOOTING', description: '常见故障、原因与处理步骤', documentCount: 12, chunkCount: 186, embeddingModel: 'BGE-M3 / 1024维', chunkMethod: 'Q&A', status: 'READY', updatedAt: '2026-08-01T16:15:00+08:00' },
  { id: 'kb-05', name: '05-模板资料库', knowledgeType: 'TEMPLATE', description: 'Word、PPT与实施方案模板', documentCount: 8, chunkCount: 96, embeddingModel: 'BGE-M3 / 1024维', chunkMethod: 'Presentation', status: 'READY', updatedAt: '2026-08-01T15:40:00+08:00' }
]

const permissionCodes = [
  'dashboard:view', 'project:view', 'project:manage', 'agent:run', 'knowledge:view',
  'knowledge:manage', 'proposal:view', 'proposal:generate', 'proposal:export',
  'model:view', 'model:manage', 'user:view', 'user:manage', 'role:view', 'role:manage', 'audit:view'
]

export const roles: RoleSummary[] = [
  { id: 'role-admin', code: 'ADMIN', name: '系统管理员', description: '平台、用户、模型和全部业务管理权限', builtIn: true, userCount: 2, permissionCodes },
  { id: 'role-consultant', code: 'CONSULTANT', name: '解决方案顾问', description: '项目分析、知识检索与方案生成权限', builtIn: true, userCount: 8, permissionCodes: permissionCodes.filter((code) => !code.startsWith('user:') && !code.startsWith('role:') && code !== 'model:manage' && code !== 'audit:view') },
  { id: 'role-reviewer', code: 'REVIEWER', name: '方案审核员', description: '项目、方案和引用证据查看审核权限', builtIn: true, userCount: 3, permissionCodes: ['dashboard:view', 'project:view', 'knowledge:view', 'proposal:view', 'role:view'] }
]

export const permissions: PermissionItem[] = permissionCodes.map((code) => {
  const [module, action] = code.split(':')
  const moduleNames: Record<string, string> = { dashboard: '总览', project: '项目', agent: '智能体', knowledge: '知识库', proposal: '方案', model: '模型', user: '用户', role: '角色', audit: '审计' }
  const actionNames: Record<string, string> = { view: '查看', manage: '管理', run: '运行', generate: '生成', export: '导出' }
  return { code, name: `${moduleNames[module]}${actionNames[action]}`, module: moduleNames[module], description: `${moduleNames[module]}模块的${actionNames[action]}权限` }
})

export const systemUsers: SystemUser[] = [
  { id: 'u-admin', username: 'admin', displayName: '系统管理员', email: 'admin@gis-agent.local', phone: '13800000001', department: '平台管理部', status: 'ACTIVE', roleCodes: ['ADMIN'], lastLoginAt: '2026-08-02T18:20:00+08:00', createdAt: '2026-07-20T09:00:00+08:00', updatedAt: '2026-08-02T18:20:00+08:00' },
  { id: 'u-01', username: 'zhangwenbo', displayName: '张文博', email: 'consultant@gis-agent.local', phone: '13800000002', department: '解决方案中心', status: 'ACTIVE', roleCodes: ['CONSULTANT'], lastLoginAt: '2026-08-02T17:36:00+08:00', createdAt: '2026-07-21T09:00:00+08:00', updatedAt: '2026-08-02T17:36:00+08:00' },
  { id: 'u-02', username: 'liyan', displayName: '李岩', email: 'liyan@gis-agent.local', phone: '13800000003', department: 'GIS研发中心', status: 'ACTIVE', roleCodes: ['CONSULTANT'], lastLoginAt: '2026-08-02T15:10:00+08:00', createdAt: '2026-07-22T09:00:00+08:00', updatedAt: '2026-08-01T16:00:00+08:00' },
  { id: 'u-03', username: 'wangyu', displayName: '王玉', email: 'wangyu@gis-agent.local', department: '质量管理部', status: 'DISABLED', roleCodes: ['REVIEWER'], createdAt: '2026-07-23T09:00:00+08:00', updatedAt: '2026-07-31T11:20:00+08:00' }
]

export async function mockRequest<T>(method: string, path: string, body?: unknown): Promise<ApiResponse<T>> {
  await wait()
  const [route, queryString = ''] = path.split('?')
  if (route === '/auth/login' && method === 'POST') return ok({ accessToken: 'mock-jwt-token', tokenType: 'Bearer', expiresIn: 7200, user: { id: 'u-admin', username: 'admin', displayName: '系统管理员', email: 'admin@gis-agent.local', role: 'ADMIN', roleCodes: ['ADMIN'], permissions: permissionCodes } } as LoginResult) as ApiResponse<T>
  if (route === '/auth/me' && method === 'GET') return ok({ id: 'u-admin', username: 'admin', displayName: '系统管理员', email: 'admin@gis-agent.local', role: 'ADMIN', roleCodes: ['ADMIN'], permissions: permissionCodes } as unknown as T)
  if (route === '/users' && method === 'GET') {
    const query = new URLSearchParams(queryString)
    const keyword = (query.get('keyword') ?? '').toLowerCase()
    const status = query.get('status')
    const roleCode = query.get('roleCode')
    const filtered = systemUsers.filter((user) =>
      (!keyword || [user.username, user.displayName, user.email, user.department ?? ''].some((value) => value.toLowerCase().includes(keyword))) &&
      (!status || user.status === status) && (!roleCode || user.roleCodes.includes(roleCode as SystemUser['roleCodes'][number])))
    const page = Number(query.get('page') ?? 1)
    const pageSize = Number(query.get('pageSize') ?? 20)
    return ok({ items: filtered.slice((page - 1) * pageSize, page * pageSize), page, pageSize, total: filtered.length } as PageResponse<SystemUser> as unknown as T)
  }
  if (route === '/users' && method === 'POST') {
    const input = body as { username: string; displayName: string; email: string; phone?: string; department?: string; roleCodes: SystemUser['roleCodes'] }
    const created: SystemUser = { id: crypto.randomUUID(), ...input, status: 'ACTIVE', createdAt: now(), updatedAt: now() }
    systemUsers.unshift(created)
    return ok(created as unknown as T)
  }
  const userMatch = route.match(/^\/users\/([^/]+)$/)
  if (userMatch && method === 'PUT') {
    const user = systemUsers.find((item) => item.id === userMatch[1])!
    Object.assign(user, body, { updatedAt: now() })
    return ok(user as unknown as T)
  }
  const statusMatch = route.match(/^\/users\/([^/]+)\/status$/)
  if (statusMatch && method === 'PATCH') {
    const user = systemUsers.find((item) => item.id === statusMatch[1])!
    Object.assign(user, body, { updatedAt: now() })
    return ok(user as unknown as T)
  }
  const roleMatch = route.match(/^\/users\/([^/]+)\/roles$/)
  if (roleMatch && method === 'PUT') {
    const user = systemUsers.find((item) => item.id === roleMatch[1])!
    Object.assign(user, body, { updatedAt: now() })
    return ok(user as unknown as T)
  }
  if (/^\/users\/[^/]+\/reset-password$/.test(route) && method === 'POST') return ok(undefined as T)
  if (route === '/roles' && method === 'GET') return ok(roles as unknown as T)
  if (route === '/permissions' && method === 'GET') return ok(permissions as unknown as T)
  if (path === '/dashboard/summary') return ok({ projectCount: 128, requirementTaskCount: 86, proposalCount: 42, knowledgeDocumentCount: 10, runningAgentCount: 15, systemStatus: 'HEALTHY', projects, stageCounts: { DRAFT: 1, REQUIREMENT_ANALYSIS: 1 }, knowledgeBases: knowledgeBases.map(item => ({ id: item.id, name: item.name, documentCount: item.documentCount, chunkCount: item.chunkCount, ready: item.status === 'READY' })) } as DashboardSummary) as ApiResponse<T>
  if (path === '/projects') return ok(projects as unknown as T)
  if (/^\/projects\/[^/]+$/.test(path)) return ok(projectDetail as unknown as T)
  if (path.endsWith('/requirement-analysis')) return ok({ taskId: 'req-001', status: 'SUCCEEDED', completion: 86, demandPoints: ['建设统一时空信息底座', '整合多源异构空间数据', '支撑国土空间规划一张图', '提供智能分析与辅助决策'], summary: '客户需要以统一空间底座为核心，打通数据治理、GIS服务、业务应用与智能分析能力。', dimensions: [{ name: '数据治理', score: 92, description: '多源空间数据汇聚与标准化' }, { name: '平台能力', score: 76, description: '二三维一体化GIS平台' }, { name: '业务应用', score: 81, description: '规划审批与专题应用' }, { name: '智能分析', score: 66, description: 'AI辅助分析和方案生成' }, { name: '开放集成', score: 70, description: '服务共享和系统对接' }], recommendedProductNames: ['SuperMap GIS Cloud', 'iServer 3D', 'iObjects X'] } as RequirementAnalysis) as ApiResponse<T>
  if (path.endsWith('/product-matches')) return ok([{ productId: 'prod-01', productName: 'SuperMap GIS Cloud', productFamily: 'GIS云平台', matchScore: 96, matchedCapabilities: ['时空数据治理', '云原生GIS', '服务共享'], gaps: [], recommended: true }, { productId: 'prod-02', productName: 'SuperMap iServer 3D', productFamily: '三维GIS', matchScore: 92, matchedCapabilities: ['三维服务发布', '空间分析'], gaps: ['需补充GPU资源评估'], recommended: true }, { productId: 'prod-03', productName: 'SuperMap iObjects X', productFamily: '组件开发', matchScore: 84, matchedCapabilities: ['业务系统集成', '空间计算'], gaps: ['需二次开发'], recommended: false }] as ProductMatch[] as unknown as T)
  if (path.endsWith('/retrievals')) return ok({ taskId: 'ret-001', status: 'SUCCEEDED', query: (body as { query?: string })?.query ?? '', durationMs: 892, hits: [
    { id: 'hit-1', knowledgeBaseId: 'kb-01', knowledgeBaseName: '01-产品技术知识库', documentId: 'doc-01', documentName: 'GIS平台总体架构设计规范V2.1.pdf', chunkId: 'chunk-101', content: '平台采用数据层、服务层、应用层和安全保障体系的分层架构，统一提供时空数据治理、GIS服务发布与业务集成能力。', score: .94, pageNumber: 18, metadata: { product: 'GIS平台', version: 'V2.1' } },
    { id: 'hit-2', knowledgeBaseId: 'kb-02', knowledgeBaseName: '02-行业解决方案库', documentId: 'doc-08', documentName: '自然资源一张图建设解决方案.pdf', chunkId: 'chunk-325', content: '围绕自然资源调查监测、国土空间规划、用途管制和执法监管，构建统一底图、统一标准、统一服务的一张图应用体系。', score: .90, pageNumber: 12, metadata: { industry: '自然资源', year: '2025' } },
    { id: 'hit-3', knowledgeBaseId: 'kb-03', knowledgeBaseName: '03-历史项目案例库', documentId: 'doc-13', documentName: '某省自然资源一张图项目案例.docx', chunkId: 'chunk-451', content: '项目通过统一空间底座和服务化架构，实现省市县三级数据汇聚、业务协同与智能分析，形成可复用的建设模式。', score: .88, pageNumber: 6, metadata: { region: '某省', status: '已验收' } }
  ] } as RetrievalResult) as ApiResponse<T>
  if (path.endsWith('/proposals') && method === 'POST') return ok({ id: 'proposal-01', projectId: 'prj-001', title: 'XX市国土空间基础信息平台建设方案', version: 3, status: 'SUCCEEDED', coverImageUrl: '/assets/images/proposal-cover-city.png', updatedAt: now(), sections: [
    { id: 's1', title: '1. 项目概述', level: 1, status: 'GENERATED', content: '本项目面向自然资源数字化治理需求，建设统一、开放、智能的国土空间基础信息平台。', citationIds: ['hit-2'] },
    { id: 's2', title: '2. 建设背景与需求分析', level: 1, status: 'GENERATED', content: '结合现状系统和数据资源，识别数据孤岛、服务分散与业务协同不足等问题。', citationIds: ['hit-3'] },
    { id: 's3', title: '3. 总体架构', level: 1, status: 'GENERATED', content: '采用基础设施层、数据层、平台层、服务层、应用层与安全保障体系的总体架构。', citationIds: ['hit-1'] },
    { id: 's4', title: '4. 建设内容', level: 1, status: 'EMPTY', content: '', citationIds: [] },
    { id: 's5', title: '5. 实施计划', level: 1, status: 'EMPTY', content: '', citationIds: [] }
  ] } as ProposalDocument) as ApiResponse<T>
  if (path === '/knowledge-bases') return ok(knowledgeBases as unknown as T)
  if (path === '/model-configs') return ok([
    { provider: 'RAGFLOW', displayName: 'RAGFlow 知识库', category: 'KNOWLEDGE_ENGINE', baseUrl: 'http://localhost:8088', modelName: 'RAGFlow v0.26.4', status: 'HEALTHY', lastCheckedAt: now(), editable: false, enabled: true },
    { provider: 'DEEPSEEK', displayName: 'RAGFlow Assistant 语言模型', category: 'KNOWLEDGE_ENGINE', baseUrl: 'https://api.deepseek.com', modelName: 'deepseek-chat', maskedApiKey: 'sk-****28af', status: 'HEALTHY', lastCheckedAt: now(), editable: false, enabled: true },
    { provider: 'BGE_M3', displayName: 'BGE-M3 向量模型', category: 'KNOWLEDGE_ENGINE', baseUrl: 'http://host.docker.internal:8001/v1', modelName: 'BAAI/bge-m3', status: 'HEALTHY', lastCheckedAt: now(), editable: false, enabled: true },
    { provider: 'PLATFORM_LLM', displayName: '平台方案生成模型', category: 'PLATFORM_GENERATION', baseUrl: '', modelName: '', status: 'UNCONFIGURED', lastCheckedAt: now(), editable: true, enabled: false }
  ] as ModelConfig[] as unknown as T)
  return ok({} as T)
}
