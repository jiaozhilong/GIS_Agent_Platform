<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import { gsap } from 'gsap'
import { IconArrowUpRight, IconSparkles } from '@tabler/icons-vue'
import type { ProjectStage, ProjectSummary } from '@/api/contracts'

const props = defineProps<{ projects: ProjectSummary[] }>()
const root = ref<HTMLElement | null>(null)
const selectedId = ref('')
const activeStage = ref<'ALL' | ProjectStage>('ALL')
let motion: gsap.Context | undefined
const stageLabels: Record<ProjectStage, string> = { DRAFT:'草稿',REQUIREMENT_ANALYSIS:'需求分析',PRODUCT_MATCH:'产品匹配',KNOWLEDGE_RETRIEVAL:'知识检索',PROPOSAL_GENERATION:'方案生成',REVIEW:'评审',DELIVERED:'已交付' }
const positions = [[15,22],[34,13],[66,14],[84,25],[88,52],[78,79],[55,86],[31,83],[12,65],[24,45],[48,23],[70,39],[64,66],[41,71],[45,45],[23,74],[75,20],[90,68],[10,42],[54,78]]
const stages = computed(() => ['ALL', ...new Set(props.projects.map(item => item.stage))] as ('ALL'|ProjectStage)[])
const visible = computed(() => props.projects.filter(item => activeStage.value === 'ALL' || item.stage === activeStage.value).map((item,index) => ({ ...item, x: positions[index % positions.length][0], y: positions[index % positions.length][1] })))
const selected = computed(() => props.projects.find(item => item.id === selectedId.value) || visible.value[0])
const animate = async () => {
  await nextTick(); if (!root.value) return
  motion?.revert()
  motion = gsap.context(() => gsap.from('.project-signal', { autoAlpha: 0, scale: .65, duration: .42, stagger: .045, ease: 'back.out(1.7)' }), root.value)
}
watch(visible, animate)
onMounted(animate)
onUnmounted(() => motion?.revert())
</script>

<template>
  <div ref="root" class="project-canvas">
    <div class="canvas-toolbar"><span>项目 Agent 网络</span><div><button v-for="stage in stages" :key="stage" :class="{active:activeStage===stage}" @click="activeStage=stage">{{ stage==='ALL'?'全部':stageLabels[stage] }}</button></div></div>
    <div class="network">
      <svg viewBox="0 0 100 100" preserveAspectRatio="none"><line v-for="project in visible" :key="project.id" x1="50" y1="50" :x2="project.x" :y2="project.y"/><circle cx="50" cy="50" r="25"/><circle cx="50" cy="50" r="37"/></svg>
      <div class="agent-core"><span><IconSparkles :size="20"/></span><b>GIS AGENT</b><small>ORCHESTRATOR</small></div>
      <button v-for="project in visible" :key="project.id" :style="{left:`${project.x}%`,top:`${project.y}%`}" :class="['project-signal',project.stage.toLowerCase(),{selected:selected?.id===project.id}]" @click="selectedId=project.id"><i/><span><b>{{ project.name }}</b><small>{{ stageLabels[project.stage] }} · {{ project.progress }}%</small></span></button>
      <aside v-if="selected" class="project-inspector"><span>{{ selected.industry }} · {{ selected.customerName }}</span><b>{{ selected.name }}</b><div><em>{{ stageLabels[selected.stage] }}</em><strong>{{ selected.progress }}%</strong></div><RouterLink :to="`/projects/${selected.id}`">进入工作台 <IconArrowUpRight :size="13"/></RouterLink></aside>
      <div v-if="!visible.length" class="no-project">当前阶段暂无项目</div>
    </div>
  </div>
</template>

<style scoped>
.project-canvas{height:100%;min-height:390px;display:grid;grid-template-rows:38px 1fr}.canvas-toolbar{display:flex;align-items:center;justify-content:space-between}.canvas-toolbar>span{font-weight:650}.canvas-toolbar>div{display:flex;gap:4px;max-width:72%;overflow:auto}.canvas-toolbar button{height:25px;padding:0 8px;border:1px solid transparent;border-radius:5px;color:var(--text-3);background:transparent;font-size:8px;white-space:nowrap}.canvas-toolbar button.active{color:var(--cyan);border-color:var(--line-strong);background:rgba(18,119,180,.1)}.network{position:relative;min-height:0;overflow:hidden;border:1px solid rgba(59,139,186,.11);border-radius:8px;background:radial-gradient(circle at 50% 50%,rgba(24,148,204,.12),transparent 22%),linear-gradient(rgba(42,127,176,.025) 1px,transparent 1px),linear-gradient(90deg,rgba(42,127,176,.025) 1px,transparent 1px);background-size:auto,25px 25px,25px 25px}.network>svg{position:absolute;inset:0;width:100%;height:100%}.network line{stroke:rgba(39,185,225,.2);stroke-width:.12;stroke-dasharray:1 1}.network circle{fill:none;stroke:rgba(41,164,211,.11);stroke-width:.14;stroke-dasharray:1.5 2}.agent-core{position:absolute;left:50%;top:50%;width:102px;height:102px;transform:translate(-50%,-50%);display:grid;place-items:center;align-content:center;gap:4px;border:1px solid rgba(42,210,235,.34);border-radius:50%;background:radial-gradient(circle,rgba(19,139,198,.3),rgba(2,15,24,.96) 68%);box-shadow:0 0 35px rgba(24,176,220,.14)}.agent-core span{color:var(--cyan)}.agent-core b{font-size:9px;letter-spacing:1px}.agent-core small{color:var(--text-3);font-size:6px;letter-spacing:1px}.project-signal{position:absolute;transform:translate(-50%,-50%);max-width:150px;min-height:34px;padding:5px 8px;display:flex;align-items:center;gap:6px;border:1px solid rgba(68,137,178,.22);border-radius:7px;color:var(--text-2);background:rgba(3,17,27,.9);text-align:left;will-change:transform,opacity}.project-signal:hover,.project-signal.selected{border-color:rgba(44,201,231,.55);color:var(--text-1);z-index:3;box-shadow:0 0 18px rgba(23,162,213,.12)}.project-signal>i{width:7px;height:7px;flex:0 0 auto;border-radius:50%;background:var(--primary);box-shadow:0 0 8px var(--primary)}.project-signal.review>i,.project-signal.delivered>i{background:var(--success)}.project-signal span{display:grid;gap:3px;min-width:0}.project-signal b{overflow:hidden;text-overflow:ellipsis;white-space:nowrap;font-size:8px}.project-signal small{color:var(--text-3);font-size:7px}.project-inspector{position:absolute;right:10px;bottom:10px;width:195px;padding:11px;border:1px solid rgba(48,174,220,.27);border-radius:8px;background:rgba(2,13,22,.94);box-shadow:0 15px 34px rgba(0,0,0,.26)}.project-inspector>span{color:var(--text-3);font-size:7px}.project-inspector>b{display:block;margin:5px 0 9px;font-size:10px}.project-inspector>div{display:flex;justify-content:space-between}.project-inspector em{font-style:normal;color:var(--primary-2);font-size:8px}.project-inspector strong{color:var(--success);font-size:9px}.project-inspector a{margin-top:9px;display:flex;justify-content:flex-end;align-items:center;gap:4px;color:var(--cyan);font-size:8px}.no-project{position:absolute;inset:0;display:grid;place-items:center;color:var(--text-3)}
</style>
