<script setup lang="ts">
import type { Component } from 'vue'
defineProps<{ label: string; value: string | number; trend?: string; icon?: Component; tone?: 'blue' | 'green' | 'amber' }>()
</script>
<template>
  <div :class="['metric', tone || 'blue']"><div class="metric-head"><span>{{ label }}</span><component :is="icon" v-if="icon" :size="17" /></div><strong>{{ value }}</strong><small v-if="trend">{{ trend }}</small></div>
</template>
<style scoped>
.metric { min-height: 84px; border: 1px solid var(--line); border-radius: 7px; padding: 13px 14px; background: linear-gradient(145deg, rgba(11,35,52,.9), rgba(4,16,26,.92)); position: relative; overflow: hidden; }
.metric::after { content: ''; position: absolute; inset: auto -20px -34px auto; width: 70px; height: 70px; border-radius: 50%; background: rgba(18,135,255,.09); filter: blur(10px); }
.metric-head { display: flex; justify-content: space-between; color: var(--text-2); font-size: 12px; }.metric strong { display: block; margin-top: 8px; font-size: 24px; letter-spacing: -.5px; }.metric small { color: var(--success); position: absolute; right: 12px; bottom: 13px; }.metric.green strong { color: var(--success); }.metric.amber strong { color: var(--warning); }
</style>
