<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { IconArrowRight, IconCheck, IconExternalLink, IconRefresh, IconSparkles } from '@tabler/icons-vue'
import AppShell from '@/components/layout/AppShell.vue'
import ProjectTabs from '@/components/layout/ProjectTabs.vue'
import ProductGalaxy from '@/components/scene/ProductGalaxy.vue'
import { api } from '@/api/services'
import type { ProductCatalogItem, ProductMatch } from '@/api/contracts'

const route = useRoute()
const matches = ref<ProductMatch[]>([])
const catalog = ref<ProductCatalogItem[]>([])
const selected = ref<string[]>([])
const selectedProductId = ref('')
const loading = ref(false)
const error = ref('')
const activeCategory = ref('全部产品')
const categories = computed(() => ['全部产品', ...new Set(catalog.value.map(item => item.category))])
const filteredMatches = computed(() => activeCategory.value === '全部产品' ? matches.value : matches.value.filter(item => item.productFamily === activeCategory.value))
const selectedCatalog = computed(() => catalog.value.find(item => item.id === selectedProductId.value))
const matchMap = computed(() => new Map(matches.value.map(item => [item.productId, item])))
const run = async () => {
  loading.value = true; error.value = ''
  try { matches.value = await api.matchProducts(String(route.params.id)); selected.value = matches.value.filter(item => item.recommended).map(item => item.productId); selectedProductId.value = matches.value[0]?.productId || '' }
  catch (e) { error.value = e instanceof Error ? e.message : '产品匹配失败' }
  finally { loading.value = false }
}
const focusProduct = (product: ProductCatalogItem) => { selectedProductId.value = product.id; activeCategory.value = product.category }
const toggle = (id: string) => { selectedProductId.value = id; selected.value = selected.value.includes(id) ? selected.value.filter(item => item !== id) : [...selected.value, id] }
const toggleProduct = (product: ProductCatalogItem) => { activeCategory.value = product.category; toggle(product.id) }
onMounted(async () => { try { catalog.value = await api.productCatalog(); await run() } catch (e) { error.value = e instanceof Error ? e.message : '产品体系加载失败' } })
</script>

<template>
  <AppShell title="产品 Agent" subtitle="基于项目需求与知识证据，在 SuperMap GIS 2026 产品宇宙中编排能力组合">
    <ProjectTabs />
    <p v-if="error" class="page-error">{{ error }}</p>
    <div class="match-layout">
      <aside class="filters tech-panel">
        <div class="agent-badge"><span><IconSparkles :size="17"/></span><div><b>Product Agent</b><small>{{ loading ? '正在推理产品组合' : `已分析 ${catalog.length} 个产品节点` }}</small></div></div>
        <div class="panel-title"><span>产品体系</span><button :disabled="loading" title="重新匹配" @click="run"><IconRefresh :size="15"/></button></div>
        <button v-for="category in categories" :key="category" :class="{active:activeCategory===category}" @click="activeCategory=category"><span>{{ category }}</span><em>{{ category==='全部产品'?catalog.length:catalog.filter(item=>item.category===category).length }}</em></button>
        <div class="match-summary"><span>需求命中</span><b>{{ matches.length }}</b><span>推荐组合</span><b>{{ selected.length }}</b></div>
        <button class="secondary-button" :disabled="loading" @click="run">{{ loading ? 'Agent 运行中...' : '重新运行产品 Agent' }}</button>
      </aside>

      <section class="galaxy-panel tech-panel"><div class="panel-title"><span>SuperMap GIS 2026 产品能力宇宙</span><span class="tag">点击节点加入/移出推荐组合</span></div><ProductGalaxy :catalog="catalog" :matches="matches" :selected-product-id="selectedProductId" :selected-product-ids="selected" @select="focusProduct" @toggle="toggleProduct"/></section>

      <aside class="match-result tech-panel">
        <div class="panel-title"><span>Agent 推荐组合</span><strong class="score">{{ matches.length }} 项</strong></div>
        <div v-if="selectedCatalog" class="selected-product"><span>{{ selectedCatalog.category }}</span><b>{{ selectedCatalog.name }}</b><p>{{ selectedCatalog.description }}</p><a :href="selectedCatalog.officialUrl" target="_blank">查看官方 2026 产品体系 <IconExternalLink :size="12"/></a></div>
        <div class="result-list"><button v-for="item in filteredMatches" :key="item.productId" :class="['product-row',{selected:selected.includes(item.productId),focused:selectedProductId===item.productId}]" @click="toggle(item.productId)"><span class="check"><IconCheck v-if="selected.includes(item.productId)" :size="13"/></span><p><b>{{ item.productName }}</b><small>{{ item.productFamily }} · {{ item.matchedCapabilities.join(' / ') }}</small></p><strong>{{ item.matchScore }}%</strong></button><p v-if="!loading&&!filteredMatches.length" class="empty-result">该产品域未被当前需求命中；可在能力图中查看完整产品。</p></div>
        <div class="selection"><span>已选 {{ selected.length }} 个产品</span><div><i v-for="id in selected" :key="id" :title="catalog.find(item=>item.id===id)?.name"/></div></div>
        <RouterLink :to="`/projects/${route.params.id}/retrieval`" class="primary-button">确认组合并检索证据 <IconArrowRight :size="16"/></RouterLink>
      </aside>
    </div>
  </AppShell>
