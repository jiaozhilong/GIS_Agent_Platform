<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { gsap } from 'gsap'
import { IconAdjustments, IconBook2, IconDatabaseSearch, IconFileText, IconSearch, IconSparkles } from '@tabler/icons-vue'
import AppShell from '@/components/layout/AppShell.vue'
import { api } from '@/api/services'
import type { KnowledgeAssetMedia, KnowledgeBase, RetrievalResult } from '@/api/contracts'

const route = useRoute()
const page = ref<HTMLElement | null>(null)
const bases = ref<KnowledgeBase[]>([])
const selected = ref<string[]>([])
const query = ref(String(route.query.q || ''))
const topK = ref(8)
const threshold = ref(.2)
const result = ref<RetrievalResult | null>(null)
const loading = ref(false)
const error = ref('')
const recent = ref<string[]>(JSON.parse(localStorage.getItem('gis-agent-search-history') || '[]'))
const mediaUrls = reactive<Record<string, string>>({})
let motion: gsap.Context | undefined
const canRun = computed(() => query.value.trim().length >= 2 && selected.value.length > 0)

const animateHits = async () => {
  await nextTick()
  if (!page.value) return
  motion?.revert()
  motion = gsap.context(() => gsap.from('.hit-card', { autoAlpha: 0, y: 16, duration: .38, stagger: .055, ease: 'power2.out' }), page.value)
}
const ensureMediaUrl = async (media: KnowledgeAssetMedia) => {
  if (!media.contentUrl || mediaUrls[media.contentUrl]) return
  try { mediaUrls[media.contentUrl] = URL.createObjectURL(await api.knowledgeAssetBlob(media.contentUrl)) } catch { /* retrieval text remains available */ }
}
const hydrateAssetMedia = async () => {
  const media = result.value?.hits.flatMap(hit => [...(hit.assetContext?.relatedImages || []), ...(hit.assetContext?.relatedVideos || [])]) || []
  await Promise.all(media.slice(0, 24).map(ensureMediaUrl))
}
const run = async () => {
  if (!canRun.value) return
  loading.value = true
  error.value = ''
  try {
    result.value = await api.searchKnowledge({ query: query.value.trim(), knowledgeBaseIds: selected.value, topK: topK.value, similarityThreshold: threshold.value })
    recent.value = [query.value.trim(), ...recent.value.filter(item => item !== query.value.trim())].slice(0, 6)
    localStorage.setItem('gis-agent-search-history', JSON.stringify(recent.value))
    await Promise.all([animateHits(), hydrateAssetMedia()])
  } catch (e) { error.value = e instanceof Error ? e.message : '知识检索失败' }
  finally { loading.value = false }
}
const useRecent = (value: string) => { query.value = value; run() }

onMounted(async () => {
  try {
    bases.value = await api.knowledgeBases()
    selected.value = bases.value.filter(item => item.documentCount > 0).map(item => item.id)
    if (query.value.trim()) await run()
  } catch (e) { error.value = e instanceof Error ? e.message : '知识库加载失败' }
})
onUnmounted(() => {
  motion?.revert()
  Object.values(mediaUrls).forEach(URL.revokeObjectURL)
})
</script>

