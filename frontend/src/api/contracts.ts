export type ApiCode = 'OK' | 'VALIDATION_ERROR' | 'UNAUTHORIZED' | 'FORBIDDEN' | 'NOT_FOUND' | 'CONFLICT' | 'UPSTREAM_ERROR' | 'INTERNAL_ERROR' | string

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

export interface ProjectUpsertRequest {
  name: string
  customerName: string
  industry: string
  region?: string
  background?: string
  rawDemand: string
  goals?: string[]
  deliveryDeadline?: string
  knowledgeBaseIds?: string[]
  collaboratorNames?: string[]
}

export interface DashboardSummary {
  projectCount: number
  requirementTaskCount: number
  proposalCount: number
  knowledgeDocumentCount: number
  runningAgentCount: number
  systemStatus: 'HEALTHY' | 'DEGRADED'
  projects: ProjectSummary[]
  stageCounts: Record<ProjectStage, number>
  knowledgeBases: { id: string; name: string; documentCount: number; chunkCount: number; ready: boolean }[]
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

export interface ProductCatalogItem {
  id: string
  name: string
  category: string
  description: string
  capabilities: string[]
  officialUrl: string
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

export interface KnowledgeDocument { id: string; name: string; chunkCount: number; progress: number; status: 'PENDING' | 'PARSING' | 'READY' | 'ERROR'; createdAt: string }
export interface CreateKnowledgeBaseRequest { name: string; description?: string; knowledgeType: KnowledgeType; chunkMethod?: string }
export interface KnowledgeSyncResult { knowledgeBaseCount: number; documentCount: number; chunkCount: number; knowledgeBases: KnowledgeBase[] }

export type KnowledgeAssetType = 'DOCUMENT' | 'IMAGE' | 'VIDEO'
export type KnowledgeAssetStatus = 'PROCESSING' | 'READY' | 'ERROR'
export type AssetKnowledgeType = 'PRODUCT' | 'SOLUTION' | 'CASE' | 'TROUBLESHOOTING' | 'TEMPLATE'
export type AssetDocumentType = 'PRODUCT_MANUAL' | 'TECHNICAL_MANUAL' | 'PRODUCT_PRESENTATION' | 'INDUSTRY_SOLUTION' | 'PROJECT_SOLUTION' | 'PROJECT_CASE' | 'FAQ' | 'TEMPLATE' | 'OTHER'
export type ParserStrategy = 'GENERAL' | 'MANUAL' | 'PRESENTATION' | 'TABLE' | 'QA' | 'PICTURE'
export interface KnowledgeAssetSummary { total: number; documents: number; images: number; videos: number; pptPages: number; ready: number }
export interface KnowledgeAsset {
  id: string; ragflowDatasetId: string; ragflowDocumentId?: string; assetType: KnowledgeAssetType; documentFormat?: string
  title: string; originalFilename: string; mediaType: string; description: string; industry: string; tags: string[]
  status: KnowledgeAssetStatus; statusMessage: string; fileSize: number; pageCount: number; extractedImageCount: number
  extractedVideoCount: number; createdBy: string; createdAt: string; updatedAt: string
  knowledgeType: AssetKnowledgeType; documentType: AssetDocumentType; gisDomain: string; product: string; productVersion: string
  projectType: string; region: string; year?: number; parserStrategy?: ParserStrategy; parserReason: string; parserConfig: Record<string, unknown>
  parserOverride: boolean; parseStatus: string; ragflowStatus: string; mediaStatus: string; chunkCount: number; tokenCount: number
  errorCode?: string; errorMessage?: string
}
export interface KnowledgeAssetPage { id: string; pageNumber: number; title: string; textContent: string; imageCount: number; videoCount: number; previewUrl?: string }
export interface KnowledgeAssetMedia { id: string; pageNumber?: number; mediaKind: 'IMAGE' | 'VIDEO'; filename: string; mediaType: string; fileSize: number; sourceKind: 'EXTRACTED' | 'UPLOADED' | 'LINKED'; externalUrl?: string; contentUrl?: string }
export interface KnowledgeAssetParseTask { id: string; stage: string; status: 'PENDING'|'RUNNING'|'SUCCEEDED'|'FAILED'; progress: number; attempt: number; parserStrategy?: ParserStrategy; errorCode?: string; errorMessage?: string; updatedAt: string }
export interface KnowledgeAssetDetail { asset: KnowledgeAsset; pages: KnowledgeAssetPage[]; media: KnowledgeAssetMedia[]; parseTask?: KnowledgeAssetParseTask }
export interface KnowledgeAssetContext { assetId: string; assetTitle: string; assetType: KnowledgeAssetType; pptPage?: number; pageTitle?: string; relatedImages: KnowledgeAssetMedia[]; relatedVideos: KnowledgeAssetMedia[] }
export interface KnowledgeAssetUploadFields { datasetId: string; title?: string; description?: string; knowledgeType?: AssetKnowledgeType; documentType?: AssetDocumentType; industry?: string; gisDomain?: string; product?: string; productVersion?: string; projectType?: string; region?: string; year?: number; tags?: string }

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
  assetContext?: KnowledgeAssetContext
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
  sectionKey: string
  title: string
  purpose: string
  content: string
  sourceType: ContentSourceType
  evidenceCoverage: number
  retrievalQueries: string[]
  requiredKnowledgeTypes: string[]
  status: string
  locked: boolean
  evidence: { evidenceId: string; chunkId: string; datasetId: string; documentId: string; documentName: string; content: string; score?: number; pageNumber?: number; assetId?: string; slideNumber?: number }[]
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
  errorMessage?: string
}

export interface RetrievalEvaluationCase {
  id: string; query: string; expectedDatasetKeyword?: string; expectedDocumentKeyword?: string
  expectedSection?: string; expectedPage?: number; expectedSlide?: number; expectedKnowledgeType?: string; active: boolean
}
export interface RetrievalEvaluationRun {
  id: string; status: string; totalCases: number; completedCases: number; metrics: Record<string, string | number | null>
  errorMessage?: string; startedAt?: string; finishedAt?: string; createdAt: string
  results: { caseId: string; query: string; durationMs: number; datasetHitRank?: number; documentHitRank?: number; slideHitRank?: number; errorMessage?: string }[]
}

export interface ModelConfig {
  provider: 'DEEPSEEK' | 'RAGFLOW' | 'BGE_M3' | 'PLATFORM_LLM'
  displayName: string
  category: 'KNOWLEDGE_ENGINE' | 'PLATFORM_GENERATION'
  baseUrl: string
  modelName: string
  maskedApiKey?: string
  status: 'HEALTHY' | 'UNAVAILABLE' | 'UNCONFIGURED'
  lastCheckedAt?: string
  message?: string
  editable: boolean
  enabled: boolean
}

export interface ModelConfigUpdateRequest { baseUrl: string; modelName: string; apiKey?: string; enabled: boolean }

export interface ApiErrorPayload { code: ApiCode; message: string; requestId: string; timestamp: string; fieldErrors?: Record<string, string> }
