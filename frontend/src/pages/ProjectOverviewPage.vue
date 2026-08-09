<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { IconArrowsMaximize, IconCalendar, IconLayersIntersect, IconMap2, IconUsers } from '@tabler/icons-vue'
import AppShell from '@/components/layout/AppShell.vue'
import ProjectTabs from '@/components/layout/ProjectTabs.vue'
import CesiumProjectScene from '@/components/scene/CesiumProjectScene.vue'
import { api } from '@/api/services'
import type { ProjectDetail } from '@/api/contracts'

const route = useRoute()
const project = ref<ProjectDetail | null>(null)
const sceneMode = ref<'2D' | '3D'>('3D')
const sceneHost = ref<HTMLElement | null>(null)
const cesiumScene = ref<InstanceType<typeof CesiumProjectScene> | null>(null)
const businessLayersVisible = ref(true)
const toggleLayers = () => {
  businessLayersVisible.value = !businessLayersVisible.value
  cesiumScene.value?.setBusinessLayersVisible(businessLayersVisible.value)
}
const resetScene = () => cesiumScene.value?.resetCamera()
const toggleFullscreen = async () => {
  if (!sceneHost.value) return
  if (document.fullscreenElement) await document.exitFullscreen()
  else await sceneHost.value.requestFullscreen()
}
onMounted(async () => { project.value = await api.project(String(route.params.id)) })
</script>
<template>
  <AppShell :title="project?.name || '项目作战室'" :subtitle="project ? `${project.customerName} · ${project.projectCode}` : '正在加载项目'">
    <ProjectTabs />
    <div class="war-room">
      <aside class="project-info tech-panel">
        <div class="panel-title"><span>项目基本信息</span><span class="status-dot" /></div>
        <dl><dt>项目名称</dt><dd>{{ project?.name }}</dd><dt>客户单位</dt><dd>{{ project?.customerName }}</dd><dt>所属行业</dt><dd>{{ project?.industry }}</dd><dt>项目负责人</dt><dd>{{ project?.ownerName }}</dd><dt>交付日期</dt><dd>{{ project?.deliveryDeadline }}</dd><dt>当前阶段</dt><dd class="score">方案生成</dd></dl>
        <div class="project-progress"><div class="ring"><strong>{{ project?.progress ?? 0 }}%</strong></div><div><b>项目进度</b><span v-for="step in ['需求分析','产品匹配','知识检索','方案生成','交付验收']" :key="step"><i :class="{ done: ['需求分析','产品匹配','知识检索'].includes(step) }" />{{ step }}</span></div></div>
      </aside>
      <section ref="sceneHost" class="scene tech-panel">
        <div class="scene-tools"><div class="mode"><button :class="{ active: sceneMode === '2D' }" @click="sceneMode = '2D'">2D</button><button :class="{ active: sceneMode === '3D' }" @click="sceneMode = '3D'">3D</button></div><button :class="{ active: businessLayersVisible }" :disabled="sceneMode !== '3D'" :title="businessLayersVisible ? '隐藏业务图层' : '显示业务图层'" @click="toggleLayers"><IconLayersIntersect :size="16" /></button><button :disabled="sceneMode !== '3D'" title="复位三维场景" @click="resetScene"><IconMap2 :size="16" /></button><button title="切换全屏" @click="toggleFullscreen"><IconArrowsMaximize :size="16" /></button></div>
        <CesiumProjectScene v-if="sceneMode === '3D'" ref="cesiumScene" />
        <div v-else class="map-2d"><img src="/assets/images/china-project-network.png" alt="项目二维GIS地图" /><span>XX市国土空间基础信息平台</span></div>
        <div class="scene-caption"><span><i class="status-dot" /> 三维场景已连接</span><span>CGCS2000 · 1:5000</span></div>
      </section>
      <aside class="collaboration tech-panel">
        <div class="panel-title"><span>项目协作</span><IconUsers :size="17" /></div>
        <div class="members"><div v-for="(name, i) in project?.collaboratorNames" :key="name"><span class="avatar">{{ name[0] }}</span><p><b>{{ name }}</b><small>{{ ['项目负责人','产品工程师','GIS工程师','方案工程师'][i] }}</small></p><span>›</span></div></div>
        <div class="activity-title"><IconCalendar :size="15" /> 协作动态</div>
        <ul><li><time>10:30</time><span>李园上传产品配置清单</span></li><li><time>10:05</time><span>王玉更新需求分析结果</span></li><li><time>09:42</time><span>陈云完成知识检索</span></li><li><time>昨天</time><span>张文博创建项目</span></li></ul>
        <RouterLink :to="`/projects/${route.params.id}/requirements`" class="primary-button">进入需求分析</RouterLink>
      </aside>
    </div>
  </AppShell>
