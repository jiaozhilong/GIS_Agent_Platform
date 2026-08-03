<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { IconArrowRight, IconCheck, IconRefresh } from '@tabler/icons-vue'
import AppShell from '@/components/layout/AppShell.vue'
import ProjectTabs from '@/components/layout/ProjectTabs.vue'
import ProductGalaxy from '@/components/scene/ProductGalaxy.vue'
import { api } from '@/api/services'
import type { ProductMatch } from '@/api/contracts'

const route = useRoute(); const matches = ref<ProductMatch[]>([]); const selected = ref<string[]>([]); const loading = ref(false)
const run = async () => { loading.value = true; try { matches.value = await api.matchProducts(String(route.params.id)); selected.value = matches.value.filter(x => x.recommended).map(x => x.productId) } finally { loading.value = false } }
onMounted(run)
</script>
<template>
  <AppShell title="产品匹配" subtitle="基于需求图谱匹配 GIS 产品能力组合">
    <ProjectTabs />
    <div class="match-layout">
      <aside class="filters tech-panel"><div class="panel-title"><span>产品分类</span><button @click="run"><IconRefresh :size="15" /></button></div><button class="active">全部产品</button><button>GIS平台</button><button>三维GIS</button><button>空间分析</button><button>开发组件</button><button>运维管理</button><label>部署方式<select class="tech-select"><option>全部</option><option>私有化部署</option><option>云原生</option></select></label><label>产品版本<select class="tech-select"><option>推荐版本</option><option>全部版本</option></select></label><label>价格区间<select class="tech-select"><option>全部</option><option>基础配置</option><option>企业配置</option></select></label><button class="secondary-button" @click="run">重新匹配</button></aside>
      <section class="galaxy-panel tech-panel"><div class="panel-title"><span>3D 产品能力地图</span><span class="tag">需求驱动匹配</span></div><ProductGalaxy /></section>
      <aside class="match-result tech-panel"><div class="panel-title"><span>匹配质量</span><strong class="score">92%</strong></div><div class="quality"><span>功能匹配度</span><div class="progress-track"><div class="progress-value" style="width:96%" /></div><em>96%</em><span>部署匹配度</span><div class="progress-track"><div class="progress-value" style="width:90%" /></div><em>90%</em><span>成本匹配度</span><div class="progress-track"><div class="progress-value" style="width:89%" /></div><em>89%</em></div><h3>配置评估</h3><div v-for="item in matches" :key="item.productId" :class="['product-row',{selected:selected.includes(item.productId)}]" @click="selected.includes(item.productId)?selected=selected.filter(x=>x!==item.productId):selected.push(item.productId)"><span class="check"><IconCheck v-if="selected.includes(item.productId)" :size="13" /></span><p><b>{{ item.productName }}</b><small>{{ item.productFamily }} · {{ item.matchedCapabilities.join(' / ') }}</small></p><strong>{{ item.matchScore }}%</strong></div><RouterLink :to="`/projects/${route.params.id}/retrieval`" class="primary-button">确认产品组合 <IconArrowRight :size="16" /></RouterLink></aside>
    </div>
  </AppShell>
</template>
<style scoped>
.match-layout{display:grid;grid-template-columns:190px minmax(460px,1fr)270px;gap:10px;min-height:calc(100vh - 152px)}.filters,.galaxy-panel,.match-result{padding:14px}.filters{display:flex;flex-direction:column;gap:7px}.filters>.panel-title{margin-bottom:4px}.filters>.panel-title button{background:none;border:0;color:var(--text-3)}.filters>button:not(.secondary-button){height:34px;text-align:left;padding:0 10px;border:1px solid transparent;background:transparent;color:var(--text-2);border-radius:5px}.filters>button.active{background:rgba(25,134,255,.2);border-color:var(--line);color:var(--primary-2)}.filters label{display:grid;gap:6px;color:var(--text-3);font-size:11px;margin-top:5px}.filters .secondary-button{margin-top:auto}.galaxy-panel{display:grid;grid-template-rows:28px 1fr;min-height:0;overflow:hidden}.match-result{display:flex;flex-direction:column;gap:12px;overflow:auto}.quality{display:grid;grid-template-columns:72px 1fr 34px;align-items:center;gap:10px 7px;font-size:10px;color:var(--text-2)}.quality em{font-style:normal;color:var(--text-3)}.match-result h3{font-size:12px;margin:6px 0 0;border-top:1px solid var(--line);padding-top:13px}.product-row{display:grid;grid-template-columns:20px 1fr 38px;align-items:center;gap:7px;border:1px solid var(--line);border-radius:6px;padding:9px;background:rgba(8,29,44,.35)}.product-row.selected{border-color:rgba(34,150,237,.45);background:rgba(25,134,255,.1)}.check{width:16px;height:16px;border:1px solid var(--line-strong);border-radius:3px;display:grid;place-items:center;color:var(--cyan)}.product-row p{display:grid;margin:0;gap:4px}.product-row b{font-size:11px}.product-row small{font-size:9px;color:var(--text-3);line-height:1.4}.product-row strong{font-size:11px;color:var(--success)}.match-result>.primary-button{margin-top:auto}@media(max-width:1150px){.match-layout{grid-template-columns:170px 1fr}.match-result{grid-column:1/-1;display:grid;grid-template-columns:1fr 1fr}.match-result>.primary-button{grid-column:2}}
</style>
