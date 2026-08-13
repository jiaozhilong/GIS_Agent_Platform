<script setup lang="ts">
import { nextTick, onMounted, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { gsap } from 'gsap'
import { IconArrowRight, IconBrain, IconBooks, IconFileText, IconFolders, IconRobot, IconShieldCheck, IconSparkles } from '@tabler/icons-vue'
import AppShell from '@/components/layout/AppShell.vue'
import MetricCard from '@/components/charts/MetricCard.vue'
import ProjectStatusChart from '@/components/charts/ProjectStatusChart.vue'
import AgentProgressChart from '@/components/charts/AgentProgressChart.vue'
import AgentProjectCanvas from '@/components/charts/AgentProjectCanvas.vue'
import { api } from '@/api/services'
import type { DashboardSummary } from '@/api/contracts'

const router = useRouter()
const page = ref<HTMLElement | null>(null)
const summary = ref<DashboardSummary | null>(null)
const command = ref('')
const selectedProjectId = ref('')
const error = ref('')
let motion: gsap.Context | undefined
const launch = () => {
  const text = command.value.trim()
  if (/(查|检索|知识|资料)/.test(text)) { router.push({ path:'/search', query:{ q:text } }); return }
  const id = selectedProjectId.value || summary.value?.projects[0]?.id
  if (!id) { router.push('/projects'); return }
  if (/需求/.test(text)) router.push(`/projects/${id}/requirements`)
  else if (/产品|选型|匹配/.test(text)) router.push(`/projects/${id}/products`)
  else if (/方案|生成|ppt|文档/i.test(text)) router.push(`/projects/${id}/proposal`)
  else router.push(`/projects/${id}`)
}
onMounted(async () => {
  try {
    summary.value = await api.dashboard(); selectedProjectId.value = summary.value.projects[0]?.id || ''
    await nextTick(); if (page.value) motion = gsap.context(() => gsap.from('.reveal', { autoAlpha:0,y:14,duration:.48,stagger:.055,ease:'power2.out' }), page.value)
  } catch (e) { error.value = e instanceof Error ? e.message : '智能中枢加载失败' }
})
onUnmounted(() => motion?.revert())
</script>

<template>
  <AppShell title="智能中枢" subtitle="观察项目 Agent 网络、知识状态与方案生成任务">
    <div ref="page" class="dashboard-grid">
      <section class="command-center reveal">
        <div class="agent-avatar"><span><IconSparkles :size="24"/></span><i/></div>
        <div class="command-copy"><span>GIS SOLUTION AGENT</span><h2>今天要推进哪项工作？</h2><p>用自然语言进入需求分析、产品匹配、知识检索或方案生成。</p></div>
        <form @submit.prevent="launch"><select v-model="selectedProjectId"><option value="">选择项目空间</option><option v-for="project in summary?.projects" :key="project.id" :value="project.id">{{ project.name }}</option></select><input v-model="command" placeholder="例如：分析这个项目的三维 GIS 产品组合"/><button><IconArrowRight :size="18"/></button></form>
        <div class="quick-prompts"><button @click="command='运行项目需求分析';launch()">需求分析</button><button @click="command='匹配超图产品体系';launch()">产品选型</button><button @click="command='查找 SuperMap iServer 云原生部署资料';launch()">知识探索</button><button @click="command='生成项目建设方案';launch()">生成方案</button></div>
      </section>
      <p v-if="error" class="page-error">{{error}}</p>
      <section class="metrics reveal">
        <MetricCard label="项目空间" :value="summary?.projectCount ?? '—'" trend="真实项目数据" :icon="IconFolders" />
        <MetricCard label="需求任务" :value="summary?.requirementTaskCount ?? '—'" trend="累计运行" :icon="IconBrain" />
        <MetricCard label="生成方案" :value="summary?.proposalCount ?? '—'" trend="成功任务" :icon="IconFileText" />
        <MetricCard label="知识库文档" :value="summary?.knowledgeDocumentCount ?? '—'" trend="仅统计文档" :icon="IconBooks" />
        <MetricCard label="运行 Agent" :value="summary?.runningAgentCount ?? '—'" trend="实时任务" :icon="IconRobot" tone="amber" />
        <MetricCard label="系统状态" :value="summary?.systemStatus === 'HEALTHY' ? '在线' : '降级'" trend="知识引擎" :icon="IconShieldCheck" tone="green" />
      </section>
      <section class="network-card tech-panel reveal"><AgentProjectCanvas :projects="summary?.projects || []"/></section>
      <section class="status-card tech-panel reveal"><div class="panel-title"><span>项目阶段分布</span><span class="secondary">实时</span></div><ProjectStatusChart :counts="summary?.stageCounts"/></section>
      <section class="project-list tech-panel reveal">
        <div class="panel-title"><span>Agent 活动流</span><RouterLink to="/projects" class="secondary">进入工作台</RouterLink></div>
        <RouterLink v-for="project in summary?.projects.slice(0,5)" :key="project.id" :to="`/projects/${project.id}`" class="project-row"><span class="signal"><i/></span><span><b>{{ project.name }}</b><small>{{ project.customerName }}</small></span><span class="stage">{{ project.stage }}</span><div class="mini-progress"><i :style="{width:`${project.progress}%`}"/></div><time>{{ project.updatedAt.slice(5,16).replace('T',' ') }}</time></RouterLink>
      </section>
      <section class="agent-card tech-panel reveal"><div class="panel-title"><span>能力调用量</span><span class="secondary">真实累计</span></div><AgentProgressChart :requirement-count="summary?.requirementTaskCount" :proposal-count="summary?.proposalCount" :running-count="summary?.runningAgentCount" :document-count="summary?.knowledgeDocumentCount"/></section>
      <section class="knowledge-card tech-panel reveal">
        <div class="panel-title"><span>知识空间</span><RouterLink to="/search" class="secondary">直接检索</RouterLink></div>
        <RouterLink v-for="(item,index) in summary?.knowledgeBases.slice(0,5)" :key="item.id" to="/knowledge" class="knowledge-row"><span>{{ String(index+1).padStart(2,'0') }}</span><div><b>{{ item.name }}</b><small>{{ item.documentCount }} 份文档</small></div><em :class="{ready:item.ready}">{{ item.ready?'READY':'SYNC' }}</em></RouterLink>
      </section>
    </div>
  </AppShell>
</template>

<style scoped>
.dashboard-grid{display:grid;grid-template-columns:minmax(0,1.7fr) minmax(280px,.72fr);gap:12px;min-height:calc(100vh - 110px)}.command-center{grid-column:1/-1;position:relative;min-height:152px;padding:23px 25px 18px 116px;display:grid;grid-template-columns:minmax(260px,.75fr) minmax(440px,1.4fr);align-items:center;gap:24px;border:1px solid rgba(56,181,231,.24);border-radius:14px;overflow:hidden;background:radial-gradient(circle at 8% 50%,rgba(24,218,241,.14),transparent 15%),radial-gradient(circle at 88% -20%,rgba(24,101,235,.15),transparent 29%),linear-gradient(110deg,rgba(7,30,46,.98),rgba(3,13,23,.94))}.command-center:after{content:'';position:absolute;inset:0;background:linear-gradient(90deg,transparent 49.8%,rgba(61,175,222,.025) 50%,transparent 50.2%);background-size:40px 100%;pointer-events:none}.agent-avatar{position:absolute;left:30px;top:31px;width:64px;height:64px;display:grid;place-items:center;border:1px solid rgba(52,214,237,.36);border-radius:20px;color:var(--cyan);background:linear-gradient(145deg,rgba(33,194,233,.18),rgba(21,95,207,.12));box-shadow:0 0 40px rgba(21,185,225,.14);z-index:1}.agent-avatar span{width:42px;height:42px;display:grid;place-items:center;border:1px dashed rgba(53,218,240,.3);border-radius:50%;animation:spin 12s linear infinite}.agent-avatar i{position:absolute;right:-2px;top:5px;width:9px;height:9px;border-radius:50%;background:var(--success);box-shadow:0 0 10px var(--success)}.command-copy,form,.quick-prompts{position:relative;z-index:1}.command-copy>span{color:var(--cyan);font-size:9px;letter-spacing:2px}.command-copy h2{margin:7px 0 5px;font-size:22px}.command-copy p{margin:0;color:var(--text-3);font-size:11px}.command-center form{height:50px;padding:5px;display:grid;grid-template-columns:190px 1fr 42px;gap:5px;border:1px solid rgba(59,168,223,.3);border-radius:9px;background:rgba(1,9,16,.74)}.command-center select,.command-center input{min-width:0;border:0;outline:0;color:var(--text-1);background:transparent;padding:0 10px}.command-center select{border-right:1px solid var(--line);color:var(--text-2);font-size:10px}.command-center select option{background:#07131f}.command-center form button{border:0;border-radius:7px;color:white;background:linear-gradient(145deg,#1978ee,#1abedb)}.quick-prompts{grid-column:2;display:flex;gap:6px;margin-top:-14px}.quick-prompts button{border:1px solid var(--line);border-radius:999px;padding:4px 9px;color:var(--text-3);background:rgba(11,45,65,.24);font-size:8px}.quick-prompts button:hover{color:var(--cyan);border-color:var(--line-strong)}.metrics{grid-column:1/-1;display:grid;grid-template-columns:repeat(6,minmax(115px,1fr));gap:10px}.network-card{min-height:430px;padding:13px}.status-card,.agent-card{min-height:220px;padding:13px}.status-card>:last-child,.agent-card>:last-child{height:185px}.project-list,.knowledge-card{padding:13px}.project-row{min-height:56px;border-top:1px solid var(--line);display:grid;grid-template-columns:16px minmax(0,1fr) 100px 65px 67px;align-items:center;gap:10px;color:var(--text-2)}.signal i{display:block;width:6px;height:6px;border-radius:50%;background:var(--cyan);box-shadow:0 0 8px var(--cyan)}.project-row>span:nth-child(2){display:grid;gap:4px}.project-row b{color:var(--text-1);font-size:11px}.project-row small,.project-row time{color:var(--text-3);font-size:9px}.stage{color:var(--primary-2);font-size:8px}.mini-progress{height:3px;background:rgba(106,151,180,.12);border-radius:10px;overflow:hidden}.mini-progress i{display:block;height:100%;background:var(--cyan)}.knowledge-row{min-height:51px;display:grid;grid-template-columns:27px 1fr auto;align-items:center;gap:8px;border-top:1px solid var(--line)}.knowledge-row>span{color:var(--primary-2);font:600 9px ui-monospace}.knowledge-row>div{display:grid;gap:4px}.knowledge-row b{font-size:10px}.knowledge-row small{color:var(--text-3);font-size:8px}.knowledge-row em{font-style:normal;color:var(--warning);font-size:8px}.knowledge-row em.ready{color:var(--success)}.page-error{grid-column:1/-1;padding:9px;color:var(--danger);border:1px solid rgba(255,90,110,.25)}.reveal{will-change:transform,opacity}@keyframes spin{to{transform:rotate(360deg)}}@media(max-width:1180px){.metrics{grid-template-columns:repeat(3,1fr)}.dashboard-grid{grid-template-columns:1fr}.command-center,.metrics{grid-column:auto}.command-center{grid-template-columns:1fr;padding-left:116px}.quick-prompts{grid-column:auto;margin:0}}@media(max-width:760px){.command-center{padding:22px}.agent-avatar{display:none}.command-center form{grid-template-columns:1fr 42px}.command-center select{display:none}}
</style>
