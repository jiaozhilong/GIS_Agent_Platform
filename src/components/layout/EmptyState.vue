<script setup lang="ts">import { IconInbox } from '@tabler/icons-vue'; defineProps<{ title: string; description: string }>()</script>
<template><div class="empty"><IconInbox :size="34" /><b>{{ title }}</b><span>{{ description }}</span><slot /></div></template>
<style scoped>.empty { min-height: 220px; display: grid; place-items: center; align-content: center; gap: 9px; color: var(--text-3); text-align: center; }.empty b { color: var(--text-2); }.empty span { max-width: 360px; }</style>
