<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { IconDownload, IconEye, IconRefresh } from '@tabler/icons-vue'
import AppShell from '@/components/layout/AppShell.vue'
import { api } from '@/api/services'
import type { SolutionGenerationRun } from '@/api/contracts'

const router = useRouter()
const records = ref<SolutionGenerationRun[]>([])
const loading = ref(false)
const error = ref('')
const load = async () => { loading.value = true; try { records.value = await api.solutionRuns() } catch (e) { error.value = e instanceof Error ? e.message : '生成记录加载失败' } finally { loading.value = false } }
const view = (item: SolutionGenerationRun) => router.push(`/projects/${item.projectId}/proposal?runId=${item.id}`)
const download = (item: SolutionGenerationRun) => {
  const content = item.sections.map(section => `${section.title}\n\n${section.content}`).join('\n\n')
  const url = URL.createObjectURL(new Blob([content], { type: 'text/plain;charset=utf-8' }))
  const link = document.createElement('a'); link.href = url; link.download = `GIS方案-${item.id}.txt`; link.click(); URL.revokeObjectURL(url)
}
onMounted(load)
</script>
<template>
  <AppShell title="生成记录" subtitle="追踪真实方案任务、模型、证据覆盖率和生成结果">
    <div class="record-actions"><button class="secondary-button" :disabled="loading" @click="load"><IconRefresh :size="15"/>刷新</button></div><p v-if="error" class="error">{{error}}</p>
    <div class="record-table tech-panel"><div class="head"><span>任务编号</span><span>项目 ID</span><span>模型</span><span>证据覆盖</span><span>章节</span><span>状态</span><span>生成时间</span><span>操作</span></div><div v-if="!loading&&!records.length" class="empty">暂无真实生成记录</div><div v-for="item in records" :key="item.id" class="row"><code>{{item.id.slice(0,13)}}</code><span>{{item.projectId}}</span><span>{{item.modelName||'等待模型'}}</span><span>{{Math.round(item.evidenceCoverage*100)}}%</span><span>{{item.sections.length}}</span><span :class="['status',item.status.toLowerCase()]">{{item.status}}</span><time>{{item.createdAt.slice(0,19).replace('T',' ')}}</time><div><button title="查看" @click="view(item)"><IconEye :size="16"/></button><button v-if="item.status==='SUCCEEDED'" title="下载文本" @click="download(item)"><IconDownload :size="16"/></button><button v-else title="刷新" @click="load"><IconRefresh :size="16"/></button></div></div></div>
  </AppShell>
</template>
<style scoped>
.record-actions{display:flex;justify-content:flex-end;margin-bottom:10px}.error{padding:10px;color:var(--danger);border:1px solid rgba(255,90,110,.25)}.record-table{min-width:920px;overflow:hidden}.head,.row{display:grid;grid-template-columns:120px minmax(230px,1fr) 150px 85px 55px 90px 155px 85px;gap:10px;align-items:center;padding:0 15px}.head{height:45px;color:var(--text-3);font-size:11px;background:rgba(14,45,66,.18)}.row{min-height:62px;border-top:1px solid var(--line);color:var(--text-2)}.row code{color:var(--primary-2)}.row time{color:var(--text-3);font-size:11px}.status{font-size:11px}.status.succeeded{color:var(--success)}.status.running,.status.pending{color:var(--warning)}.status.failed{color:var(--danger)}.row>div{display:flex;gap:4px}.row button{width:30px;height:30px;border:1px solid var(--line);border-radius:4px;background:transparent;color:var(--text-2);display:grid;place-items:center}.row button:hover{color:var(--cyan);border-color:var(--primary)}.empty{padding:50px;text-align:center;color:var(--text-3)}
</style>
