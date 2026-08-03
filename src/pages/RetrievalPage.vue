<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { IconArrowRight, IconBook2, IconDatabaseSearch, IconFileText, IconPlayerPlay } from '@tabler/icons-vue'
import AppShell from '@/components/layout/AppShell.vue'
import ProjectTabs from '@/components/layout/ProjectTabs.vue'
import { api } from '@/api/services'
import type { KnowledgeBase, RetrievalResult } from '@/api/contracts'

const route = useRoute(); const bases = ref<KnowledgeBase[]>([]); const result = ref<RetrievalResult | null>(null); const loading = ref(false)
const query = ref('自然资源一张图平台需要建设哪些核心能力？请给出总体架构、数据治理、GIS服务和应用建设要点。')
const selected = ref<string[]>(['kb-01','kb-02','kb-03'])
const topK = ref(5); const threshold = ref(.68)
const canRun = computed(() => query.value.trim().length > 8 && selected.value.length > 0)
const run = async () => { if(!canRun.value)return;loading.value=true;try{result.value=await api.retrieve(String(route.params.id),{query:query.value,knowledgeBaseIds:selected.value,topK:topK.value,similarityThreshold:threshold.value})}finally{loading.value=false} }
onMounted(async()=>{bases.value=await api.knowledgeBases();await run()})
</script>
<template>
  <AppShell title="知识检索" subtitle="检索 RAGFlow 知识片段并确认方案引用依据">
    <ProjectTabs />
    <div class="retrieval-layout">
      <aside class="query-panel tech-panel"><div class="panel-title"><span>检索配置</span><IconDatabaseSearch :size="17" /></div><label>检索问题<textarea v-model="query" class="tech-textarea" /></label><label>知识范围</label><div class="base-list"><label v-for="base in bases" :key="base.id" :class="{active:selected.includes(base.id)}"><input v-model="selected" type="checkbox" :value="base.id"/><span><b>{{ base.name }}</b><small>{{ base.documentCount }}份文档 · {{ base.chunkMethod }}</small></span></label></div><div class="params"><label>Top K<input v-model.number="topK" type="number" class="tech-input" min="1" max="20"/></label><label>相似度阈值<input v-model.number="threshold" type="number" class="tech-input" min="0" max="1" step="0.05"/></label></div><button class="primary-button" :disabled="!canRun||loading" @click="run"><IconPlayerPlay :size="16"/>{{loading?'正在检索...':'开始检索'}}</button></aside>
      <section class="evidence tech-panel"><div class="panel-title"><span>命中依据 {{ result?.hits.length ?? 0 }}</span><span class="secondary">{{ result ? `${result.durationMs}ms · 可追溯引用` : '等待检索' }}</span></div><div class="query-summary"><span class="tag">BGE-M3</span><span class="tag">RAGFlow</span><p>{{ result?.query }}</p></div><article v-for="(hit,i) in result?.hits" :key="hit.id"><span class="rank">{{i+1}}</span><div class="doc-icon"><IconFileText :size="20"/></div><div class="hit-content"><div><b>{{hit.documentName}}</b><span class="score">{{(hit.score*100).toFixed(0)}}%</span></div><p>{{hit.content}}</p><footer><span><IconBook2 :size="13"/>{{hit.knowledgeBaseName}}</span><span>第 {{hit.pageNumber}} 页</span><span v-for="(value,key) in hit.metadata" :key="key">{{key}}: {{value}}</span></footer></div></article><div class="evidence-footer"><span>{{ result ? '已选择全部命中依据用于方案生成' : '请先运行检索' }}</span><RouterLink :to="`/projects/${route.params.id}/proposal`" class="primary-button">生成方案 <IconArrowRight :size="16"/></RouterLink></div></section>
    </div>
  </AppShell>
</template>
<style scoped>
.retrieval-layout{display:grid;grid-template-columns:330px minmax(540px,1fr);gap:10px;min-height:calc(100vh - 152px)}.query-panel,.evidence{padding:14px}.query-panel{display:flex;flex-direction:column;gap:12px}.query-panel>label{color:var(--text-2);display:grid;gap:7px}.base-list{display:grid;gap:6px}.base-list label{min-height:48px;padding:8px 10px;border:1px solid var(--line);border-radius:6px;display:grid;grid-template-columns:18px 1fr;align-items:center;gap:8px;background:rgba(7,28,43,.28)}.base-list label.active{border-color:rgba(31,143,233,.42);background:rgba(25,134,255,.09)}.base-list span{display:grid;gap:4px}.base-list b{font-size:11px}.base-list small{color:var(--text-3);font-size:9px}.params{display:grid;grid-template-columns:1fr 1fr;gap:8px}.params label{display:grid;gap:6px;color:var(--text-3);font-size:10px}.query-panel>.primary-button{margin-top:auto}.evidence{display:flex;flex-direction:column;min-height:0}.query-summary{display:flex;align-items:center;gap:7px;border-bottom:1px solid var(--line);padding:12px 0}.query-summary p{margin:0 0 0 6px;color:var(--text-2);font-size:12px}.evidence article{display:grid;grid-template-columns:25px 38px 1fr;gap:9px;padding:15px 4px;border-bottom:1px solid var(--line)}.rank{color:var(--text-3);font-weight:700}.doc-icon{width:34px;height:34px;border-radius:5px;background:rgba(25,134,255,.13);color:var(--primary-2);display:grid;place-items:center}.hit-content>div{display:flex;justify-content:space-between}.hit-content p{color:var(--text-2);line-height:1.65;margin:8px 0;font-size:12px}.hit-content footer{display:flex;flex-wrap:wrap;gap:8px 13px;color:var(--text-3);font-size:10px}.hit-content footer span{display:flex;align-items:center;gap:4px}.evidence-footer{margin-top:auto;display:flex;align-items:center;justify-content:space-between;padding-top:14px;color:var(--text-3);font-size:11px}@media(max-width:1050px){.retrieval-layout{grid-template-columns:1fr}.query-panel>.primary-button{margin-top:0}}
</style>