</template>

<style scoped>
.match-layout{display:grid;grid-template-columns:205px minmax(640px,1fr)310px;gap:10px;min-height:calc(100vh - 158px)}.filters,.galaxy-panel,.match-result{padding:13px}.filters{display:flex;flex-direction:column;gap:6px}.agent-badge{display:flex;align-items:center;gap:9px;padding:10px;margin-bottom:6px;border:1px solid rgba(39,190,231,.2);border-radius:8px;background:rgba(17,107,166,.08)}.agent-badge>span{width:31px;height:31px;display:grid;place-items:center;border-radius:9px;color:var(--cyan);background:rgba(23,160,215,.13)}.agent-badge>div{display:grid;gap:4px}.agent-badge small{color:var(--text-3);font-size:8px}.filters>.panel-title{margin:6px 0}.filters>.panel-title button{border:0;background:transparent;color:var(--text-3)}.filters>button:not(.secondary-button){min-height:33px;padding:0 9px;display:flex;justify-content:space-between;align-items:center;border:1px solid transparent;border-radius:6px;background:transparent;color:var(--text-2);font-size:10px}.filters>button em{font-style:normal;color:var(--text-3)}.filters>button.active{background:rgba(25,134,255,.14);border-color:var(--line);color:var(--primary-2)}.match-summary{margin-top:8px;padding:10px;display:grid;grid-template-columns:1fr auto;gap:8px;border:1px solid var(--line);border-radius:7px;color:var(--text-3);font-size:9px}.match-summary b{color:var(--text-1)}.filters .secondary-button{margin-top:auto;padding:0 8px;font-size:10px}.galaxy-panel{display:grid;grid-template-rows:30px 1fr;min-height:0;overflow:hidden}.match-result{display:flex;flex-direction:column;gap:10px;min-height:0;overflow:hidden}.selected-product{padding:11px;border:1px solid rgba(39,171,224,.22);border-radius:7px;background:rgba(18,94,145,.08)}.selected-product>span{color:var(--cyan);font-size:8px;letter-spacing:1px}.selected-product>b{display:block;margin:5px 0;font-size:11px}.selected-product p{margin:0;color:var(--text-3);font-size:9px;line-height:1.5}.selected-product a{margin-top:7px;display:flex;align-items:center;gap:4px;color:var(--primary-2);font-size:8px}.result-list{flex:1;overflow:auto;display:grid;align-content:start;gap:6px}.product-row{width:100%;display:grid;grid-template-columns:20px 1fr 38px;align-items:center;gap:7px;border:1px solid var(--line);border-radius:7px;padding:9px;background:rgba(8,29,44,.35);color:var(--text-1);text-align:left}.product-row.selected{border-color:rgba(44,230,160,.35);background:rgba(23,147,106,.08)}.product-row.focused{box-shadow:inset 2px 0 var(--cyan)}.check{width:16px;height:16px;border:1px solid var(--line-strong);border-radius:4px;display:grid;place-items:center;color:var(--success)}.product-row p{display:grid;margin:0;gap:4px}.product-row b{font-size:10px}.product-row small{font-size:8px;color:var(--text-3);line-height:1.4}.product-row strong{font-size:10px;color:var(--success)}.empty-result{color:var(--text-3);font-size:10px;text-align:center;line-height:1.6}.selection{display:flex;justify-content:space-between;align-items:center;color:var(--text-3);font-size:9px}.selection>div{display:flex;gap:3px}.selection i{width:6px;height:6px;border-radius:50%;background:var(--success);box-shadow:0 0 6px rgba(44,230,160,.5)}.match-result>.primary-button{width:100%;font-size:10px}.page-error{padding:9px 12px;color:var(--danger);border:1px solid rgba(255,90,110,.25)}@media(max-width:1250px){.match-layout{grid-template-columns:185px 1fr}.match-result{grid-column:1/-1;max-height:420px}}@media(max-width:820px){.match-layout{grid-template-columns:1fr}}
</style>
