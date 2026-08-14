<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { IconEdit, IconPlus, IconSearch, IconTrash, IconX } from '@tabler/icons-vue'
import AppShell from '@/components/layout/AppShell.vue'
import { api } from '@/api/services'
import { useAuthStore } from '@/stores/auth'
import type { KnowledgeBase, ProjectDetail, ProjectSummary, ProjectUpsertRequest } from '@/api/contracts'

const router = useRouter()
const auth = useAuthStore()
const projects = ref<ProjectSummary[]>([])
const bases = ref<KnowledgeBase[]>([])
const keyword = ref('')
const dialogOpen = ref(false)
const editing = ref<ProjectDetail | null>(null)
const pendingDelete = ref<ProjectSummary | null>(null)
const saving = ref(false)
const deleting = ref(false)
const error = ref('')
const canManage = computed(() => auth.user?.permissions.includes('project:manage') ?? false)
const emptyForm = (): ProjectUpsertRequest => ({ name: '', customerName: '', industry: '自然资源', region: '', background: '', rawDemand: '', goals: [], knowledgeBaseIds: [], collaboratorNames: [], deliveryDeadline: '' })
const form = reactive<ProjectUpsertRequest>(emptyForm())

const load = async () => {
  const [projectItems, knowledgeItems] = await Promise.all([api.projects(), api.knowledgeBases()])
  projects.value = projectItems
  bases.value = knowledgeItems
}
onMounted(async () => { try { await load() } catch (e) { error.value = e instanceof Error ? e.message : '项目加载失败' } })
const filtered = computed(() => projects.value.filter(item => `${item.name}${item.customerName}${item.industry}`.toLowerCase().includes(keyword.value.trim().toLowerCase())))
const setForm = (value?: ProjectDetail) => Object.assign(form, value ? {
  name: value.name, customerName: value.customerName, industry: value.industry, region: value.region,
  background: value.background, rawDemand: value.rawDemand, goals: [...value.goals], deliveryDeadline: value.deliveryDeadline,
  knowledgeBaseIds: [...value.knowledgeBaseIds], collaboratorNames: [...value.collaboratorNames],
} : { ...emptyForm(), name: '自然资源一张图智能化升级项目', knowledgeBaseIds: bases.value.filter(item => item.status === 'READY').map(item => item.id) })
const openCreate = () => { editing.value = null; setForm(); error.value = ''; dialogOpen.value = true }
const openEdit = async (project: ProjectSummary) => {
  error.value = ''
  try { editing.value = await api.project(project.id); setForm(editing.value); dialogOpen.value = true }
  catch (e) { error.value = e instanceof Error ? e.message : '项目详情加载失败' }
}
const save = async () => {
  error.value = ''
  if (!form.name.trim() || !form.customerName.trim() || !form.rawDemand.trim()) { error.value = '请填写项目名称、客户名称和原始需求'; return }
  saving.value = true
  try {
    const payload = { ...form, knowledgeBaseIds: [...(form.knowledgeBaseIds || [])], goals: [...(form.goals || [])], collaboratorNames: [...(form.collaboratorNames || [])] }
    if (editing.value) {
      await api.updateProject(editing.value.id, payload)
      dialogOpen.value = false
      await load()
    } else {
      const project = await api.createProject(payload)
      dialogOpen.value = false
      await router.push(`/projects/${project.id}`)
    }
  } catch (e) { error.value = e instanceof Error ? e.message : '项目保存失败' }
  finally { saving.value = false }
}
const remove = async () => {
  if (!pendingDelete.value) return
  deleting.value = true; error.value = ''
  try {
    const id = pendingDelete.value.id
    await api.deleteProject(id)
    if (localStorage.getItem('gis-agent-project-id') === id) localStorage.removeItem('gis-agent-project-id')
    pendingDelete.value = null
    await load()
  } catch (e) { error.value = e instanceof Error ? e.message : '项目删除失败' }
  finally { deleting.value = false }
}
</script>

