<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { IconPlus, IconSearch } from '@tabler/icons-vue'
import AppShell from '@/components/layout/AppShell.vue'
import { api } from '@/api/services'
import type { ProjectSummary } from '@/api/contracts'

const projects = ref<ProjectSummary[]>([])
const keyword = ref('')
const showCreate = ref(false)
onMounted(async () => { projects.value = await api.projects() })
const filtered = computed(() => projects.value.filter((item) => `${item.name}${item.customerName}${item.industry}`.includes(keyword.value)))
const createMock = () => { projects.value.unshift({ id: `prj-${Date.now()}`, name: '新建GIS解决方案项目', customerName: '待补充客户', industry: '待分类', stage: 'DRAFT', progress: 5, ownerName: '张文博', updatedAt: new Date().toISOString() }); showCreate.value = false }
</script>
<template>
  <AppShell title="项目管理" subtitle="统一管理需求、知识证据、生成过程与交付版本">
    <div class="projects-toolbar"><label class="search"><IconSearch :size="17" /><input v-model="keyword" placeholder="搜索项目、客户或行业" /></label><button class="primary-button" @click="showCreate = true"><IconPlus :size="17" /> 新建项目</button></div>
    <div class="project-table tech-panel">
      <div class="table-head"><span>项目名称</span><span>行业</span><span>当前阶段</span><span>进度</span><span>负责人</span><span>更新时间</span></div>
      <RouterLink v-for="project in filtered" :key="project.id" :to="`/projects/${project.id}`" class="table-row"><span><b>{{ project.name }}</b><small>{{ project.customerName }}</small></span><span>{{ project.industry }}</span><span class="stage-pill">{{ project.stage }}</span><span class="progress-cell"><div class="progress-track"><div class="progress-value" :style="{ width: `${project.progress}%` }" /></div><em>{{ project.progress }}%</em></span><span>{{ project.ownerName }}</span><time>{{ project.updatedAt.slice(0, 16).replace('T', ' ') }}</time></RouterLink>
    </div>
    <div v-if="showCreate" class="modal-backdrop" @click.self="showCreate = false"><form class="create-dialog tech-panel" @submit.prevent="createMock"><h2>新建解决方案项目</h2><label>项目名称<input class="tech-input" value="新建GIS解决方案项目" /></label><label>客户名称<input class="tech-input" placeholder="请输入客户单位" /></label><label>所属行业<select class="tech-select"><option>自然资源</option><option>水利</option><option>智慧城市</option><option>交通</option></select></label><div><button type="button" class="ghost-button" @click="showCreate = false">取消</button><button class="primary-button">创建项目</button></div></form></div>
  </AppShell>
</template>
<style scoped>
.projects-toolbar { display: flex; justify-content: space-between; margin-bottom: 12px; }.search { width: 360px; height: 40px; display: flex; align-items: center; gap: 9px; border: 1px solid var(--line); border-radius: 6px; padding: 0 12px; color: var(--text-3); background: rgba(3,13,22,.7); }.search input { flex: 1; background: none; border: 0; outline: 0; color: var(--text-1); }
.project-table { min-width: 850px; overflow: hidden; }.table-head, .table-row { display: grid; grid-template-columns: minmax(260px, 1.8fr) 90px 150px 150px 90px 140px; align-items: center; gap: 12px; padding: 0 16px; }.table-head { height: 44px; color: var(--text-3); font-size: 12px; background: rgba(17,50,73,.16); }.table-row { min-height: 64px; border-top: 1px solid var(--line); color: var(--text-2); }.table-row:hover { background: rgba(23,83,126,.09); }.table-row > span:first-child { display: grid; gap: 5px; }.table-row b { color: var(--text-1); }.table-row small, time { color: var(--text-3); font-size: 11px; }.stage-pill { color: var(--primary-2); }.progress-cell { display: grid; grid-template-columns: 1fr 36px; align-items: center; gap: 7px; }.progress-cell em { font-style: normal; color: var(--text-3); font-size: 11px; }
.modal-backdrop { position: fixed; inset: 0; z-index: 20; background: rgba(0,5,10,.7); display: grid; place-items: center; backdrop-filter: blur(5px); }.create-dialog { width: 460px; padding: 24px; display: grid; gap: 16px; }.create-dialog h2 { margin: 0 0 4px; }.create-dialog label { display: grid; gap: 7px; color: var(--text-2); }.create-dialog > div { display: flex; justify-content: flex-end; gap: 10px; margin-top: 5px; }
</style>