<template>
  <AppShell title="知识探索" subtitle="直接穿透 RAGFlow 知识库，定位可追溯的原始依据">
    <div ref="page" class="search-workspace">
      <section class="search-hero">
        <div class="hero-orb"><IconSparkles :size="25" /></div>
        <div><span class="eyebrow">KNOWLEDGE INTELLIGENCE</span><h2>想从知识库中找到什么？</h2><p>由 BGE-M3 完成语义召回，结果直接来自 RAGFlow 文档与切片。</p></div>
        <form class="command-search" @submit.prevent="run"><IconSearch :size="20"/><input v-model="query" placeholder="例如：SuperMap iServer 2026 如何部署云原生 GIS？"/><button :disabled="!canRun || loading">{{ loading ? '检索中' : '检索' }}</button></form>
        <div v-if="recent.length" class="recent"><span>最近检索</span><button v-for="item in recent" :key="item" @click="useRecent(item)">{{ item }}</button></div>
      </section>

      <p v-if="error" class="page-error">{{ error }}</p>
      <div class="search-grid">
        <aside class="scope-panel tech-panel">
          <div class="panel-title"><span>知识范围</span><IconAdjustments :size="17"/></div>
          <button class="select-all" @click="selected = selected.length === bases.length ? [] : bases.map(item => item.id)">{{ selected.length === bases.length ? '取消全选' : '选择全部' }}</button>
          <label v-for="base in bases" :key="base.id" :class="{ active: selected.includes(base.id) }"><input v-model="selected" type="checkbox" :value="base.id"/><span><b>{{ base.name }}</b><small>{{ base.documentCount }} 份文档 · {{ base.status }}</small></span></label>
          <div class="parameters"><label>返回数量<input v-model.number="topK" class="tech-input" type="number" min="1" max="20"/></label><label>相似度阈值<input v-model.number="threshold" class="tech-input" type="number" min="0" max="1" step=".05"/></label></div>
        </aside>

        <section class="result-panel tech-panel">
          <div class="result-head"><div><span class="eyebrow">RETRIEVAL STREAM</span><h3>{{ result ? `命中 ${result.hits.length} 条知识证据` : '等待输入问题' }}</h3></div><span v-if="result" class="latency"><i/>{{ result.durationMs }} ms</span></div>
          <div v-if="!result && !loading" class="empty-state"><span><IconDatabaseSearch :size="36"/></span><b>连接到 {{ bases.length }} 个 RAGFlow 知识库</b><p>输入自然语言问题，系统会返回文档来源、页码、相似度和原始片段。</p></div>
          <div v-if="loading" class="reasoning"><span/><span/><span/><p>正在向量化问题并搜索知识空间...</p></div>
          <article v-for="(hit, index) in result?.hits" :key="hit.id" class="hit-card">
            <div class="hit-rank">{{ String(index + 1).padStart(2, '0') }}</div><div class="file-icon"><IconFileText :size="19"/></div>
            <div class="hit-body"><header><b>{{ hit.documentName }}</b><span>{{ (hit.score * 100).toFixed(1) }}%</span></header><p>{{ hit.content }}</p>
              <div v-if="hit.assetContext" class="asset-evidence">
                <RouterLink :to="{ path: '/assets', query: { asset: hit.assetContext.assetId } }"><strong>GIS 知识资产</strong><b>{{ hit.assetContext.assetTitle }}</b><span v-if="hit.assetContext.pptPage">P{{ hit.assetContext.pptPage }} · {{ hit.assetContext.pageTitle || 'PPT 页面来源' }}</span></RouterLink>
                <div v-if="hit.assetContext.relatedImages.length || hit.assetContext.relatedVideos.length" class="asset-media">
                  <img v-for="media in hit.assetContext.relatedImages.slice(0, 3)" :key="media.id" :src="media.contentUrl ? mediaUrls[media.contentUrl] : undefined" :alt="media.filename" />
                  <video v-for="media in hit.assetContext.relatedVideos.slice(0, 2)" :key="media.id" :src="media.contentUrl ? mediaUrls[media.contentUrl] : undefined" muted controls preload="metadata" />
                </div>
              </div>
              <footer><span><IconBook2 :size="13"/>{{ hit.knowledgeBaseName }}</span><span v-if="hit.pageNumber">第 {{ hit.pageNumber }} 页</span><code>{{ hit.chunkId.slice(0, 12) }}</code></footer></div>
          </article>
        </section>
      </div>
    </div>
  </AppShell>
</template>

