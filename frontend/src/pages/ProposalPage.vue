<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { IconDownload, IconPresentation, IconRefresh, IconSparkles } from '@tabler/icons-vue'
import AppShell from '@/components/layout/AppShell.vue'
import ProjectTabs from '@/components/layout/ProjectTabs.vue'
import { api } from '@/api/services'
import type { KnowledgeBase, ProjectDetail, SolutionGenerationRun } from '@/api/contracts'

const route = useRoute()
const project = ref<ProjectDetail | null>(null)
const bases = ref<KnowledgeBase[]>([])
const run = ref<SolutionGenerationRun | null>(null)
const loading = ref(false)
const error = ref('')
const activeSection = ref('')
const view = ref<'outline'|'preview'>('preview')
const editingContent = ref('')
let timer: number | undefined

const section = computed(() => run.value?.sections.find(item => item.id === activeSection.value) || run.value?.sections[0])
watch(() => [section.value?.id, section.value?.content] as const, ([, content]) => { editingContent.value = content || '' })
const statusText = computed(() => ({ PENDING:'等待执行', RUNNING:'正在联合生成', SUCCEEDED:'已完成', FAILED:'生成失败', CANCELLED:'已取消' }[run.value?.status || 'PENDING']))

const poll = (id: string) => {
  window.clearInterval(timer)
  timer = window.setInterval(async () => {
    try {
      const latest = await api.solutionRun(id); run.value = latest
      if (latest.sections.length && !activeSection.value) activeSection.value = latest.sections[0].id
      if (['SUCCEEDED','FAILED','CANCELLED'].includes(latest.status)) { window.clearInterval(timer); loading.value = false }
    } catch (e) { error.value = e instanceof Error ? e.message : '查询任务失败'; window.clearInterval(timer); loading.value = false }
  }, 2000)
}

const generate = async () => {
  if (!project.value) return
  const selected = project.value.knowledgeBaseIds.length ? project.value.knowledgeBaseIds : bases.value.filter(item => item.chunkCount > 0).map(item => item.id)
  if (!selected.length) { error.value = '没有可用于生成的知识库，请先同步并上传资料'; return }
  loading.value = true; error.value = ''; activeSection.value = ''
  try {
    run.value = await api.startSolutionRun(project.value.id, { knowledgeBaseIds:selected, groundingPolicy:'BALANCED', allowModelSupplement:true, outputFormats:['DOCX','PPTX','PDF'] })
    poll(run.value.id)
  } catch (e) { error.value = e instanceof Error ? e.message : '方案生成启动失败'; loading.value = false }
}

const exportContent = (format: 'DOC'|'TXT') => {
  if (!run.value?.sections.length) return
  const text = run.value.sections.map(item => `${item.title}\n\n${item.content}`).join('\n\n')
  const isDoc = format === 'DOC'
  const body = isDoc ? `<html><meta charset="utf-8"><body>${text.split('\n').map(line => `<p>${line || '&nbsp;'}</p>`).join('')}</body></html>` : text
  const url = URL.createObjectURL(new Blob([body], { type: isDoc ? 'application/msword;charset=utf-8' : 'text/plain;charset=utf-8' }))
  const link = document.createElement('a'); link.href = url; link.download = `${project.value?.name || 'GIS方案'}.${isDoc ? 'doc' : 'txt'}`; link.click(); URL.revokeObjectURL(url)
}

const selectSection = (id: string) => {
  activeSection.value = id; view.value = 'outline'
  editingContent.value = run.value?.sections.find(item => item.id === id)?.content || ''
}
const saveSection = async () => {
  if (!section.value) return
  try {
    const updated = await api.updateSolutionSection(section.value.id, { title: section.value.title, content: editingContent.value })
    Object.assign(section.value, updated)
  } catch (e) { error.value = e instanceof Error ? e.message : '保存章节失败' }
}
const toggleLock = async () => {
  if (!section.value) return
  try { Object.assign(section.value, await api.lockSolutionSection(section.value.id, !section.value.locked)) }
  catch (e) { error.value = e instanceof Error ? e.message : '更新锁定状态失败' }
}
const regenerateSection = async () => {
  if (!section.value) return
  try {
    Object.assign(section.value, await api.regenerateSolutionSection(section.value.id))
    poll(run.value!.id)
  } catch (e) { error.value = e instanceof Error ? e.message : '章节重新生成失败' }
}

onMounted(async () => {
  try {
    const projectId = String(route.params.id)
    const [detail, knowledge, runs] = await Promise.all([api.project(projectId), api.knowledgeBases(), api.solutionRuns(projectId)])
    project.value = detail; bases.value = knowledge
    const queryRun = typeof route.query.runId === 'string' ? runs.find(item => item.id === route.query.runId) : undefined
    run.value = queryRun || runs[0] || null
    if (run.value?.sections.length) activeSection.value = run.value.sections[0].id
    if (run.value && ['PENDING','RUNNING'].includes(run.value.status)) { loading.value = true; poll(run.value.id) }
  } catch (e) { error.value = e instanceof Error ? e.message : '方案页面加载失败' }
})
onBeforeUnmount(() => window.clearInterval(timer))
</script>

