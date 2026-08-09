<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { IconPlus, IconSearch, IconX } from '@tabler/icons-vue'
import AppShell from '@/components/layout/AppShell.vue'
import { api } from '@/api/services'
import type { KnowledgeBase, ProjectSummary, ProjectUpsertRequest } from '@/api/contracts'

const router = useRouter()
const projects = ref<ProjectSummary[]>([])
const bases = ref<KnowledgeBase[]>([])
const keyword = ref('')
const showCreate = ref(false)
const saving = ref(false)
const error = ref('')
const form = reactive<ProjectUpsertRequest>({
  name: '自然资源一张图智能化升级项目', customerName: '', industry: '自然资源', region: '',
  background: '', rawDemand: '', goals: [], knowledgeBaseIds: [], collaboratorNames: [],
  deliveryDeadline: ''
})

onMounted(async () => {
  try {
    const [projectItems, knowledgeItems] = await Promise.all([api.projects(), api.knowledgeBases()])
    projects.value = projectItems
    bases.value = knowledgeItems
    form.knowledgeBaseIds = knowledgeItems.filter(item => item.status === 'READY').map(item => item.id)
  } catch (e) { error.value = e instanceof Error ? e.message : '项目加载失败' }
})

const filtered = computed(() => projects.value.filter((item) => `${item.name}${item.customerName}${item.industry}`.includes(keyword.value)))

const create = async () => {
  error.value = ''
  if (!form.name.trim() || !form.customerName.trim() || !form.rawDemand.trim()) {
    error.value = '请填写项目名称、客户名称和原始需求'
    return
  }
  saving.value = true
  try {
    const project = await api.createProject({ ...form, knowledgeBaseIds: [...(form.knowledgeBaseIds || [])] })
    showCreate.value = false
    await router.push(`/projects/${project.id}`)
  } catch (e) { error.value = e instanceof Error ? e.message : '项目创建失败' }
  finally { saving.value = false }
}
</script>

<template>
  <AppShell title="项目管理" subtitle="统一管理需求、知识证据、生成过程与交付版本">
    <div class="projects-toolbar">
      <label class="search"><IconSearch :size="17" /><input v-model="keyword" placeholder="搜索项目、客户或行业" /></label>
      <button class="primary-button" @click="showCreate = true"><IconPlus :size="17" /> 新建项目</button>
    </div>
    <p v-if="error && !showCreate" class="page-error">{{ error }}</p>
    <div class="project-table tech-panel">
      <div class="table-head"><span>项目名称</span><span>行业</span><span>当前阶段</span><span>进度</span><span>负责人</span><span>更新时间</span></div>
      <RouterLink v-for="project in filtered" :key="project.id" :to="`/projects/${project.id}`" class="table-row">
        <span><b>{{ project.name }}</b><small>{{ project.customerName }}</small></span><span>{{ project.industry }}</span>
        <span class="stage-pill">{{ project.stage }}</span><span class="progress-cell"><div class="progress-track"><div class="progress-value" :style="{ width: `${project.progress}%` }" /></div><em>{{ project.progress }}%</em></span>
        <span>{{ project.ownerName }}</span><time>{{ project.updatedAt.slice(0, 16).replace('T', ' ') }}</time>
      </RouterLink>
      <div v-if="!filtered.length" class="empty">暂无项目，请新建一个项目开始完整流程。</div>
    </div>

    <div v-if="showCreate" class="modal-backdrop">
      <form class="create-dialog tech-panel" @submit.prevent="create">
        <header><div><h2>新建解决方案项目</h2><p>项目将保存到 PostgreSQL，并用于后续真实 AI 流程。</p></div><button type="button" @click="showCreate = false"><IconX /></button></header>
        <div class="form-grid">
          <label>项目名称 *<input v-model="form.name" class="tech-input" /></label>
          <label>客户名称 *<input v-model="form.customerName" class="tech-input" placeholder="请输入客户单位" /></label>
          <label>所属行业 *<select v-model="form.industry" class="tech-select"><option>自然资源</option><option>水利</option><option>智慧城市</option><option>交通</option><option>应急</option></select></label>
          <label>所在区域<input v-model="form.region" class="tech-input" placeholder="例如 北京市" /></label>
          <label class="wide">项目背景<textarea v-model="form.background" class="tech-textarea" placeholder="说明现有系统、数据和建设背景" /></label>
          <label class="wide">原始需求 *<textarea v-model="form.rawDemand" class="tech-textarea" placeholder="填写需要建设的业务和技术能力" /></label>
          <fieldset class="wide"><legend>关联 RAGFlow 知识库</legend><label v-for="base in bases" :key="base.id" class="check"><input v-model="form.knowledgeBaseIds" type="checkbox" :value="base.id" />{{ base.name }}（{{ base.chunkCount }} 切片）</label></fieldset>
        </div>
        <p v-if="error" class="dialog-error">{{ error }}</p>
        <footer><button type="button" class="ghost-button" @click="showCreate = false">取消</button><button class="primary-button" :disabled="saving">{{ saving ? '正在创建...' : '创建项目' }}</button></footer>
      </form>
    </div>
  </AppShell>