<template>
  <AppShell title="项目工作台" subtitle="从项目入口统一编排需求 Agent、产品 Agent、知识检索与方案生成">
    <section class="workspace-head">
      <div><span>PROJECT ORCHESTRATION</span><h2>解决方案项目空间</h2><p>每个项目都是一条可追溯的 Agent 工作流，点击项目进入完整工作台。</p></div>
      <div class="workspace-stat"><b>{{ projects.length }}</b><span>项目空间</span></div><div class="workspace-stat"><b>{{ projects.filter(item => item.progress < 100).length }}</b><span>运行中</span></div>
    </section>
    <div class="projects-toolbar">
      <label class="search"><IconSearch :size="17" /><input v-model="keyword" placeholder="搜索项目、客户或行业" /></label>
      <button v-if="canManage" class="primary-button" @click="openCreate"><IconPlus :size="17" /> 新建项目</button>
    </div>
    <p v-if="error && !dialogOpen" class="page-error">{{ error }}</p>
    <div class="project-table tech-panel">
      <div class="table-head"><span>项目名称</span><span>行业</span><span>当前阶段</span><span>Agent 进度</span><span>负责人</span><span>更新时间</span><span>管理</span></div>
      <div v-for="project in filtered" :key="project.id" class="table-row">
        <RouterLink :to="`/projects/${project.id}`" class="project-name"><span class="project-node"><i/></span><span><b>{{ project.name }}</b><small>{{ project.customerName }}</small></span></RouterLink>
        <span>{{ project.industry }}</span><span class="stage-pill">{{ project.stage }}</span><span class="progress-cell"><div class="progress-track"><div class="progress-value" :style="{ width: `${project.progress}%` }" /></div><em>{{ project.progress }}%</em></span>
        <span>{{ project.ownerName }}</span><time>{{ project.updatedAt.slice(0, 16).replace('T', ' ') }}</time>
        <span class="row-actions"><button v-if="canManage" title="编辑项目" @click="openEdit(project)"><IconEdit :size="16"/></button><button v-if="canManage" title="删除项目" class="danger" @click="pendingDelete = project"><IconTrash :size="16"/></button></span>
      </div>
      <div v-if="!filtered.length" class="empty">暂无匹配项目，请新建项目启动 Agent 工作流。</div>
    </div>

    <div v-if="dialogOpen" class="modal-backdrop">
      <form class="project-dialog tech-panel" @submit.prevent="save">
        <header><div><span class="eyebrow">PROJECT PROFILE</span><h2>{{ editing ? '编辑项目配置' : '创建解决方案项目' }}</h2><p>项目配置将作为全部 Agent 的共享上下文。</p></div><button type="button" @click="dialogOpen = false"><IconX /></button></header>
        <div class="form-grid">
          <label>项目名称 *<input v-model="form.name" class="tech-input" /></label><label>客户名称 *<input v-model="form.customerName" class="tech-input" placeholder="请输入客户单位" /></label>
          <label>所属行业 *<select v-model="form.industry" class="tech-select"><option>自然资源</option><option>水利</option><option>智慧城市</option><option>交通</option><option>应急</option></select></label><label>所在区域<input v-model="form.region" class="tech-input" placeholder="例如 北京市" /></label>
          <label>交付日期<input v-model="form.deliveryDeadline" class="tech-input" type="date"/></label><label>协作成员<input :value="form.collaboratorNames?.join('、')" class="tech-input" placeholder="使用顿号分隔" @input="form.collaboratorNames = ($event.target as HTMLInputElement).value.split('、').filter(Boolean)"/></label>
          <label class="wide">项目背景<textarea v-model="form.background" class="tech-textarea" placeholder="说明现有系统、数据和建设背景" /></label><label class="wide">原始需求 *<textarea v-model="form.rawDemand" class="tech-textarea" placeholder="填写需要建设的业务和技术能力" /></label>
          <fieldset class="wide"><legend>关联 RAGFlow 知识库</legend><label v-for="base in bases" :key="base.id" class="check"><input v-model="form.knowledgeBaseIds" type="checkbox" :value="base.id" />{{ base.name }}（{{ base.documentCount }} 份文档）</label></fieldset>
        </div>
        <p v-if="error" class="dialog-error">{{ error }}</p><footer><button type="button" class="ghost-button" @click="dialogOpen = false">取消</button><button class="primary-button" :disabled="saving">{{ saving ? '正在保存...' : editing ? '保存修改' : '创建并进入工作台' }}</button></footer>
      </form>
    </div>

    <div v-if="pendingDelete" class="modal-backdrop"><section class="confirm-dialog tech-panel"><span class="danger-orb"><IconTrash/></span><h2>删除项目？</h2><p>“{{ pendingDelete.name }}”及其需求分析、产品匹配、检索和方案记录将一并删除，此操作无法撤销。</p><footer><button class="ghost-button" @click="pendingDelete = null">取消</button><button class="delete-button" :disabled="deleting" @click="remove">{{ deleting ? '正在删除...' : '确认删除' }}</button></footer></section></div>
  </AppShell>
</template>

