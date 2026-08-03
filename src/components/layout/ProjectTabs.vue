<script setup lang="ts">
import { useRoute } from 'vue-router'
const route = useRoute()
const id = String(route.params.id || 'prj-001')
const tabs = [
  { label: '项目概览', to: `/projects/${id}` },
  { label: '需求分析', to: `/projects/${id}/requirements` },
  { label: '产品匹配', to: `/projects/${id}/products` },
  { label: '知识检索', to: `/projects/${id}/retrieval` },
  { label: '方案生成', to: `/projects/${id}/proposal` }
]
</script>
<template><div class="project-tabs"><RouterLink v-for="tab in tabs" :key="tab.to" :to="tab.to" :class="{ active: route.path === tab.to }">{{ tab.label }}</RouterLink></div></template>
<style scoped>
.project-tabs { height: 48px; display: flex; align-items: end; gap: 12px; border-bottom: 1px solid var(--line); margin: -16px -16px 16px; padding: 0 18px; background: rgba(4,14,23,.56); }
.project-tabs a { height: 48px; padding: 0 13px; display: grid; place-items: center; color: var(--text-3); border-bottom: 2px solid transparent; }
.project-tabs a:hover { color: var(--text-1); }.project-tabs a.active { color: #e6f6ff; border-bottom-color: var(--primary); }
</style>
