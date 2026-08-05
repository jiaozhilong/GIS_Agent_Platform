<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import {
  IconActivity, IconBooks, IconBrain, IconBuildingCommunity, IconDatabaseSearch,
  IconFileAnalytics, IconFolders, IconLogout, IconPresentationAnalytics, IconSettings,
  IconSparkles, IconTopologyStar3, IconUserCircle, IconUsersGroup
} from '@tabler/icons-vue'

defineProps<{ title?: string; subtitle?: string; projectMode?: boolean }>()
const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const projectId = computed(() => String(route.params.id || 'prj-001'))
const roleNames = { ADMIN: '系统管理员', CONSULTANT: '解决方案顾问', REVIEWER: '方案审核员', USER: '普通用户' } as const
const displayName = computed(() => auth.user?.displayName || auth.user?.username || '平台用户')
const displayRole = computed(() => auth.user ? roleNames[auth.user.role] : '正在加载用户信息')
const items = computed(() => [
  { label: '总览看板', icon: IconActivity, to: '/dashboard', match: '/dashboard' },
  { label: '项目管理', icon: IconFolders, to: '/projects', match: '/projects' },
  { label: '需求分析', icon: IconBrain, to: `/projects/${projectId.value}/requirements`, match: '/requirements' },
  { label: '产品匹配', icon: IconTopologyStar3, to: `/projects/${projectId.value}/products`, match: '/products' },
  { label: '知识检索', icon: IconDatabaseSearch, to: `/projects/${projectId.value}/retrieval`, match: '/retrieval' },
  { label: '方案生成', icon: IconPresentationAnalytics, to: `/projects/${projectId.value}/proposal`, match: '/proposal' },
  { label: '知识库管理', icon: IconBooks, to: '/knowledge', match: '/knowledge' },
  { label: '生成记录', icon: IconFileAnalytics, to: '/generations', match: '/generations' },
  { label: '模型配置', icon: IconSettings, to: '/settings/models', match: '/settings/models' },
  { label: '用户与权限', icon: IconUsersGroup, to: '/settings/users', match: '/settings/users' }
])
const active = (match: string) => match === '/projects' ? route.path === '/projects' || /^\/projects\/[^/]+$/.test(route.path) : route.path.includes(match)
const logout = () => { auth.logout(); router.push('/login') }
onMounted(auth.hydrate)
</script>

<template>
  <div class="shell">
    <aside class="sidebar">
      <RouterLink to="/dashboard" class="brand">
        <span class="brand-mark"><IconSparkles :size="19" /></span>
        <span><b>GIS Agent</b><small>Platform</small></span>
      </RouterLink>
      <nav>
        <RouterLink v-for="item in items" :key="item.label" :to="item.to" :class="['nav-item', { active: active(item.match) }]">
          <component :is="item.icon" :size="18" stroke-width="1.7" />
          <span>{{ item.label }}</span>
        </RouterLink>
      </nav>
      <div class="sidebar-footer">
        <div class="user"><IconUserCircle :size="31" /><span><b>{{ displayName }}</b><small>{{ displayRole }}</small></span></div>
        <button class="logout" aria-label="退出登录" @click="logout"><IconLogout :size="18" /></button>
      </div>
    </aside>
    <main class="main">
      <header class="topbar">
        <div>
          <h1>{{ title || 'GIS Agent Platform' }}</h1>
          <p v-if="subtitle">{{ subtitle }}</p>
        </div>
        <div class="system-health">
          <span><i class="status-dot" /> RAGFlow</span>
          <span><i class="status-dot" /> DeepSeek</span>
          <span><i class="status-dot" /> BGE-M3</span>
        </div>
      </header>
      <section class="content"><slot /></section>
    </main>
  </div>
</template>

<style scoped>
.shell { width: 100%; height: 100%; display: grid; grid-template-columns: 210px minmax(0, 1fr); }
.sidebar { background: rgba(2, 10, 17, .97); border-right: 1px solid var(--line); display: flex; flex-direction: column; min-height: 0; }
.brand { height: 72px; display: flex; align-items: center; gap: 11px; padding: 0 18px; border-bottom: 1px solid var(--line); }
.brand-mark { width: 34px; height: 34px; display: grid; place-items: center; background: linear-gradient(145deg, #19d7dd, #156cff); clip-path: polygon(50% 0, 93% 25%, 93% 75%, 50% 100%, 7% 75%, 7% 25%); }
.brand span:last-child { display: grid; line-height: 1.05; }
.brand b { font-size: 15px; }.brand small { color: var(--text-3); margin-top: 5px; letter-spacing: .8px; }
nav { padding: 14px 10px; display: grid; gap: 4px; overflow-y: auto; }
.nav-item { min-height: 42px; border: 1px solid transparent; border-radius: 6px; padding: 0 12px; display: flex; align-items: center; gap: 11px; color: var(--text-2); transition: .16s ease; }
.nav-item:hover { color: var(--text-1); background: rgba(31, 92, 132, .12); }
.nav-item.active { color: #dff5ff; border-color: rgba(36, 129, 206, .24); background: linear-gradient(90deg, rgba(23, 113, 224, .26), rgba(20, 95, 162, .07)); box-shadow: inset 2px 0 var(--primary); }
.sidebar-footer { margin-top: auto; height: 70px; border-top: 1px solid var(--line); display: flex; align-items: center; justify-content: space-between; padding: 0 14px; }
.user { display: flex; align-items: center; gap: 8px; }.user span { display: grid; }.user small { color: var(--text-3); margin-top: 3px; font-size: 11px; }
.logout { border: 0; color: var(--text-3); background: transparent; padding: 7px; }.logout:hover { color: var(--danger); }
.main { min-width: 0; min-height: 0; display: grid; grid-template-rows: 72px minmax(0, 1fr); }
.topbar { display: flex; align-items: center; justify-content: space-between; padding: 0 22px; background: rgba(3, 11, 18, .87); border-bottom: 1px solid var(--line); }
.topbar h1 { margin: 0; font-size: 17px; letter-spacing: .1px; }.topbar p { margin: 5px 0 0; color: var(--text-3); font-size: 12px; }
.system-health { display: flex; gap: 8px; }.system-health span { padding: 7px 10px; border: 1px solid var(--line); border-radius: 5px; color: var(--text-2); display: flex; align-items: center; gap: 7px; font-size: 12px; }
.content { min-height: 0; overflow: auto; padding: 16px; }
@media (max-width: 1080px) { .shell { grid-template-columns: 74px minmax(0, 1fr); }.brand span:last-child, .nav-item span, .user span, .logout { display: none; }.brand { justify-content: center; padding: 0; }.nav-item { justify-content: center; padding: 0; }.sidebar-footer { justify-content: center; }.system-health { display: none; } }
</style>
