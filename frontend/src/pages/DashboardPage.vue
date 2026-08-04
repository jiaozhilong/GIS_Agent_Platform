<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { IconBrain, IconBooks, IconFileText, IconFolders, IconRobot, IconShieldCheck } from '@tabler/icons-vue'
import AppShell from '@/components/layout/AppShell.vue'
import MetricCard from '@/components/charts/MetricCard.vue'
import ProjectStatusChart from '@/components/charts/ProjectStatusChart.vue'
import AgentProgressChart from '@/components/charts/AgentProgressChart.vue'
import { api } from '@/api/services'
import type { DashboardSummary } from '@/api/contracts'

const summary = ref<DashboardSummary | null>(null)
onMounted(async () => { summary.value = await api.dashboard() })
</script>
<template>
  <AppShell title="总览看板" subtitle="GIS 解决方案智能作战态势">
    <div class="dashboard-grid">
      <section class="metrics">
        <MetricCard label="项目总数" :value="summary?.projectCount ?? '—'" trend="同比 +12%" :icon="IconFolders" />
        <MetricCard label="需求分析" :value="summary?.requirementTaskCount ?? '—'" trend="进行中 9项" :icon="IconBrain" />
        <MetricCard label="方案生成" :value="summary?.proposalCount ?? '—'" trend="本周 +8份" :icon="IconFileText" />
        <MetricCard label="知识库文档" :value="(summary?.knowledgeChunkCount ?? 0).toLocaleString()" trend="今日 +32段" :icon="IconBooks" />
        <MetricCard label="智能体" :value="summary?.runningAgentCount ?? '—'" trend="运行中" :icon="IconRobot" tone="amber" />
        <MetricCard label="系统状态" :value="summary?.systemStatus === 'HEALTHY' ? '正常' : '降级'" trend="全部服务可用" :icon="IconShieldCheck" tone="green" />
      </section>
      <section class="map-card tech-panel">
        <div class="panel-title"><span>项目分布地图</span><span class="secondary">实时项目网络</span></div>
        <div class="china-map"><img src="/assets/images/china-project-network.png" alt="全国项目分布网络地图" /><div class="map-legend"><span><i class="blue" /> 交付中</span><span><i class="green" /> 方案中</span><span><i class="amber" /> 需求分析</span></div></div>
      </section>
      <section class="status-card tech-panel"><div class="panel-title"><span>项目类型分布</span><span class="secondary">共128项</span></div><ProjectStatusChart /></section>
      <section class="project-list tech-panel">
        <div class="panel-title"><span>近期项目动态</span><RouterLink to="/projects" class="secondary">查看全部</RouterLink></div>
        <RouterLink v-for="project in summary?.projects" :key="project.id" :to="`/projects/${project.id}`" class="project-row"><span><b>{{ project.name }}</b><small>{{ project.customerName }}</small></span><span class="stage">{{ project.stage }}</span><time>{{ project.updatedAt.slice(5, 16).replace('T', ' ') }}</time></RouterLink>
      </section>
      <section class="agent-card tech-panel"><div class="panel-title"><span>任务执行情况</span><span class="secondary">Agent 作业</span></div><AgentProgressChart /></section>
      <section class="knowledge-card tech-panel">
        <div class="panel-title"><span>知识库热度 TOP5</span><RouterLink to="/knowledge" class="secondary">管理知识库</RouterLink></div>
        <ol><li v-for="(item, i) in ['SuperMap iServer产品文档','自然资源一张图解决方案','数字孪生城市案例','3D GIS技术文档','GIS平台总体架构']" :key="item"><span>{{ i + 1 }}</span><b>{{ item }}</b><div class="progress-track"><div class="progress-value" :style="{ width: `${92 - i * 11}%` }" /></div><em>{{ 1245 - i * 139 }}</em></li></ol>
      </section>
    </div>
  </AppShell>
</template>
<style scoped>
.dashboard-grid { display: grid; grid-template-columns: minmax(0, 1.75fr) minmax(270px, .75fr); gap: 12px; min-height: calc(100vh - 104px); }
.metrics { grid-column: 1 / -1; display: grid; grid-template-columns: repeat(6, minmax(115px, 1fr)); gap: 10px; }.map-card { min-height: 390px; padding: 13px; }.panel-title { height: 26px; }
.china-map { height: calc(100% - 26px); position: relative; overflow: hidden; }.china-map img { width: 100%; height: 100%; object-fit: contain; }.map-legend { position: absolute; left: 12px; bottom: 10px; display: flex; gap: 14px; color: var(--text-3); font-size: 11px; }.map-legend i { display: inline-block; width: 7px; height: 7px; border-radius: 50%; margin-right: 4px; }.blue { background: var(--primary); }.green { background: var(--success); }.amber { background: var(--warning); }
.status-card, .agent-card { min-height: 220px; padding: 13px; }.status-card > :last-child, .agent-card > :last-child { height: 180px; }.project-list, .knowledge-card { padding: 13px; }
.project-row { min-height: 56px; border-top: 1px solid var(--line); display: grid; grid-template-columns: minmax(0, 1fr) 100px 76px; align-items: center; gap: 12px; color: var(--text-2); }.project-row:first-of-type { margin-top: 8px; }.project-row span:first-child { display: grid; gap: 4px; }.project-row b { color: var(--text-1); font-size: 12px; font-weight: 560; }.project-row small, .project-row time { color: var(--text-3); font-size: 11px; }.stage { color: var(--warning); font-size: 11px; }
.knowledge-card ol { list-style: none; padding: 4px 0 0; margin: 0; }.knowledge-card li { display: grid; grid-template-columns: 24px minmax(150px, 1fr) minmax(80px, .8fr) 45px; align-items: center; gap: 9px; min-height: 36px; font-size: 11px; }.knowledge-card li > span { color: var(--primary-2); }.knowledge-card li b { font-weight: 500; }.knowledge-card li em { color: var(--text-3); font-style: normal; text-align: right; }
@media (max-width: 1180px) { .metrics { grid-template-columns: repeat(3, 1fr); }.dashboard-grid { grid-template-columns: 1fr; }.metrics, .map-card { grid-column: auto; } }
</style>