<style scoped>
.workspace-head{min-height:116px;margin-bottom:12px;padding:20px 24px;display:grid;grid-template-columns:1fr 110px 110px;align-items:center;gap:12px;border:1px solid rgba(49,168,227,.2);border-radius:12px;background:radial-gradient(circle at 82% 50%,rgba(20,183,231,.11),transparent 26%),linear-gradient(110deg,rgba(8,31,47,.96),rgba(3,14,23,.9))}.workspace-head>div:first-child>span,.eyebrow{color:var(--cyan);font-size:9px;letter-spacing:1.8px}.workspace-head h2{margin:6px 0 5px;font-size:20px}.workspace-head p{margin:0;color:var(--text-3);font-size:11px}.workspace-stat{height:66px;display:grid;place-items:center;align-content:center;gap:5px;border-left:1px solid var(--line)}.workspace-stat b{font-size:25px}.workspace-stat span{color:var(--text-3);font-size:10px}.projects-toolbar{display:flex;justify-content:space-between;margin-bottom:12px}.search{width:360px;height:40px;display:flex;align-items:center;gap:9px;border:1px solid var(--line);border-radius:7px;padding:0 12px;color:var(--text-3);background:rgba(3,13,22,.7)}.search input{flex:1;background:none;border:0;outline:0;color:var(--text-1)}.project-table{min-width:960px;overflow:hidden}.table-head,.table-row{display:grid;grid-template-columns:minmax(260px,1.8fr) 90px 150px 150px 90px 135px 70px;align-items:center;gap:12px;padding:0 16px}.table-head{height:44px;color:var(--text-3);font-size:11px;background:rgba(17,50,73,.16)}.table-row{min-height:66px;border-top:1px solid var(--line);color:var(--text-2);transition:.16s}.table-row:hover{background:linear-gradient(90deg,rgba(23,118,174,.1),transparent)}.project-name{display:flex;align-items:center;gap:10px}.project-name>span:last-child{display:grid;gap:5px}.project-name b{color:var(--text-1)}.project-name small,time{color:var(--text-3);font-size:10px}.project-node{width:28px;height:28px;display:grid;place-items:center;border:1px solid rgba(47,164,226,.27);border-radius:8px;background:rgba(18,129,213,.09)}.project-node i{width:7px;height:7px;border-radius:50%;background:var(--cyan);box-shadow:0 0 10px var(--cyan)}.stage-pill{color:var(--primary-2);font-size:10px}.progress-cell{display:grid;grid-template-columns:1fr 36px;align-items:center;gap:7px}.progress-cell em{font-style:normal;color:var(--text-3);font-size:10px}.row-actions{display:flex;gap:4px}.row-actions button{width:30px;height:30px;display:grid;place-items:center;border:1px solid var(--line);border-radius:6px;background:transparent;color:var(--text-2)}.row-actions button:hover{border-color:var(--primary);color:var(--cyan)}.row-actions button.danger:hover{border-color:var(--danger);color:var(--danger)}.empty{padding:50px;text-align:center;color:var(--text-3)}.modal-backdrop{position:fixed;inset:0;z-index:30;background:rgba(0,5,10,.79);display:grid;place-items:center;backdrop-filter:blur(7px)}.project-dialog{width:min(780px,calc(100vw - 32px));max-height:calc(100vh - 40px);overflow:auto;padding:0}.project-dialog header,.project-dialog footer{padding:16px 20px;display:flex;align-items:center;justify-content:space-between;border-bottom:1px solid var(--line)}.project-dialog footer{border:0;border-top:1px solid var(--line);justify-content:flex-end;gap:10px}.project-dialog h2{margin:4px 0 0}.project-dialog p{margin:5px 0 0;color:var(--text-3);font-size:11px}.project-dialog header button{border:0;background:transparent;color:var(--text-3)}.form-grid{padding:18px 20px;display:grid;grid-template-columns:1fr 1fr;gap:14px}.form-grid>label{display:grid;gap:7px;color:var(--text-2);font-size:11px}.wide{grid-column:1/-1}.form-grid textarea{min-height:78px}fieldset{border:1px solid var(--line);border-radius:7px;padding:12px;display:grid;gap:8px}legend{padding:0 7px;color:var(--text-2)}.check{display:flex!important;align-items:center;gap:8px!important;color:var(--text-2)}.page-error,.dialog-error{color:var(--danger)}.page-error{padding:10px;border:1px solid rgba(255,90,110,.3)}.dialog-error{padding:0 20px 10px!important}.confirm-dialog{width:min(430px,calc(100vw - 32px));padding:28px;text-align:center}.danger-orb{width:58px;height:58px;margin:auto;display:grid;place-items:center;border-radius:50%;border:1px solid rgba(255,106,124,.35);color:var(--danger);background:rgba(255,106,124,.08)}.confirm-dialog h2{margin:16px 0 8px}.confirm-dialog p{color:var(--text-2);line-height:1.7}.confirm-dialog footer{display:flex;justify-content:center;gap:9px;margin-top:20px}.delete-button{min-height:38px;padding:0 18px;border:1px solid rgba(255,106,124,.42);border-radius:6px;color:white;background:rgba(198,48,69,.7)}
</style>