<template>
  <AppShell title="方案生成" subtitle="RAGFlow 知识检索与 DeepSeek 联合生成，结果保留证据覆盖率">
    <ProjectTabs />
    <div class="proposal-toolbar"><div><button :class="{active:view==='outline'}" @click="view='outline'">正文内容</button><button :class="{active:view==='preview'}" @click="view='preview'">封面预览</button></div><span v-if="run" class="tag">{{statusText}} · 证据 {{Math.round(run.evidenceCoverage*100)}}%</span><button class="secondary-button" :disabled="loading" @click="generate"><IconRefresh :size="15"/>{{loading?'生成中...':run?'重新生成':'开始生成'}}</button><button class="primary-button" :disabled="run?.status!=='SUCCEEDED'" @click="exportContent('DOC')"><IconPresentation :size="16"/>导出 Word</button><button class="ghost-button" :disabled="run?.status!=='SUCCEEDED'" @click="exportContent('TXT')"><IconDownload :size="16"/>下载正文</button></div>
    <p v-if="error || run?.errorMessage" class="error">{{error || run?.errorMessage}}</p>
    <div class="proposal-layout">
      <aside class="outline tech-panel"><div class="panel-title"><span>方案章节</span><span class="score">{{run?.sections.length || 0}}</span></div><button v-for="item in run?.sections" :key="item.id" :class="{active:activeSection===item.id}" @click="selectSection(item.id)"><span>{{item.title}}</span><i>{{item.sourceType}} · {{item.status}}</i></button><div v-if="!run?.sections.length" class="empty">点击“开始生成”，系统会先检索知识库，再由大模型补全方案。</div></aside>
      <section class="slide tech-panel">
        <div v-if="view==='preview'" class="cover"><img src="/assets/images/proposal-cover-city.png" alt="GIS解决方案封面"/><div class="cover-copy"><span>GIS AGENT PLATFORM</span><h2>{{project?.name || 'GIS 项目建设方案'}}</h2><p>知识库证据 · 大模型补全 · 全程可追溯</p><small>{{project?.customerName}}　{{new Date().getFullYear()}}年</small></div></div>
        <div v-else class="document-editor"><div class="section-heading"><h2>{{section?.title || '尚未生成正文'}}</h2><div v-if="section" class="section-actions"><button @click="toggleLock">{{section.locked?'解锁':'锁定'}}</button><button :disabled="section.locked" @click="regenerateSection">重新生成</button><button class="primary" :disabled="section.locked" @click="saveSection">保存正文</button></div></div><div v-if="section?.purpose" class="section-purpose">{{section.purpose}} · {{section.status}}</div><div v-if="loading&&!section" class="generating"><IconSparkles :size="26"/>系统正在规划章节、分章节检索并生成 Evidence Package...</div><textarea v-else v-model="editingContent" class="tech-textarea" :readonly="section?.locked" :placeholder="section?.content || '点击上方“开始生成”运行章节级知识检索与方案生成。'"/><div v-if="section" class="citation-note">内容来源：{{section.sourceType}}　证据覆盖率：{{Math.round(section.evidenceCoverage*100)}}%　引用：{{section.evidence.length}} 条</div><div v-if="section?.evidence.length" class="evidence-list"><b>本章节可追溯依据</b><article v-for="item in section.evidence" :key="item.evidenceId"><span>{{item.documentName}}</span><em v-if="item.slideNumber">PPT 第 {{item.slideNumber}} 页</em><em v-else-if="item.pageNumber">第 {{item.pageNumber}} 页</em><RouterLink v-if="item.assetId" :to="{path:'/assets',query:{asset:item.assetId}}">查看知识资产</RouterLink></article></div></div>
        <footer><span>{{run ? `任务 ${run.id.slice(0,8)}` : '尚未创建任务'}}</span><span>{{run?.modelName || '等待模型返回'}}</span><span>{{run?.updatedAt?.slice(0,19).replace('T',' ')}}</span></footer>
      </section>
      <aside class="templates tech-panel"><div class="panel-title"><span>生成配置</span></div><div class="config"><b>证据策略</b><span>BALANCED</span><b>模型补全</b><span>已启用</span><b>已选知识库</b><span>{{project?.knowledgeBaseIds.length || bases.filter(item=>item.chunkCount>0).length}} 个</span><b>输出状态</b><span>{{statusText}}</span></div><div class="export-list"><b>可用交付</b><button :disabled="run?.status!=='SUCCEEDED'" @click="exportContent('DOC')"><IconPresentation :size="15"/>Word 正文</button><button :disabled="run?.status!=='SUCCEEDED'" @click="exportContent('TXT')"><IconDownload :size="15"/>纯文本</button></div></aside>
    </div>
  </AppShell>