</template>
<style scoped>
.war-room { display: grid; grid-template-columns: 230px minmax(450px, 1fr) 220px; gap: 10px; height: calc(100vh - 152px); min-height: 560px; }.project-info, .collaboration { padding: 14px; overflow: auto; }.project-info dl { display: grid; grid-template-columns: 66px 1fr; gap: 12px 8px; font-size: 11px; margin: 18px 0; }.project-info dt { color: var(--text-3); }.project-info dd { margin: 0; color: var(--text-2); }
.project-progress { border-top: 1px solid var(--line); padding-top: 18px; display: flex; gap: 12px; align-items: center; }.ring { width: 72px; height: 72px; flex: 0 0 72px; display: grid; place-items: center; border-radius: 50%; background: conic-gradient(var(--primary) 68%, rgba(86,143,183,.14) 0); position: relative; }.ring::after { content: ''; position: absolute; inset: 7px; border-radius: 50%; background: var(--bg-2); }.ring strong { z-index: 1; }.project-progress > div:last-child { display: grid; gap: 7px; font-size: 10px; color: var(--text-3); }.project-progress span { display: flex; align-items: center; gap: 5px; }.project-progress i { width: 5px; height: 5px; border-radius: 50%; background: var(--text-3); }.project-progress i.done { background: var(--success); box-shadow: 0 0 7px var(--success); }
.scene { min-height: 0; position: relative; overflow: hidden; }.scene:fullscreen{background:var(--bg-1)}.scene-tools { position: absolute; z-index: 5; right: 10px; top: 10px; display: flex; gap: 5px; }.scene-tools button { width: 33px; height: 30px; border: 1px solid var(--line); color: var(--text-2); background: rgba(2,12,20,.82); border-radius: 4px; display: grid; place-items: center; }.scene-tools>button.active{color:var(--cyan);border-color:var(--primary)}.scene-tools>button:disabled{opacity:.4;cursor:not-allowed}.mode { display: flex; }.mode button { border-radius: 0; }.mode button:first-child { border-radius: 4px 0 0 4px; }.mode button:last-child { border-radius: 0 4px 4px 0; }.mode button.active { background: var(--primary); color: white; }.map-2d { height: 100%; display: grid; place-items: center; position: relative; }.map-2d img { width: 100%; height: 100%; object-fit: contain; }.map-2d span { position: absolute; padding: 7px 10px; background: rgba(3,17,29,.86); border: 1px solid var(--primary); border-radius: 4px; }
.scene-caption { position: absolute; bottom: 0; inset-inline: 0; height: 36px; padding: 0 12px; background: rgba(2,9,15,.82); display: flex; align-items: center; justify-content: space-between; color: var(--text-3); font-size: 10px; }
.members { margin: 13px 0; }.members > div { min-height: 49px; display: grid; grid-template-columns: 30px 1fr 12px; align-items: center; gap: 8px; border-bottom: 1px solid var(--line); }.avatar { width: 28px; height: 28px; border-radius: 50%; display: grid; place-items: center; background: linear-gradient(145deg,#1f7fe5,#4ce2db); color: white; }.members p { display: grid; margin: 0; }.members small { color: var(--text-3); margin-top: 3px; font-size: 10px; }.activity-title { display: flex; gap: 6px; color: var(--text-2); margin: 15px 0 8px; }.collaboration ul { padding: 0; list-style: none; display: grid; gap: 12px; }.collaboration li { display: grid; grid-template-columns: 38px 1fr; gap: 6px; font-size: 10px; }.collaboration time { color: var(--text-3); }.collaboration li span { color: var(--text-2); }.collaboration .primary-button { width: 100%; margin-top: 10px; }
@media (max-width: 1200px) { .war-room { grid-template-columns: 220px 1fr; }.collaboration { display: none; } }
</style>
