import { createRouter, createWebHistory } from 'vue-router'
const LoginPage = () => import('@/pages/LoginPage.vue')
const DashboardPage = () => import('@/pages/DashboardPage.vue')
const ProjectsPage = () => import('@/pages/ProjectsPage.vue')
const ProjectOverviewPage = () => import('@/pages/ProjectOverviewPage.vue')
const RequirementAnalysisPage = () => import('@/pages/RequirementAnalysisPage.vue')
const ProductMatchPage = () => import('@/pages/ProductMatchPage.vue')
const RetrievalPage = () => import('@/pages/RetrievalPage.vue')
const ProposalPage = () => import('@/pages/ProposalPage.vue')
const KnowledgePage = () => import('@/pages/KnowledgePage.vue')
const KnowledgeSearchPage = () => import('@/pages/KnowledgeSearchPage.vue')
const KnowledgeAssetCenterPage = () => import('@/pages/KnowledgeAssetCenterPage.vue')
const GenerationRecordsPage = () => import('@/pages/GenerationRecordsPage.vue')
const ModelSettingsPage = () => import('@/pages/ModelSettingsPage.vue')
const UserManagementPage = () => import('@/pages/UserManagementPage.vue')
const RetrievalEvaluationPage = () => import('@/pages/RetrievalEvaluationPage.vue')

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', redirect: '/login' },
    { path: '/login', component: LoginPage, meta: { public: true } },
    { path: '/dashboard', component: DashboardPage },
    { path: '/projects', component: ProjectsPage },
    { path: '/projects/:id', component: ProjectOverviewPage },
    { path: '/projects/:id/requirements', component: RequirementAnalysisPage },
    { path: '/projects/:id/products', component: ProductMatchPage },
    { path: '/projects/:id/retrieval', component: RetrievalPage },
    { path: '/projects/:id/proposal', component: ProposalPage },
    { path: '/knowledge', component: KnowledgePage },
    { path: '/assets', component: KnowledgeAssetCenterPage },
    { path: '/search', component: KnowledgeSearchPage },
    { path: '/evaluations/retrieval', component: RetrievalEvaluationPage },
    { path: '/generations', component: GenerationRecordsPage },
    { path: '/settings/models', component: ModelSettingsPage },
    { path: '/settings/users', component: UserManagementPage }
  ]
})

router.beforeEach((to) => {
  if (to.meta.public) return true
  if (localStorage.getItem('gis-agent-token')) return true
  return '/login'
})

export default router