</template>

<style scoped>
.projects-toolbar { display:flex;justify-content:space-between;margin-bottom:12px }.search{width:360px;height:40px;display:flex;align-items:center;gap:9px;border:1px solid var(--line);border-radius:6px;padding:0 12px;color:var(--text-3);background:rgba(3,13,22,.7)}.search input{flex:1;background:none;border:0;outline:0;color:var(--text-1)}
.project-table{min-width:850px;overflow:hidden}.table-head,.table-row{display:grid;grid-template-columns:minmax(260px,1.8fr) 90px 150px 150px 90px 140px;align-items:center;gap:12px;padding:0 16px}.table-head{height:44px;color:var(--text-3);font-size:12px;background:rgba(17,50,73,.16)}.table-row{min-height:64px;border-top:1px solid var(--line);color:var(--text-2)}.table-row:hover{background:rgba(23,83,126,.09)}.table-row>span:first-child{display:grid;gap:5px}.table-row b{color:var(--text-1)}.table-row small,time{color:var(--text-3);font-size:11px}.stage-pill{color:var(--primary-2)}.progress-cell{display:grid;grid-template-columns:1fr 36px;align-items:center;gap:7px}.progress-cell em{font-style:normal;color:var(--text-3);font-size:11px}.empty{padding:50px;text-align:center;color:var(--text-3)}
.modal-backdrop{position:fixed;inset:0;z-index:30;background:rgba(0,5,10,.76);display:grid;place-items:center;backdrop-filter:blur(5px)}.create-dialog{width:min(760px,calc(100vw - 32px));max-height:calc(100vh - 40px);overflow:auto;padding:0}.create-dialog header,.create-dialog footer{padding:16px 20px;display:flex;align-items:center;justify-content:space-between;border-bottom:1px solid var(--line)}.create-dialog footer{border:0;border-top:1px solid var(--line);justify-content:flex-end;gap:10px}.create-dialog h2{margin:0}.create-dialog p{margin:5px 0 0;color:var(--text-3);font-size:11px}.create-dialog header button{border:0;background:transparent;color:var(--text-3)}.form-grid{padding:18px 20px;display:grid;grid-template-columns:1fr 1fr;gap:14px}.form-grid>label{display:grid;gap:7px;color:var(--text-2)}.wide{grid-column:1/-1}.form-grid textarea{min-height:78px}fieldset{border:1px solid var(--line);border-radius:6px;padding:12px;display:grid;gap:8px}legend{padding:0 7px;color:var(--text-2)}.check{display:flex!important;grid-template-columns:18px 1fr!important;align-items:center;gap:8px!important;color:var(--text-2)}.page-error,.dialog-error{color:var(--danger)}.page-error{padding:10px;border:1px solid rgba(255,90,110,.3)}.dialog-error{padding:0 20px 10px!important}
</style>
