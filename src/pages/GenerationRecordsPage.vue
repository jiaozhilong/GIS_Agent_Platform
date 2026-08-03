<script setup lang="ts">
import { ref } from 'vue'
import { IconDownload, IconEye, IconRefresh } from '@tabler/icons-vue'
import AppShell from '@/components/layout/AppShell.vue'
const records=ref([
 {id:'GEN-20260802-003',project:'XX市国土空间基础信息平台建设项目',type:'PPTX',version:'V3',status:'已完成',time:'2026-08-02 10:42',citations:36},
 {id:'GEN-20260802-002',project:'XX市国土空间基础信息平台建设项目',type:'DOCX',version:'V2',status:'已完成',time:'2026-08-02 10:18',citations:42},
 {id:'GEN-20260802-001',project:'水利数字孪生流域平台',type:'PPTX',version:'V1',status:'生成中',time:'2026-08-02 09:51',citations:28},
 {id:'GEN-20260801-006',project:'城市运行一网统管升级',type:'PDF',version:'V4',status:'失败',time:'2026-08-01 17:35',citations:0}
])
</script>
<template>
  <AppShell title="生成记录" subtitle="追踪方案任务、版本、引用依据和交付文件">
    <div class="record-table tech-panel"><div class="head"><span>任务编号</span><span>项目</span><span>格式</span><span>版本</span><span>引用</span><span>状态</span><span>生成时间</span><span>操作</span></div><div v-for="item in records" :key="item.id" class="row"><code>{{item.id}}</code><span>{{item.project}}</span><span class="tag">{{item.type}}</span><span>{{item.version}}</span><span>{{item.citations}}条</span><span :class="['status',item.status]">{{item.status}}</span><time>{{item.time}}</time><div><button title="查看"><IconEye :size="16"/></button><button v-if="item.status==='已完成'" title="下载"><IconDownload :size="16"/></button><button v-else title="重试"><IconRefresh :size="16"/></button></div></div></div>
  </AppShell>
</template>
<style scoped>
.record-table{min-width:920px;overflow:hidden}.head,.row{display:grid;grid-template-columns:150px minmax(250px,1fr) 70px 60px 70px 80px 150px 85px;gap:10px;align-items:center;padding:0 15px}.head{height:45px;color:var(--text-3);font-size:11px;background:rgba(14,45,66,.18)}.row{min-height:62px;border-top:1px solid var(--line);color:var(--text-2)}.row code{color:var(--primary-2)}.row time{color:var(--text-3);font-size:11px}.status{font-size:11px}.status.已完成{color:var(--success)}.status.生成中{color:var(--warning)}.status.失败{color:var(--danger)}.row>div{display:flex;gap:4px}.row button{width:30px;height:30px;border:1px solid var(--line);border-radius:4px;background:transparent;color:var(--text-2);display:grid;place-items:center}.row button:hover{color:var(--cyan);border-color:var(--primary)}
</style>
