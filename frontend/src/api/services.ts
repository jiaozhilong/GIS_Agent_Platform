import { apiRequest, apiUpload } from './client'
import type {
  AssignUserRolesRequest, CreateKnowledgeBaseRequest, CreateUserRequest, DashboardSummary,
  GenerateProposalRequest, KnowledgeBase, KnowledgeDocument, KnowledgeSyncResult, LoginRequest,
  LoginResult, ModelConfig, PageResponse, PermissionItem, ProductMatch, ProjectDetail, ProjectSummary,
  ProjectUpsertRequest, ProposalDocument, RegisterRequest, RequirementAnalysis, ResetPasswordRequest,
  RetrievalRequest, RetrievalResult, RoleSummary, SolutionGenerationRequest, SolutionGenerationRun,
  SystemUser, UpdateUserRequest, UpdateUserStatusRequest
} from './contracts'

export const api = {
  login: (body: LoginRequest) => apiRequest<LoginResult>('POST', '/auth/login', body),
  register: (body: RegisterRequest) => apiRequest<LoginResult>('POST', '/auth/register', body),
  me: () => apiRequest<LoginResult['user']>('GET', '/auth/me'),
  dashboard: () => apiRequest<DashboardSummary>('GET', '/dashboard/summary'),
  projects: () => apiRequest<ProjectSummary[]>('GET', '/projects'),
  createProject: (body: ProjectUpsertRequest) => apiRequest<ProjectDetail>('POST', '/projects', body),
  project: (id: string) => apiRequest<ProjectDetail>('GET', `/projects/${id}`),
  updateProject: (id: string, body: ProjectUpsertRequest) => apiRequest<ProjectDetail>('PUT', `/projects/${id}`, body),
  analyzeRequirements: (id: string) => apiRequest<RequirementAnalysis>('POST', `/projects/${id}/requirement-analysis`),
  matchProducts: (id: string) => apiRequest<ProductMatch[]>('POST', `/projects/${id}/product-matches`),
  retrieve: (id: string, body: RetrievalRequest) => apiRequest<RetrievalResult>('POST', `/projects/${id}/retrievals`, body),
  generateProposal: (id: string, body: GenerateProposalRequest) => apiRequest<ProposalDocument>('POST', `/projects/${id}/proposals`, body),
  knowledgeBases: () => apiRequest<KnowledgeBase[]>('GET', '/knowledge-bases'),
  syncKnowledgeBases: () => apiRequest<KnowledgeSyncResult>('POST', '/knowledge-bases/sync'),
  createKnowledgeBase: (body: CreateKnowledgeBaseRequest) => apiRequest<KnowledgeBase>('POST', '/knowledge-bases', body),
  knowledgeDocuments: (id: string) => apiRequest<KnowledgeDocument[]>('GET', `/knowledge-bases/${id}/documents`),
  uploadKnowledgeDocument: (id: string, file: File) => apiUpload<KnowledgeDocument[]>(`/knowledge-bases/${id}/documents`, file),
  modelConfigs: () => apiRequest<ModelConfig[]>('GET', '/model-configs'),
  testModelConfig: (provider: ModelConfig['provider']) => apiRequest<ModelConfig>('POST', `/model-configs/${provider}/test`),
  saveModelConfig: (provider: ModelConfig['provider'], body: Pick<ModelConfig, 'baseUrl' | 'modelName'>) => apiRequest<ModelConfig>('PUT', `/model-configs/${provider}`, body),
  users: (query = '') => apiRequest<PageResponse<SystemUser>>('GET', `/users${query ? `?${query}` : ''}`),
  createUser: (body: CreateUserRequest) => apiRequest<SystemUser>('POST', '/users', body),
  updateUser: (id: string, body: UpdateUserRequest) => apiRequest<SystemUser>('PUT', `/users/${id}`, body),
  updateUserStatus: (id: string, body: UpdateUserStatusRequest) => apiRequest<SystemUser>('PATCH', `/users/${id}/status`, body),
  assignUserRoles: (id: string, body: AssignUserRolesRequest) => apiRequest<SystemUser>('PUT', `/users/${id}/roles`, body),
  resetUserPassword: (id: string, body: ResetPasswordRequest) => apiRequest<void>('POST', `/users/${id}/reset-password`, body),
  roles: () => apiRequest<RoleSummary[]>('GET', '/roles'),
  permissions: () => apiRequest<PermissionItem[]>('GET', '/permissions'),
  startSolutionRun: (projectId: string, body: SolutionGenerationRequest) => apiRequest<SolutionGenerationRun>('POST', `/projects/${projectId}/solution-runs`, body),
  solutionRun: (runId: string) => apiRequest<SolutionGenerationRun>('GET', `/solution-runs/${runId}`),
  solutionRuns: (projectId?: string) => apiRequest<SolutionGenerationRun[]>('GET', `/solution-runs${projectId ? `?projectId=${encodeURIComponent(projectId)}` : ''}`)
}
