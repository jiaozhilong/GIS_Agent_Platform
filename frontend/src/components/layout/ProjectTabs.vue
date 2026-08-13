<script setup lang="ts">
import { useRoute } from 'vue-router'
import { IconArrowLeft, IconBrain, IconDatabaseSearch, IconLayoutDashboard, IconPresentationAnalytics, IconTopologyStar3 } from '@tabler/icons-vue'
const route = useRoute()
const id = String(route.params.id || '')
const tabs = [
  { label:'项目上下文',caption:'Context',icon:IconLayoutDashboard,to:`/projects/${id}` },
  { label:'需求 Agent',caption:'Analyze',icon:IconBrain,to:`/projects/${id}/requirements` },
  { label:'产品 Agent',caption:'Match',icon:IconTopologyStar3,to:`/projects/${id}/products` },
  { label:'证据检索',caption:'Ground',icon:IconDatabaseSearch,to:`/projects/${id}/retrieval` },
  { label:'方案生成',caption:'Generate',icon:IconPresentationAnalytics,to:`/projects/${id}/proposal` },
]
</script>
<template><div class="workbench-tabs"><RouterLink to="/projects" class="back"><IconArrowLeft :size="15"/><span>项目空间</span></RouterLink><div class="flow"><RouterLink v-for="(tab,index) in tabs" :key="tab.to" :to="tab.to" :class="{active:route.path===tab.to}"><i>{{ index+1 }}</i><component :is="tab.icon" :size="15"/><span><b>{{tab.label}}</b><small>{{tab.caption}}</small></span></RouterLink></div><span class="flow-status"><i/>WORKBENCH ACTIVE</span></div></template>
<style scoped>
.workbench-tabs{height:58px;margin:-16px -16px 14px;padding:0 16px;display:flex;align-items:center;gap:14px;border-bottom:1px solid var(--line);background:linear-gradient(90deg,rgba(4,18,29,.94),rgba(5,23,36,.68))}.back{height:34px;padding-right:13px;display:flex;align-items:center;gap:6px;border-right:1px solid var(--line);color:var(--text-3);font-size:9px}.flow{height:100%;display:flex;align-items:center;gap:3px;overflow:auto}.flow>a{position:relative;min-width:116px;height:42px;padding:0 9px;display:grid;grid-template-columns:18px 18px 1fr;align-items:center;gap:5px;border:1px solid transparent;border-radius:7px;color:var(--text-3)}.flow>a:after{content:'›';position:absolute;right:-6px;color:#35546a}.flow>a:last-child:after{display:none}.flow>a>i{width:17px;height:17px;display:grid;place-items:center;border:1px solid var(--line);border-radius:50%;font-style:normal;font-size:7px}.flow>a>span{display:grid;gap:3px}.flow b{font-size:9px;font-weight:550}.flow small{font-size:7px;text-transform:uppercase;letter-spacing:.8px}.flow>a.active{border-color:rgba(43,180,228,.3);color:#e7fbff;background:rgba(20,118,180,.12)}.flow>a.active>i{border-color:var(--cyan);color:var(--cyan);box-shadow:0 0 8px rgba(39,225,255,.2)}.flow-status{margin-left:auto;display:flex;align-items:center;gap:6px;color:#496a7f;font-size:7px;letter-spacing:1px;white-space:nowrap}.flow-status i{width:6px;height:6px;border-radius:50%;background:var(--success);box-shadow:0 0 8px var(--success)}@media(max-width:1100px){.flow-status{display:none}.flow>a{min-width:97px}.flow>a>svg{display:none}.flow>a{grid-template-columns:18px 1fr}}@media(max-width:760px){.back span,.flow small{display:none}.flow>a{min-width:66px}}
</style>