<style scoped>
.search-workspace{display:grid;gap:12px}.search-hero{position:relative;min-height:222px;padding:28px 32px 22px 108px;border:1px solid rgba(71,180,238,.23);border-radius:14px;background:radial-gradient(circle at 8% 26%,rgba(26,211,255,.14),transparent 20%),linear-gradient(120deg,rgba(7,30,47,.96),rgba(3,13,23,.92));overflow:hidden}.search-hero:after{content:'';position:absolute;inset:0;background:linear-gradient(90deg,transparent 49.8%,rgba(71,180,238,.035) 50%,transparent 50.2%),linear-gradient(0deg,transparent 49.8%,rgba(71,180,238,.025) 50%,transparent 50.2%);background-size:38px 38px;pointer-events:none}.hero-orb{position:absolute;left:30px;top:31px;width:58px;height:58px;display:grid;place-items:center;border-radius:18px;color:var(--cyan);background:linear-gradient(145deg,rgba(34,210,255,.2),rgba(31,99,255,.13));border:1px solid rgba(72,209,255,.32);box-shadow:0 0 35px rgba(20,180,255,.16);z-index:1}.search-hero>div,.command-search,.recent{position:relative;z-index:1}.eyebrow{color:var(--cyan);font-size:10px;letter-spacing:2px}.search-hero h2{margin:7px 0 5px;font-size:24px}.search-hero p{margin:0;color:var(--text-3)}.command-search{height:54px;margin-top:24px;display:flex;align-items:center;gap:12px;padding:0 7px 0 16px;border:1px solid rgba(61,170,234,.34);border-radius:10px;background:rgba(1,9,16,.72);box-shadow:0 14px 36px rgba(0,0,0,.23)}.command-search svg{color:var(--cyan)}.command-search input{flex:1;border:0;outline:0;background:transparent;color:var(--text-1);font-size:14px}.command-search button{height:40px;min-width:82px;border:0;border-radius:7px;color:white;background:linear-gradient(90deg,#1679ef,#19bcea)}.command-search button:disabled{opacity:.45}.recent{display:flex;align-items:center;gap:7px;margin-top:12px;overflow:hidden}.recent>span{color:var(--text-3);font-size:10px;flex:0 0 auto}.recent button{max-width:190px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;border:1px solid var(--line);border-radius:999px;padding:4px 9px;background:rgba(15,52,73,.25);color:var(--text-2);font-size:10px}.search-grid{display:grid;grid-template-columns:290px minmax(0,1fr);gap:12px;min-height:540px}.scope-panel{padding:16px;display:flex;flex-direction:column;gap:8px}.select-all{align-self:flex-end;border:0;background:transparent;color:var(--primary-2);font-size:10px}.scope-panel>label{display:grid;grid-template-columns:18px 1fr;gap:8px;align-items:center;padding:10px;border:1px solid var(--line);border-radius:7px;color:var(--text-2);background:rgba(6,24,37,.4)}.scope-panel>label.active{border-color:rgba(37,166,238,.42);background:rgba(22,126,211,.1)}.scope-panel>label span{display:grid;gap:4px}.scope-panel>label b{font-size:11px}.scope-panel>label small{color:var(--text-3);font-size:9px}.parameters{margin-top:auto;display:grid;grid-template-columns:1fr 1fr;gap:8px;padding-top:12px;border-top:1px solid var(--line)}.parameters label{display:grid;gap:6px;color:var(--text-3);font-size:10px}.result-panel{padding:18px;min-height:0}.result-head{display:flex;justify-content:space-between;align-items:center;padding-bottom:14px;border-bottom:1px solid var(--line)}.result-head h3{margin:5px 0 0}.latency{display:flex;align-items:center;gap:7px;color:var(--success);font-size:11px}.latency i{width:7px;height:7px;border-radius:50%;background:var(--success);box-shadow:0 0 10px var(--success)}.empty-state{min-height:390px;display:grid;place-items:center;align-content:center;gap:10px;color:var(--text-2);text-align:center}.empty-state>span{width:70px;height:70px;display:grid;place-items:center;border:1px solid var(--line-strong);border-radius:50%;color:var(--cyan);background:rgba(20,133,221,.08)}.empty-state p{max-width:430px;color:var(--text-3)}.reasoning{min-height:300px;display:flex;align-items:center;justify-content:center;gap:7px;color:var(--text-3)}.reasoning span{width:7px;height:7px;border-radius:50%;background:var(--cyan);animation:pulse 1s infinite alternate}.reasoning span:nth-child(2){animation-delay:.2s}.reasoning span:nth-child(3){animation-delay:.4s}.reasoning p{margin-left:8px}.hit-card{display:grid;grid-template-columns:28px 38px 1fr;gap:10px;padding:16px 2px;border-bottom:1px solid var(--line);will-change:transform,opacity}.hit-rank{font:600 11px ui-monospace;color:var(--text-3)}.file-icon{width:34px;height:34px;display:grid;place-items:center;border:1px solid var(--line);border-radius:8px;color:var(--primary-2);background:rgba(24,135,227,.09)}.hit-body header{display:flex;justify-content:space-between;gap:10px}.hit-body header span{color:var(--success);font-size:11px}.hit-body p{color:var(--text-2);font-size:12px;line-height:1.72}.hit-body footer{display:flex;gap:13px;color:var(--text-3);font-size:10px}.hit-body footer span{display:flex;align-items:center;gap:4px}.hit-body code{color:#637f93}.page-error{padding:10px;color:var(--danger);border:1px solid rgba(255,90,110,.25)}@keyframes pulse{to{opacity:.25;transform:scale(.7)}}@media(max-width:900px){.search-grid{grid-template-columns:1fr}.search-hero{padding-left:26px}.hero-orb{display:none}}
.asset-evidence{display:grid;grid-template-columns:minmax(210px,1fr) auto;gap:10px;margin:9px 0;padding:8px;border:1px solid rgba(44,181,225,.2);border-radius:7px;background:rgba(13,73,102,.1)}.asset-evidence>a{display:grid;gap:3px;color:var(--text-2)}.asset-evidence strong{color:var(--cyan);font-size:8px;letter-spacing:.8px}.asset-evidence b{font-size:10px}.asset-evidence span{font-size:8px;color:var(--text-3)}.asset-media{display:flex;gap:5px}.asset-media img,.asset-media video{width:84px;height:48px;object-fit:cover;border-radius:4px;background:#06131d}@media(max-width:700px){.asset-evidence{grid-template-columns:1fr}.asset-media{overflow:auto}}
</style>
