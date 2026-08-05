export type ApiCode = 'OK' | 'VALIDATION_ERROR' | 'UNAUTHORIZED' | 'FORBIDDEN' | 'NOT_FOUND' | 'CONFLICT' | 'UPSTREAM_ERROR' | 'INTERNAL_ERROR'

export interface ApiResponse<T> {
  code: ApiCode
  message: string
  requestId: string
  timestamp: string
  data: T
}

export interface PageResponse<T> {
  items: T[]
  page: number
  pageSize: number
  total: number
}

export type ProjectStage = 'DRAFT' | 'REQUIREMENT_ANALYSIS' | 'PRODUCT_MATCH' | 'KNOWLEDGE_RETRIEVAL' | 'PROPOSAL_GENERATION' | 'REVIEW' | 'DELIVERED'
export type TaskStatus = 'PENDING' | 'RUNNING' | 'SUCCEEDED' | 'FAILED' | 'CANCELLED'
export type KnowledgeType = 'PRODUCT_TECH' | 'INDUSTRY_SOLUTION' | 'PROJECT_CASE' | 'TROUBLESHOOTING' | 'TEMPLATE'
export type RoleCode = 'ADMIN' | 'CONSULTANT' | 'REVIEWER' | 'USER'
export type UserStatus = 'ACTIVE' | 'DISABLED' | 'LOCKED'
export type ContentSourceType = 'KNOWLEDGE_BASE' | 'MODEL_GENERATED' | 'HYBRID' | 'PENDING_CONFIRMATION'
export type GroundingPolicy = 'BALANCED' | 'STRICT' | 'CREATIVE'

export interface UserProfile { id: string; username: string; displayName: string; email: string; role: RoleCode; roleCodes: RoleCode[]; permissions: string[] }
export interface LoginRequest { account: string; password: string }
export interface RegisterRequest { username: string; displayName: string; email: string; phone?: string; department?: string; password: string }
export interface LoginResult { accessToken: string; tokenType: 'Bearer'; expiresIn: number; user: UserProfile }

export interface SystemUser {
  id: string
  username: string
  displayName: string
  email: string
  phone?: string
  department?: string
  status: UserStatus
  roleCodes: RoleCode[]
  lastLoginAt?: string
  createdAt: string
  updatedAt: string
}

export interface RoleSummary {
  id: string
  code: RoleCode
  name: string
  description: string
  builtIn: boolean
  userCount: number
  permissionCodes: string[]
}

export interface PermissionItem { code: string; name: string; module: string; description: string }
export interface CreateUserRequest { username: string; displayName: string; email: string; phone?: string; department?: string; password: string; roleCodes: RoleCode[] }
export interface UpdateUserRequest { displayName: string; email: string; phone?: string; department?: string }
export interface UpdateUserStatusRequest { status: UserStatus }
export interface AssignUserRolesRequest { roleCodes: RoleCode[] }
export interface ResetPasswordRequest { newPassword: string }

export interface ProjectSummary {
  id: string
  name: string
  customerName: string
  industry: string
  stage: ProjectStage
  progress: number
  ownerName: string
  updatedAt: string
}

export interface ProjectDetail extends ProjectSummary {
  projectCode: string
  region: string
  background: string
  rawDemand: string
  goals: string[]
  deliveryDeadline: string
  knowledgeBaseIds: string[]
  collaboratorNames: string[]
}

export interface DashboardSummary {
  projectCount: number
  requirementTaskCount: number
  proposalCount: number
  knowledgeChunkCount: number
  runningAgentCount: number
  systemStatus: 'HEALTHY' | 'DEGRADED'
  projects: ProjectSummary[]
}

export interface RequirementDimension { name: string; score: number; description: string }
export interface RequirementAnalysis {
  taskId: string
  status: TaskStatus
  completion: number
  demandPoints: string[]
  summary: string
  dimensions: RequirementDimension[]
  recommendedProductNames: string[]
}

export interface ProductMatch {
  productId: string
  productName: string
  productFamily: string
  matchScore: number
  matchedCapabilities: string[]
  gaps: string[]
  recommended: boolean
}

export interface KnowledgeBase {
  id: string
  name: string
  knowledgeType: KnowledgeType
  description: string
  documentCount: number
  chunkCount: number
  embeddingModel: string
  chunkMethod: string
  status: 'READY' | 'PARSING' | 'ERROR'
  updatedAt: string
}

export interface RetrievalRequest {
  query: string
  knowledgeBaseIds: string[]
  topK: number
  similarityThreshold: number
  metadataFilters?: Record<string, string | string[]>
}

export interface RetrievalHit {
  id: string
  knowledgeBaseId: string
  knowledgeBaseName: string
  documentId: string
  documentName: string
  chunkId: string
  content: string
  score: number
  pageNumber?: number
  metadata: Record<string, string>
}

export interface RetrievalResult {
  taskId: string
  status: TaskStatus
  query: string
  durationMs: number
  hits: RetrievalHit[]
}

export interface ProposalSection { id: string; title: string; level: number; status: 'EMPTY' | 'GENERATED' | 'EDITED'; content: string; citationIds: string[] }
export interface ProposalDocument { id: string; projectId: string; title: string; version: number; status: TaskStatus; sections: ProposalSection[]; coverImageUrl: string; updatedAt: string }
export interface GenerateProposalRequest { retrievalTaskId: string; templateKnowledgeBaseId: string; tone: 'GOVERNMENT_FORMAL' | 'BUSINESS' | 'TECHNICAL'; outputFormats: ('DOCX' | 'PPTX' | 'PDF')[] }

export interface SolutionGenerationRequest {
  knowledgeBaseIds: string[]
  ragflowAssistantId?: string
  groundingPolicy: GroundingPolicy
  allowModelSupplement: boolean
  outputFormats: ('DOCX' | 'PPTX' | 'PDF')[]
}

export interface SolutionSectionResult {
  id: string
  title: string
  content: string
  sourceType: ContentSourceType
  evidenceCoverage: number
  citationIds: string[]
  confirmationReason?: string
}

export interface SolutionGenerationRun {
  id: string
  projectId: string
  status: TaskStatus
  stage: 'PLANNING' | 'GENERATING' | 'REVIEWING' | 'COMPLETED'
  ragflowSessionId?: string
  modelName?: string
  evidenceCoverage: number
  sections: SolutionSectionResult[]
  createdAt: string
  updatedAt: string
}

export interface ModelConfig {
  provider: 'DEEPSEEK' | 'RAGFLOW' | 'BGE_M3'
  displayName: string
  baseUrl: string
  modelName: string
  maskedApiKey?: string
  status: 'HEALTHY' | 'UNAVAILABLE' | 'UNCONFIGURED'
  lastCheckedAt?: string
}

export interface ApiErrorPayload { code: ApiCode; message: string; requestId: string; timestamp: string; fieldErrors?: Record<string, string> }
