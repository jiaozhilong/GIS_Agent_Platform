<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { IconArrowRight, IconPlayerPlay, IconRefresh } from '@tabler/icons-vue'
import AppShell from '@/components/layout/AppShell.vue'
import ProjectTabs from '@/components/layout/ProjectTabs.vue'
import DemandRadarChart from '@/components/charts/DemandRadarChart.vue'
import { api } from '@/api/services'
import type { RequirementAnalysis } from '@/api/contracts'

const route = useRoute(); const loading = ref(false); const result = ref<RequirementAnalysis | null>(null)
const rawDemand = ref('建设统一的国土空间基础信息平台，整合现有自然资源业务系统和多源空间数据，支持二三维一体化展示、专题图层管理、空间分析、规划审批联动与智能辅助决策。')
const run = async () => { loading.value = true; try { result.value = await api.analyzeRequirements(String(route.params.id)) } finally { loading.value = false } }
onMounted(run)
const currentStep = computed(() => result.value ? 3 : loading.value ? 2 : 1)
</script>
<template>
  <AppShell title="需求分析" subtitle="AI 提取需求要点、能力维度与产品方向">
    <ProjectTabs />
    <div class="agent-steps"><span v-for="(step, i) in ['需求整理','语义理解','行业分析','需求量化','生成报告']" :key="step" :class="{ active: currentStep === i + 1, done: currentStep > i + 1 }"><i>{{ currentStep > i + 1 ? '✓' : i + 1 }}</i>{{ step }}</span></div>
    <div class="analysis-grid">
      <section class="source tech-panel"><div class="panel-title"><span>客户原始需求</span><button class="ghost-button" @click="run"><IconRefresh :size="15" />重新分析</button></div><textarea v-model="rawDemand" class="tech-textarea" /><button class="primary-button" :disabled="loading" @click="run"><IconPlayerPlay :size="16" />{{ loading ? 'Agent分析中...' : '运行需求分析Agent' }}</button><div class="demand-points"><h3>需求要点</h3><ul><li v-for="point in result?.demandPoints" :key="point">{{ point }}</li></ul></div><div class="case"><span>推荐行业案例</span><b>XX市国土空间基础信息平台</b><small>自然资源 · 匹配度 96%</small></div></section>
      <section class="graph tech-panel"><div class="panel-title"><span>需求关联图谱</span><span class="secondary">实体关系可视化</span></div><div class="network"><div class="center">需求</div><div v-for="(item, i) in ['数据整合','空间分析','规划管理','多源支持','业务应用','AI辅助']" :key="item" class="node" :style="{ '--i': i }"><span>{{ item }}</span></div></div></section>
      <aside class="score-panel tech-panel"><div class="panel-title"><span>需求覆盖度</span><strong class="score">{{ result?.completion ?? 0 }}%</strong></div><DemandRadarChart /><div class="dimension-list"><div v-for="item in result?.dimensions" :key="item.name"><span>{{ item.name }}</span><div class="progress-track"><div class="progress-value" :style="{ width: `${item.score}%` }" /></div><em>{{ item.score }}%</em></div></div><div class="recommend"><b>建议产品组合</b><span v-for="name in result?.recommendedProductNames" :key="name">{{ name }}</span></div><RouterLink :to="`/projects/${route.params.id}/products`" class="primary-button">进入产品匹配 <IconArrowRight :size="16" /></RouterLink></aside>
    </div>
  </AppShell>
</template>
<style scoped>
.agent-steps { height: 46px; margin: -6px 0 12px; display: grid; grid-template-columns: repeat(5,1fr); border: 1px solid var(--line); border-radius: 7px; background: rgba(4,15,24,.62); }.agent-steps span { display: flex; align-items: center; justify-content: center; gap: 8px; color: var(--text-3); position: relative; }.agent-steps span:not(:last-child)::after { content: ''; position: absolute; right: -12%; width: 24%; height: 1px; background: var(--line); }.agent-steps i { width: 22px; height: 22px; display: grid; place-items: center; border-radius: 50%; background: rgba(73,112,140,.24); font-style: normal; font-size: 11px; }.agent-steps .active { color: var(--text-1); }.agent-steps .active i { background: var(--primary); box-shadow: 0 0 14px rgba(25,134,255,.45); }.agent-steps .done i { background: var(--success); color: #03120c; }
.analysis-grid { display: grid; grid-template-columns: minmax(300px,.85fr) minmax(390px,1.2fr) 250px; gap: 10px; min-height: calc(100vh - 218px); }.source,.graph,.score-panel { padding: 14px; }.source { display: flex; flex-direction: column; gap: 13px; }.source .primary-button { align-self: flex-start; }.demand-points { border-top: 1px solid var(--line); }.demand-points h3 { font-size: 13px; }.demand-points ul { padding-left: 18px; color: var(--text-2); line-height: 2; }.demand-points li::marker { color: var(--primary); }.case { margin-top: auto; border: 1px solid var(--line); padding: 12px; display: grid; gap: 5px; border-radius: 6px; background: rgba(13,43,63,.25); }.case span,.case small { color: var(--text-3); font-size: 11px; }
.graph { display: grid; grid-template-rows: 30px 1fr; }.network { position: relative; min-height: 420px; overflow: hidden; }.center,.node { position: absolute; display: grid; place-items: center; border-radius: 50%; border: 1px solid var(--line-strong); }.center { width: 90px; height: 90px; left: 50%; top: 50%; transform: translate(-50%,-50%); background: radial-gradient(circle,#238cff,#07305e); box-shadow: 0 0 34px rgba(33,137,255,.45); font-size: 17px; }.node { --angle: calc(var(--i) * 60deg); width: 74px; height: 74px; left: calc(50% + cos(var(--angle)) * 34%); top: calc(50% + sin(var(--angle)) * 36%); transform: translate(-50%,-50%); background: rgba(8,30,49,.94); color: var(--text-2); }.node::before { content:''; position:absolute; width: 150px; height:1px; right: 100%; top:50%; transform-origin:right; transform: rotate(var(--angle)); background: linear-gradient(90deg,transparent,var(--primary)); opacity:.45; }
.score-panel { display: grid; grid-template-rows: auto 230px auto auto auto; gap: 13px; }.score-panel > :nth-child(2) { height: 230px; }.dimension-list { display: grid; gap: 10px; }.dimension-list > div { display:grid; grid-template-columns: 55px 1fr 35px; align-items:center; gap:7px; font-size:10px; color:var(--text-2); }.dimension-list em { font-style:normal;color:var(--text-3); }.recommend { border-top:1px solid var(--line);padding-top:12px;display:flex;flex-wrap:wrap;gap:6px; }.recommend b { width:100%;font-size:12px; }.recommend span { padding:4px 7px;background:rgba(25,134,255,.12);border:1px solid var(--line);border-radius:4px;font-size:10px;color:var(--primary-2); }
@media(max-width:1180px){.analysis-grid{grid-template-columns:1fr 1fr}.score-panel{grid-column:1/-1;grid-template-columns:1fr 1fr;grid-template-rows:auto 220px auto}.score-panel>.primary-button{grid-column:2}.score-panel>.panel-title{grid-column:1/-1}.score-panel>:nth-child(2){height:220px}}
</style>