</template>

<style scoped>
.proposal-toolbar{height:46px;margin:-8px 0 10px;display:flex;align-items:center;justify-content:flex-end;gap:8px}.proposal-toolbar>div{margin-right:auto;display:flex}.proposal-toolbar>div button{height:38px;background:transparent;border:0;border-bottom:2px solid transparent;color:var(--text-3);padding:0 14px}.proposal-toolbar>div button.active{color:var(--text-1);border-bottom-color:var(--primary)}.error{padding:9px 12px;color:var(--danger);border:1px solid rgba(255,90,110,.25)}.proposal-layout{display:grid;grid-template-columns:210px minmax(520px,1fr)180px;gap:10px;height:calc(100vh - 206px);min-height:520px}.outline,.templates{padding:13px;overflow:auto}.outline{display:flex;flex-direction:column;gap:5px}.outline>button{min-height:48px;background:transparent;border:1px solid transparent;border-radius:5px;color:var(--text-2);display:flex;flex-direction:column;align-items:flex-start;justify-content:center;gap:4px;text-align:left;padding:7px 8px}.outline>button.active{background:rgba(25,134,255,.14);border-color:var(--line-strong);color:var(--text-1)}.outline i{font-size:9px;font-style:normal;color:var(--success)}.empty{padding:22px 5px;color:var(--text-3);font-size:11px;line-height:1.7}.slide{display:grid;grid-template-rows:1fr 34px;padding:14px;min-height:0}.cover{position:relative;overflow:hidden;min-height:0;border:1px solid rgba(125,179,217,.2);box-shadow:0 20px 50px rgba(0,0,0,.3)}.cover img{position:absolute;inset:0;width:100%;height:100%;object-fit:cover}.cover::after{content:'';position:absolute;inset:0;background:linear-gradient(90deg,rgba(238,247,255,.95),rgba(237,247,255,.72) 45%,transparent 72%)}.cover-copy{position:absolute;z-index:2;left:8%;top:17%;color:#0a3153;max-width:58%}.cover-copy>span{font-size:11px;letter-spacing:2px;color:#2c78aa}.cover-copy h2{font-size:clamp(24px,3vw,44px);line-height:1.3;margin:30px 0 14px}.cover-copy p{font-size:16px;font-weight:650}.cover-copy small{display:block;margin-top:15%;color:#4c6d86}.slide>footer{display:flex;align-items:end;justify-content:space-between;color:var(--text-3);font-size:10px}.document-editor{padding:28px;overflow:auto}.document-editor h2{font-size:23px}.document-editor textarea{min-height:420px;line-height:1.9}.generating{min-height:320px;display:grid;place-items:center;align-content:center;gap:12px;color:var(--primary-2)}.citation-note{margin:12px 0;padding:10px;border-left:2px solid var(--primary);background:rgba(25,134,255,.08);color:var(--text-3);font-size:11px}.templates{display:flex;flex-direction:column;gap:8px}.config{display:grid;gap:7px}.config b{margin-top:8px}.config span{color:var(--text-3);font-size:11px}.export-list{margin-top:auto;border-top:1px solid var(--line);padding-top:12px;display:grid;gap:7px}.export-list button{height:32px;border:1px solid var(--line);background:rgba(17,55,81,.2);color:var(--text-2);border-radius:4px;display:flex;align-items:center;gap:7px;padding:0 9px}.export-list button:disabled{opacity:.45}@media(max-width:1100px){.proposal-layout{grid-template-columns:190px 1fr}.templates{display:none}.proposal-toolbar .tag{display:none}}
.section-purpose{margin:-10px 0 12px;color:var(--cyan);font-size:11px}.evidence-list{display:grid;gap:6px;margin-top:10px;padding:12px;border:1px solid var(--line);border-radius:6px}.evidence-list>article{display:grid;grid-template-columns:1fr auto auto;gap:8px;color:var(--text-2);font-size:10px}.evidence-list em{font-style:normal;color:var(--text-3)}.evidence-list a{color:var(--primary-2)}
.section-heading{display:flex;align-items:center;justify-content:space-between;gap:12px}.section-actions{display:flex;gap:6px}.section-actions button{height:30px;padding:0 10px;border:1px solid var(--line);border-radius:4px;background:rgba(17,55,81,.3);color:var(--text-2)}.section-actions button.primary{border-color:var(--primary);background:rgba(25,134,255,.2);color:var(--primary-2)}.section-actions button:disabled{opacity:.45}
</style>
