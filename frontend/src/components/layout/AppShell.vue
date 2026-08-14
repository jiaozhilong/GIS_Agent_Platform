<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import {
  IconActivity, IconArchive, IconBooks, IconFileAnalytics, IconFolders, IconLogout, IconRadar,
  IconSearch, IconSettings, IconSparkles, IconUserCircle, IconUsersGroup, IconChartDots
} from '@tabler/icons-vue'

defineProps<{ title?: string; subtitle?: string }>()
const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const roleNames = { ADMIN: '系统管理员', CONSULTANT: '解决方案顾问', REVIEWER: '方案审核员', USER: '普通用户' } as const
const displayName = computed(() => auth.user?.displayName || auth.user?.username || '平台用户')
const displayRole = computed(() => auth.user ? roleNames[auth.user.role] : '正在加载用户信息')
const groups = [
  { label: 'AGENT WORKSPACE', items: [
    { label: '智能中枢', icon: IconActivity, to: '/dashboard', active: () => route.path === '/dashboard' },
    { label: '项目工作台', icon: IconFolders, to: '/projects', active: () => route.path.startsWith('/projects') },
  ] },
  { label: 'KNOWLEDGE MESH', items: [
    { label: '知识探索', icon: IconSearch, to: '/search', active: () => route.path === '/search' },
    { label: '知识资产', icon: IconArchive, to: '/assets', active: () => route.path === '/assets' },
    { label: 'RAGFlow 配置', icon: IconBooks, to: '/knowledge', active: () => route.path === '/knowledge' },
    { label: '检索评测', icon: IconChartDots, to: '/evaluations/retrieval', active: () => route.path === '/evaluations/retrieval' },
  ] },
  { label: 'OPERATIONS', items: [
    { label: '生成记录', icon: IconFileAnalytics, to: '/generations', active: () => route.path === '/generations' },
    { label: '模型路由', icon: IconSettings, to: '/settings/models', active: () => route.path === '/settings/models' },
    { label: '用户权限', icon: IconUsersGroup, to: '/settings/users', active: () => route.path === '/settings/users' },
  ] },
]
const logout = () => { auth.logout(); router.push('/login') }
onMounted(() => auth.hydrate())
</script>

<template>
  <div class="shell">
    <aside class="sidebar">
      <RouterLink to="/dashboard" class="brand">
        <span class="brand-mark"><IconSparkles :size="19" /></span>
        <span><b>GIS AGENT</b><small>INTELLIGENCE OS</small></span>
      </RouterLink>
      <div class="agent-core"><span class="core-orbit"><i/><i/><IconRadar :size="19"/></span><div><b>Agent Mesh</b><small><i/> 3 个引擎在线</small></div></div>
      <nav>
        <section v-for="group in groups" :key="group.label"><label>{{ group.label }}</label><RouterLink v-for="item in group.items" :key="item.label" :to="item.to" :class="['nav-item', { active: item.active() }]">
          <component :is="item.icon" :size="18" stroke-width="1.7" /><span>{{ item.label }}</span><i v-if="item.active()"/>
        </RouterLink></section>
      </nav>
      <div class="sidebar-footer">
        <div class="user"><IconUserCircle :size="31" /><span><b>{{ displayName }}</b><small>{{ displayRole }}</small></span></div>
        <button class="logout" aria-label="退出登录" @click="logout"><IconLogout :size="18" /></button>
      </div>
    </aside>
    <main class="main">
      <header class="topbar">
        <div class="page-title"><span class="breadcrumb">GIS AGENT / {{ route.path.split('/')[1]?.toUpperCase() || 'HOME' }}</span><h1>{{ title || 'GIS Agent Platform' }}</h1><p v-if="subtitle">{{ subtitle }}</p></div>
        <div class="top-actions"><RouterLink to="/search" class="global-search"><IconSearch :size="15"/>探索知识库 <kbd>⌘ K</kbd></RouterLink><div class="system-health"><span title="RAGFlow"><i />R</span><span title="平台语言模型"><i />L</span><span title="BGE-M3"><i />E</span></div></div>
      </header>
      <section class="content"><slot /></section>
    </main>
  </div>
</template>

