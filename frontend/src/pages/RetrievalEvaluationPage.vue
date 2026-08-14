<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { IconChartDots, IconPlayerPlay, IconRefresh } from '@tabler/icons-vue'
import AppShell from '@/components/layout/AppShell.vue'
import { api } from '@/api/services'
import type { RetrievalEvaluationCase, RetrievalEvaluationRun } from '@/api/contracts'

const cases = ref<RetrievalEvaluationCase[]>([])
const runs = ref<RetrievalEvaluationRun[]>([])
const active = ref<RetrievalEvaluationRun | null>(null)
const loading = ref(false)
const error = ref('')
let timer: number | undefined
const metric = (name: string) => active.value?.metrics?.[name]
const percent = (name: string) => typeof metric(name) === 'number' ? `${Math.round(Number(metric(name))*100)}%` : '待标注'
const progress = computed(() => active.value ? Math.round(active.value.completedCases / Math.max(1, active.value.totalCases) * 100) : 0)

const load = async () => {
  try {
    ;[cases.value, runs.value] = await Promise.all([api.retrievalEvaluationCases(), api.retrievalEvaluationRuns()])
    if (!active.value && runs.value.length) await select(runs.value[0].id)
  } catch (e) { error.value = e instanceof Error ? e.message : '评测数据加载失败' }
}
const select = async (id: string) => { active.value = await api.retrievalEvaluationRun(id) }
const poll = (id: string) => {
  window.clearInterval(timer); timer = window.setInterval(async () => {
    active.value = await api.retrievalEvaluationRun(id)
    if (!['PENDING','RUNNING'].includes(active.value.status)) { window.clearInterval(timer); loading.value=false; await load() }
  }, 2500)
}
const start = async () => {
  loading.value=true; error.value=''
  try { active.value = await api.startRetrievalEvaluation(); poll(active.value.id) }
  catch(e){ error.value=e instanceof Error?e.message:'启动评测失败'; loading.value=false }
}
onMounted(load)
onBeforeUnmount(() => window.clearInterval(timer))
</script>

<template>
  <AppShell title="检索评测" subtitle="用真实 GIS 售前问题持续测量 RAGFlow 检索质量">
    <div class="toolbar tech-panel"><div><IconChartDots :size="22"/><span><b>Retrieval Evaluation</b><small>{{cases.length}} 条业务评测题 · 不修改生产切片</small></span></div><button @click="load"><IconRefresh :size="15"/>刷新</button><button class="primary" :disabled="loading" @click="start"><IconPlayerPlay :size="15"/>运行评测</button></div>
    <p v-if="error" class="error">{{error}}</p>
    <div v-if="active" class="metrics">
      <article class="tech-panel"><span>Recall@5</span><b>{{percent('Recall@5')}}</b></article><article class="tech-panel"><span>Recall@10</span><b>{{percent('Recall@10')}}</b></article><article class="tech-panel"><span>MRR</span><b>{{metric('MRR') ?? '—'}}</b></article><article class="tech-panel"><span>查询成功率</span><b>{{percent('successfulQueryRate')}}</b></article><article class="tech-panel"><span>PPT页命中</span><b>{{percent('expectedPptSlideHitRate')}}</b></article>
    </div>
    <div class="grid">
      <section class="tech-panel runs"><div class="panel-title"><span>评测运行</span><i>{{runs.length}}</i></div><button v-for="run in runs" :key="run.id" :class="{active:active?.id===run.id}" @click="select(run.id)"><b>{{run.createdAt.slice(0,19).replace('T',' ')}}</b><span>{{run.status}} · {{run.completedCases}}/{{run.totalCases}}</span></button></section>
      <section class="tech-panel detail"><div class="panel-title"><span>当前结果</span><i>{{active?.status || 'IDLE'}}</i></div><div class="progress"><i :style="{width:progress+'%'}"/><span>{{progress}}%</span></div><table><thead><tr><th>问题</th><th>知识库排名</th><th>文档排名</th><th>耗时</th><th>状态</th></tr></thead><tbody><tr v-for="item in active?.results" :key="item.caseId"><td>{{item.query}}</td><td>{{item.datasetHitRank ?? '未命中'}}</td><td>{{item.documentHitRank ?? '未标注'}}</td><td>{{item.durationMs}}ms</td><td :class="{bad:item.errorMessage}">{{item.errorMessage?'失败':'完成'}}</td></tr><tr v-if="!active?.results.length"><td colspan="5">等待运行或结果写入……</td></tr></tbody></table></section>
    </div>
  </AppShell>
</template>

<style scoped>
.toolbar{min-height:70px;padding:12px 16px;display:flex;align-items:center;gap:8px}.toolbar>div{display:flex;align-items:center;gap:10px;margin-right:auto;color:var(--cyan)}.toolbar span{display:grid}.toolbar small{color:var(--text-3);margin-top:4px}.toolbar button{height:34px;padding:0 12px;border:1px solid var(--line);border-radius:5px;background:rgba(15,48,68,.35);color:var(--text-2);display:flex;align-items:center;gap:6px}.toolbar button.primary{border-color:var(--primary);color:var(--primary-2)}.error{color:var(--danger)}.metrics{display:grid;grid-template-columns:repeat(5,1fr);gap:9px;margin:10px 0}.metrics article{padding:13px}.metrics span{color:var(--text-3);font-size:10px}.metrics b{display:block;margin-top:7px;font-size:21px;color:var(--cyan)}.grid{display:grid;grid-template-columns:260px 1fr;gap:10px;height:calc(100vh - 280px);min-height:460px}.runs,.detail{padding:13px;overflow:auto}.panel-title{display:flex;justify-content:space-between;margin-bottom:10px}.panel-title i{font-style:normal;color:var(--cyan)}.runs button{width:100%;display:grid;gap:5px;text-align:left;padding:10px;margin-bottom:6px;border:1px solid transparent;border-radius:5px;background:rgba(13,39,55,.35);color:var(--text-2)}.runs button.active{border-color:var(--primary);background:rgba(25,134,255,.13)}.runs span{font-size:10px;color:var(--text-3)}.progress{position:relative;height:26px;background:rgba(10,36,52,.55);border:1px solid var(--line);margin-bottom:10px}.progress i{display:block;height:100%;background:linear-gradient(90deg,var(--primary),var(--cyan));opacity:.55}.progress span{position:absolute;inset:0;display:grid;place-items:center;font-size:10px}table{width:100%;border-collapse:collapse;font-size:10px}th,td{padding:9px;border-bottom:1px solid var(--line);text-align:left}th{color:var(--text-3)}td.bad{color:var(--danger)}@media(max-width:1100px){.metrics{grid-template-columns:repeat(3,1fr)}.grid{grid-template-columns:1fr}.runs{display:none}}
</style>