<style scoped>
.shell{width:100%;height:100%;display:grid;grid-template-columns:218px minmax(0,1fr);background:radial-gradient(circle at 72% -15%,rgba(20,131,217,.09),transparent 34%)}.sidebar{position:relative;background:linear-gradient(180deg,rgba(2,9,16,.99),rgba(3,12,20,.97));border-right:1px solid rgba(64,167,226,.18);display:flex;flex-direction:column;min-height:0}.sidebar:after{content:'';position:absolute;right:-1px;top:0;width:1px;height:28%;background:linear-gradient(transparent,var(--cyan),transparent);opacity:.48}.brand{height:72px;display:flex;align-items:center;gap:11px;padding:0 17px;border-bottom:1px solid var(--line)}.brand-mark{position:relative;width:36px;height:36px;display:grid;place-items:center;color:white;background:linear-gradient(145deg,#19d7dd,#156cff);clip-path:polygon(50% 0,93% 25%,93% 75%,50% 100%,7% 75%,7% 25%);filter:drop-shadow(0 0 12px rgba(33,193,255,.28))}.brand span:last-child{display:grid;line-height:1}.brand b{font-size:14px;letter-spacing:.7px}.brand small{color:var(--text-3);margin-top:6px;font-size:8px;letter-spacing:1.5px}.agent-core{margin:13px 11px 2px;padding:11px;border:1px solid rgba(48,153,218,.18);border-radius:9px;background:linear-gradient(110deg,rgba(17,72,105,.22),rgba(7,27,42,.25));display:flex;align-items:center;gap:10px}.core-orbit{position:relative;width:34px;height:34px;display:grid;place-items:center;color:var(--cyan);border:1px solid rgba(53,203,239,.28);border-radius:50%}.core-orbit>i{position:absolute;inset:3px;border:1px dashed rgba(51,210,245,.26);border-radius:50%;animation:spin 8s linear infinite}.core-orbit>i:nth-child(2){inset:-3px;animation-direction:reverse;animation-duration:12s}.agent-core>div{display:grid;gap:4px}.agent-core b{font-size:11px}.agent-core small{color:var(--text-3);font-size:9px}.agent-core small i{display:inline-block;width:6px;height:6px;border-radius:50%;background:var(--success);box-shadow:0 0 8px var(--success)}nav{padding:10px;display:grid;gap:14px;overflow-y:auto}nav section{display:grid;gap:3px}nav section>label{padding:0 10px 5px;color:#405a6d;font-size:8px;letter-spacing:1.4px}.nav-item{position:relative;min-height:40px;border:1px solid transparent;border-radius:7px;padding:0 11px;display:flex;align-items:center;gap:10px;color:var(--text-2);transition:.18s ease}.nav-item:hover{color:var(--text-1);background:rgba(31,92,132,.12)}.nav-item.active{color:#e5f9ff;border-color:rgba(36,159,220,.25);background:linear-gradient(90deg,rgba(20,132,218,.24),rgba(18,89,140,.04));box-shadow:inset 2px 0 var(--cyan)}.nav-item.active>i{margin-left:auto;width:5px;height:5px;border-radius:50%;background:var(--cyan);box-shadow:0 0 9px var(--cyan)}.sidebar-footer{margin-top:auto;height:70px;border-top:1px solid var(--line);display:flex;align-items:center;justify-content:space-between;padding:0 14px}.user{display:flex;align-items:center;gap:8px}.user span{display:grid}.user small{color:var(--text-3);margin-top:3px;font-size:10px}.logout{border:0;color:var(--text-3);background:transparent;padding:7px}.logout:hover{color:var(--danger)}.main{min-width:0;min-height:0;display:grid;grid-template-rows:78px minmax(0,1fr)}.topbar{display:flex;align-items:center;justify-content:space-between;padding:0 22px;background:rgba(2,10,17,.76);border-bottom:1px solid var(--line);backdrop-filter:blur(15px)}.breadcrumb{display:block;color:#42647b;font-size:8px;letter-spacing:1.4px;margin-bottom:4px}.topbar h1{margin:0;font-size:17px;letter-spacing:.1px}.topbar p{margin:4px 0 0;color:var(--text-3);font-size:11px}.top-actions{display:flex;align-items:center;gap:10px}.global-search{height:34px;padding:0 8px 0 11px;display:flex;align-items:center;gap:8px;border:1px solid var(--line);border-radius:7px;background:rgba(12,37,54,.3);color:var(--text-2);font-size:10px}.global-search kbd{padding:3px 6px;border:1px solid var(--line);border-radius:4px;color:var(--text-3);font:9px var(--font-ui)}.system-health{display:flex;gap:5px}.system-health span{width:31px;height:31px;border:1px solid var(--line);border-radius:50%;color:var(--text-3);display:grid;place-items:center;font-size:9px;position:relative}.system-health i{position:absolute;right:0;top:1px;width:6px;height:6px;border-radius:50%;background:var(--success);box-shadow:0 0 7px var(--success)}.content{min-height:0;overflow:auto;padding:16px}@keyframes spin{to{transform:rotate(360deg)}}@media(max-width:1080px){.shell{grid-template-columns:74px minmax(0,1fr)}.brand span:last-child,.nav-item span,.user span,.logout,.agent-core>div,nav section>label{display:none}.brand{justify-content:center;padding:0}.agent-core{justify-content:center;margin:10px}.nav-item{justify-content:center;padding:0}.sidebar-footer{justify-content:center}.global-search{display:none}}
</style>
