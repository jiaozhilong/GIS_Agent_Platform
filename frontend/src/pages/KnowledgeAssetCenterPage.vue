<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import {
  IconArchive, IconBook2, IconChevronRight, IconCloudUpload, IconDatabase, IconDownload,
  IconFileSpreadsheet, IconFileText, IconFileTypePdf, IconPhoto, IconPresentation,
  IconRefresh, IconSearch, IconVideo, IconX
} from '@tabler/icons-vue'
import AppShell from '@/components/layout/AppShell.vue'
import { api } from '@/api/services'
import type { AssetDocumentType, AssetKnowledgeType, KnowledgeAsset, KnowledgeAssetDetail, KnowledgeAssetPage, KnowledgeAssetSummary, KnowledgeAssetType, KnowledgeBase, ParserStrategy } from '@/api/contracts'
import { useRoute } from 'vue-router'

const route = useRoute()
const summary = ref<KnowledgeAssetSummary>({ total: 0, documents: 0, images: 0, videos: 0, pptPages: 0, ready: 0 })
const assets = ref<KnowledgeAsset[]>([])
const bases = ref<KnowledgeBase[]>([])
const detail = ref<KnowledgeAssetDetail | null>(null)
const selectedPage = ref<KnowledgeAssetPage | null>(null)
const type = ref<'ALL' | KnowledgeAssetType>('ALL')
const keyword = ref('')
const loading = ref(false)
const uploading = ref(false)
const uploadProgress = ref(0)
const syncing = ref(false)
const uploadOpen = ref(false)
const notice = ref('')
const error = ref('')
const file = ref<File | null>(null)
const fileInput = ref<HTMLInputElement | null>(null)
const objectUrls = reactive<Record<string, string>>({})
const advanced = ref(false)
const parserOverride = ref<ParserStrategy | ''>('')
const form = reactive({ datasetId: '', title: '', description: '', knowledgeType: 'SOLUTION' as AssetKnowledgeType, documentType: 'INDUSTRY_SOLUTION' as AssetDocumentType, industry: '自然资源', gisDomain: '', product: '', productVersion: '', projectType: '', region: '', year: new Date().getFullYear(), tags: '' })

const typeOptions = computed(() => [
  { value: 'ALL' as const, label: '全部资产', count: summary.value.total, icon: IconArchive },
  { value: 'DOCUMENT' as const, label: '文档知识', count: summary.value.documents, icon: IconFileText },
  { value: 'IMAGE' as const, label: '视觉素材', count: summary.value.images, icon: IconPhoto },
  { value: 'VIDEO' as const, label: '视频案例', count: summary.value.videos, icon: IconVideo }
])
const filtered = computed(() => assets.value.filter(item => (type.value === 'ALL' || item.assetType === type.value)
  && `${item.title}${item.originalFilename}${item.description}${item.industry}${item.tags.join('')}`.toLowerCase().includes(keyword.value.trim().toLowerCase())))
const pageMedia = computed(() => detail.value?.media.filter(item => item.pageNumber == null || item.pageNumber === selectedPage.value?.pageNumber) || [])
const formatSize = (size: number) => size >= 1024 * 1024 ? `${(size / 1024 / 1024).toFixed(1)} MB` : `${Math.max(1, Math.round(size / 1024))} KB`
const typeLabel = (value: KnowledgeAssetType) => ({ DOCUMENT: '文档知识', IMAGE: '视觉素材', VIDEO: '视频案例' }[value])
const assetIcon = (item: KnowledgeAsset) => item.assetType === 'IMAGE' ? IconPhoto : item.assetType === 'VIDEO' ? IconVideo : item.documentFormat?.startsWith('PPT') ? IconPresentation : item.documentFormat === 'EXCEL' ? IconFileSpreadsheet : item.documentFormat === 'PDF' ? IconFileTypePdf : IconFileText

const releaseUrls = () => { Object.values(objectUrls).forEach(URL.revokeObjectURL); Object.keys(objectUrls).forEach(key => delete objectUrls[key]) }
const ensureUrl = async (path?: string) => {
  if (!path || objectUrls[path]) return
  try { objectUrls[path] = URL.createObjectURL(await api.knowledgeAssetBlob(path)) } catch { /* text metadata remains usable */ }
}
const loadPageMedia = async () => {
  if (selectedPage.value?.previewUrl) await ensureUrl(selectedPage.value.previewUrl)
  await Promise.all(pageMedia.value.filter(item => item.contentUrl).slice(0, 12).map(item => ensureUrl(item.contentUrl)))
}
const selectPage = async (page: KnowledgeAssetPage) => { selectedPage.value = page; await loadPageMedia() }
const selectAsset = async (asset: KnowledgeAsset) => {
  loading.value = true; error.value = ''; releaseUrls()
  try {
    detail.value = await api.knowledgeAsset(asset.id)
    selectedPage.value = detail.value.pages[0] || null
    await loadPageMedia()
  } catch (e) { error.value = e instanceof Error ? e.message : '资产详情加载失败' }
  finally { loading.value = false }
}
let pollTimer: number | undefined
const pollAsset = (id: string) => {
  window.clearInterval(pollTimer)
  pollTimer = window.setInterval(async () => {
    try {
      const next = await api.knowledgeAsset(id); detail.value = next
      const index = assets.value.findIndex(item => item.id === id); if (index >= 0) assets.value[index] = next.asset
      if (next.asset.status !== 'PROCESSING') { window.clearInterval(pollTimer); await refresh() }
    } catch { window.clearInterval(pollTimer) }
  }, 2000)
}
const refresh = async (keepSelection = true) => {
  const current = keepSelection ? detail.value?.asset.id : undefined
  const [nextSummary, nextAssets, nextBases] = await Promise.all([api.knowledgeAssetSummary(), api.knowledgeAssets(), api.knowledgeBases()])
  summary.value = nextSummary; assets.value = nextAssets; bases.value = nextBases
  if (!form.datasetId) form.datasetId = bases.value.find(item => item.documentCount > 0)?.id || bases.value[0]?.id || ''
  const requested = String(route.query.asset || '')
  const target = assets.value.find(item => item.id === current) || assets.value.find(item => item.id === requested) || assets.value[0]
  if (target) await selectAsset(target); else detail.value = null
}
const chooseFile = () => fileInput.value?.click()
const pickFile = (event: Event) => {
  file.value = (event.target as HTMLInputElement).files?.[0] || null
  if (file.value && file.value.size > 5 * 1024 * 1024 * 1024) {
    error.value = '文件超过 5GB，请压缩媒体资源后再上传'
    file.value = null
    ;(event.target as HTMLInputElement).value = ''
    return
  }
  if (file.value && !form.title) form.title = file.value.name.replace(/\.[^.]+$/, '')
}
const upload = async () => {
  if (!file.value || !form.datasetId) { error.value = '请选择资产文件和目标 RAGFlow 知识库'; return }
  uploading.value = true; uploadProgress.value = 0; error.value = ''; notice.value = ''
  try {
    const created = await api.uploadKnowledgeAsset(file.value, form, percent => { uploadProgress.value = percent })
    uploadOpen.value = false
    file.value = null
    await refresh(false)
    const asset = assets.value.find(item => item.id === created.asset.id)
    if (asset) await selectAsset(asset)
    notice.value = created.asset.status === 'READY'
      ? `“${created.asset.title}”已完成业务解析并同步 RAGFlow`
      : `“${created.asset.title}”已保存，后台正在执行校验、Parser 路由和 RAGFlow 索引`
    if (created.asset.status === 'PROCESSING') pollAsset(created.asset.id)
  } catch (e) { error.value = e instanceof Error ? e.message : '知识资产上传失败' }
  finally { uploading.value = false; uploadProgress.value = 0 }
}
const retry = async () => {
  if (!detail.value) return
  syncing.value = true; error.value = ''; notice.value = ''
  try { detail.value = await api.reparseKnowledgeAsset(detail.value.asset.id, parserOverride.value || undefined); notice.value = '重新解析任务已提交'; pollAsset(detail.value.asset.id); await refresh() }
  catch (e) { error.value = e instanceof Error ? e.message : 'RAGFlow 重试失败' }
  finally { syncing.value = false }
}
const download = async () => {
  if (!detail.value) return
  const blob = await api.knowledgeAssetBlob(`/knowledge-assets/${detail.value.asset.id}/download`)
  const url = URL.createObjectURL(blob); const link = document.createElement('a'); link.href = url; link.download = detail.value.asset.originalFilename; link.click(); URL.revokeObjectURL(url)
}

onMounted(async () => { try { await refresh(false) } catch (e) { error.value = e instanceof Error ? e.message : '资产中心加载失败' } })
onBeforeUnmount(() => { releaseUrls(); window.clearInterval(pollTimer) })
</script>

<template>
  <AppShell title="GIS 知识资产中心" subtitle="把行业文档、方案视觉和演示视频转化为 Agent 可检索、可追溯的业务知识资产">
    <section class="asset-hero">
      <div class="hero-copy"><span>GIS KNOWLEDGE ASSET FABRIC</span><h2>不是文件仓库，而是 Agent 的行业记忆层</h2><p>平台管理业务语义、PPT 页面与关联媒体；RAGFlow 负责文本切片、向量化和召回。</p></div>
      <div class="ppt-flow"><div><IconPresentation/><b>PPTX</b><small>业务方案</small></div><IconChevronRight/><div><IconFileText/><b>页面文本</b><small>逐页索引</small></div><IconChevronRight/><div><IconPhoto/><b>关联媒体</b><small>图片 · 视频</small></div><IconChevronRight/><div><IconDatabase/><b>RAGFlow</b><small>语义知识库</small></div></div>
      <div class="boundary"><b>资产边界</b><span>支持 PDF / Word / PPT / Excel / 图片 / 视频</span><em>不接收三维模型、GIS 数据、CAD、遥感影像</em></div>
      <button class="primary-button" @click="uploadOpen=true"><IconCloudUpload :size="17"/>注入知识资产</button>
    </section>

    <div class="metric-grid"><article><span>知识资产</span><b>{{summary.total}}</b><small>{{summary.ready}} 项可供 Agent 使用</small></article><article><span>行业文档</span><b>{{summary.documents}}</b><small>PDF · Word · PPT · Excel</small></article><article><span>PPT 页面记忆</span><b>{{summary.pptPages}}</b><small>页面级来源追溯</small></article><article><span>视觉 / 视频</span><b>{{summary.images + summary.videos}}</b><small>{{summary.images}} 图片 · {{summary.videos}} 视频</small></article></div>
    <div v-if="notice" class="notice">{{notice}}</div><div v-if="error" class="error">{{error}}</div>

    <div class="asset-workspace">
      <aside class="asset-scope tech-panel">
        <div class="panel-title"><span>资产知识域</span><button title="刷新" @click="refresh()"><IconRefresh :size="15"/></button></div>
        <button v-for="item in typeOptions" :key="item.value" :class="{active:type===item.value}" @click="type=item.value"><component :is="item.icon" :size="16"/><span>{{item.label}}</span><em>{{item.count}}</em></button>
        <div class="scope-rule"><b>业务管理层</b><p>只管理可被方案 Agent 理解和引用的知识资产，不承担空间数据治理。</p></div>
        <RouterLink to="/knowledge" class="ragflow-link"><IconDatabase :size="15"/>进入 RAGFlow 底层配置</RouterLink>
      </aside>

      <section class="asset-stream tech-panel">
        <header><label><IconSearch :size="16"/><input v-model="keyword" placeholder="搜索行业、方案、产品或标签"/></label><span>{{filtered.length}} 个知识节点</span></header>
        <div v-if="!filtered.length" class="empty"><IconArchive :size="38"/><b>还没有匹配的知识资产</b><p>上传方案文档、架构图或演示视频，让 Agent 建立可检索的行业记忆。</p></div>
        <button v-for="asset in filtered" :key="asset.id" :class="['asset-card',{active:detail?.asset.id===asset.id}]" @click="selectAsset(asset)">
          <span class="asset-icon"><component :is="assetIcon(asset)" :size="21"/></span>
          <span class="asset-main"><small>{{typeLabel(asset.assetType)}} · {{asset.industry || '通用 GIS'}}</small><b>{{asset.title}}</b><p>{{asset.description || asset.originalFilename}}</p><i><em v-for="tag in asset.tags.slice(0,4)" :key="tag">{{tag}}</em></i></span>
          <span class="asset-signal"><i :class="asset.status.toLowerCase()"/><b>{{asset.status}}</b><small v-if="asset.pageCount">{{asset.pageCount}} 页</small><small v-else>{{formatSize(asset.fileSize)}}</small></span>
        </button>
      </section>

      <aside v-if="detail" class="asset-inspector tech-panel">
        <header><div><span>{{typeLabel(detail.asset.assetType)}} / {{detail.asset.documentFormat || detail.asset.mediaType}}</span><h3>{{detail.asset.title}}</h3></div><button title="下载原件" @click="download"><IconDownload :size="16"/></button></header>
        <div class="asset-status"><span :class="detail.asset.status.toLowerCase()">{{detail.asset.status}}</span><p>{{detail.asset.statusMessage}}</p><button v-if="detail.asset.status!=='PROCESSING'" :disabled="syncing" @click="retry"><IconRefresh :size="13"/>{{syncing?'提交中':'重新解析'}}</button></div>
        <div v-if="detail.parseTask" class="parse-progress"><div><span>{{detail.parseTask.stage}}</span><b>{{detail.parseTask.progress}}%</b></div><i><em :style="{width:`${detail.parseTask.progress}%`}"/></i><small>Parser：{{detail.asset.parserStrategy || '待路由'}} · {{detail.asset.parserReason || '正在判断文件类型与资料类型'}}</small></div>
        <div class="asset-meta"><span><b>{{detail.asset.pageCount}}</b>PPT 页面</span><span><b>{{detail.asset.extractedImageCount}}</b>关联图片</span><span><b>{{detail.asset.extractedVideoCount}}</b>关联视频</span></div>
        <div v-if="detail.pages.length" class="page-index"><div class="panel-title"><span>PPT 页面知识索引</span><em>{{selectedPage?.pageNumber || 1}} / {{detail.pages.length}}</em></div><div><button v-for="page in detail.pages" :key="page.id" :class="{active:selectedPage?.id===page.id}" @click="selectPage(page)"><b>{{String(page.pageNumber).padStart(2,'0')}}</b><span>{{page.title}}</span><small>{{page.imageCount}} 图 · {{page.videoCount}} 视频</small></button></div></div>
        <div v-if="selectedPage" class="page-intelligence">
          <img v-if="selectedPage.previewUrl && objectUrls[selectedPage.previewUrl]" :src="objectUrls[selectedPage.previewUrl]" :alt="selectedPage.title"/>
          <div><span>PAGE {{selectedPage.pageNumber}} KNOWLEDGE</span><b>{{selectedPage.title}}</b><p>{{selectedPage.textContent || '该页面没有可提取文本，仍保留页面与媒体关联。'}}</p></div>
        </div>
        <div v-if="pageMedia.length" class="media-grid"><div class="panel-title"><span>关联视觉与视频</span><em>{{pageMedia.length}}</em></div><article v-for="media in pageMedia" :key="media.id">
          <img v-if="media.mediaKind==='IMAGE' && media.contentUrl && objectUrls[media.contentUrl]" :src="objectUrls[media.contentUrl]" :alt="media.filename"/>
          <video v-else-if="media.mediaKind==='VIDEO' && media.contentUrl && objectUrls[media.contentUrl]" :src="objectUrls[media.contentUrl]" controls/>
          <a v-else-if="media.externalUrl" :href="media.externalUrl" target="_blank"><IconVideo :size="24"/>打开外链视频</a>
          <span><b>{{media.filename}}</b><small>{{media.pageNumber ? `P${media.pageNumber}` : '独立资产'}} · {{media.sourceKind}}</small></span>
        </article></div>
        <div class="semantic"><b>Agent 可用语义</b><p>{{detail.asset.description || '暂无业务说明'}}</p><div><span v-for="tag in detail.asset.tags" :key="tag"># {{tag}}</span></div></div>
      </aside>
      <aside v-else class="asset-inspector empty-inspector tech-panel"><IconBook2 :size="40"/><b>等待知识资产</b><p>选择资产后查看页面索引、文本知识和关联媒体。</p></aside>
    </div>

    <div v-if="uploadOpen" class="dialog-mask"><form class="upload-dialog tech-panel" @submit.stop.prevent="upload"><header><div><span>ASSET INGESTION</span><h2>注入 GIS 行业知识资产</h2></div><button type="button" @click="uploadOpen=false"><IconX/></button></header>
      <div class="upload-form"><button type="button" class="drop-zone" @click="chooseFile"><IconCloudUpload :size="32"/><b>{{file?.name || '选择业务知识文件'}}</b><span>PDF / DOCX / PPTX / XLS / XLSX / PNG / JPG / MP4 / MOV</span><small>大型 PPTX 将使用流式识别并提取页面文本、图片、视频信息和页面预览 · 单文件最大 5GB</small></button><input ref="fileInput" hidden type="file" accept=".pdf,.docx,.pptx,.xls,.xlsx,.txt,.md,.png,.jpg,.jpeg,.webp,.mp4,.mov,.avi,.wmv,.webm,.m4v" @change="pickFile"/>
        <div class="fields"><label>同步到 RAGFlow 知识库 *<select v-model="form.datasetId" class="tech-select"><option value="" disabled>请选择知识库</option><option v-for="base in bases" :key="base.id" :value="base.id">{{base.name}}</option></select></label><label>资料类型 *<select v-model="form.documentType" class="tech-select"><option value="PRODUCT_MANUAL">产品手册</option><option value="TECHNICAL_MANUAL">技术手册</option><option value="PRODUCT_PRESENTATION">产品介绍PPT</option><option value="INDUSTRY_SOLUTION">行业解决方案</option><option value="PROJECT_SOLUTION">项目技术方案</option><option value="PROJECT_CASE">历史项目案例</option><option value="FAQ">FAQ</option><option value="TEMPLATE">模板</option><option value="OTHER">其他</option></select></label><label>知识类型<select v-model="form.knowledgeType" class="tech-select"><option value="PRODUCT">产品</option><option value="SOLUTION">解决方案</option><option value="CASE">案例</option><option value="TROUBLESHOOTING">故障排查</option><option value="TEMPLATE">模板</option></select></label><label>资产标题<input v-model="form.title" class="tech-input" placeholder="例如：自然资源一张图建设方案"/></label><label>行业领域<input v-model="form.industry" class="tech-input" placeholder="自然资源 / 住建 / 水利"/></label><label>GIS领域<input v-model="form.gisDomain" class="tech-input" placeholder="一张图 / 数字孪生 / 时空底座"/></label><label>产品<input v-model="form.product" class="tech-input" placeholder="SuperMap iServer"/></label><label>产品版本<input v-model="form.productVersion" class="tech-input" placeholder="12i"/></label><label>区域<input v-model="form.region" class="tech-input" placeholder="江苏 / 全国"/></label><label>业务标签<input v-model="form.tags" class="tech-input" placeholder="一张图，三维GIS，数据治理"/></label><label class="full">业务说明<textarea v-model="form.description" class="tech-textarea" placeholder="说明该资产适用的业务场景、方案阶段和可复用内容"/></label><button type="button" class="advanced-toggle" @click="advanced=!advanced">{{advanced?'收起':'展开'}}管理员高级设置</button><label v-if="advanced" class="full">重新解析时人工覆盖 Parser<select v-model="parserOverride" class="tech-select"><option value="">系统自动推荐</option><option value="GENERAL">General</option><option value="MANUAL">Manual</option><option value="PRESENTATION">Presentation</option><option value="TABLE">Table</option><option value="QA">Q&A</option></select></label></div>
      </div><div class="reject-note"><b>明确不接收</b><span>三维模型 · GIS 数据 · CAD · 遥感影像</span><small>这些内容应进入专业 GIS 数据管理或空间数据基础设施，不进入 RAGFlow 知识资产链路。</small></div>
      <footer><button type="button" class="ghost-button" @click="uploadOpen=false">取消</button><button type="submit" class="primary-button" :disabled="uploading || !file || !form.datasetId">{{uploading ? (uploadProgress < 100 ? `上传中 ${uploadProgress}%` : '正在解析并同步...') : '开始资产化'}}</button></footer></form></div>
  </AppShell>
</template>

<style scoped>
.asset-hero{min-height:150px;padding:22px;display:grid;grid-template-columns:minmax(260px,.8fr) minmax(410px,1.2fr) minmax(180px,.6fr) 132px;align-items:center;gap:18px;border:1px solid rgba(46,183,235,.22);border-radius:12px;background:radial-gradient(circle at 44% 20%,rgba(22,172,230,.13),transparent 28%),linear-gradient(120deg,rgba(6,27,42,.97),rgba(2,12,20,.95))}.asset-hero>.primary-button{width:132px;min-height:42px;white-space:nowrap}.hero-copy>span{color:var(--cyan);font-size:8px;letter-spacing:1.8px}.hero-copy h2{margin:7px 0;font-size:19px}.hero-copy p{margin:0;color:var(--text-3);font-size:10px;line-height:1.6}.ppt-flow{display:flex;align-items:center;justify-content:center;gap:7px}.ppt-flow>div{min-width:82px;padding:10px 8px;display:grid;place-items:center;gap:4px;border:1px solid var(--line);border-radius:8px;color:var(--cyan);background:rgba(9,43,63,.45)}.ppt-flow b{color:var(--text-1);font-size:9px}.ppt-flow small{color:var(--text-3);font-size:7px}.ppt-flow>svg{color:#3b6680}.boundary{display:grid;gap:5px;padding:11px;border-left:1px solid var(--line)}.boundary b{font-size:10px}.boundary span{color:var(--text-2);font-size:8px}.boundary em{font-style:normal;color:var(--warning);font-size:8px}.metric-grid{display:grid;grid-template-columns:repeat(4,1fr);gap:9px;margin:10px 0}.metric-grid article{padding:12px 15px;border:1px solid var(--line);border-radius:8px;background:linear-gradient(135deg,rgba(7,28,43,.9),rgba(3,14,23,.8));display:grid;grid-template-columns:1fr auto;align-items:center}.metric-grid span{color:var(--text-3);font-size:9px}.metric-grid b{grid-row:1/3;grid-column:2;font-size:24px;color:var(--primary-2)}.metric-grid small{font-size:8px;color:#49697d}.notice,.error{padding:9px 12px;margin-bottom:9px;border-radius:5px;font-size:10px}.notice{color:var(--success);border:1px solid rgba(44,230,160,.2)}.error{color:var(--danger);border:1px solid rgba(255,90,110,.25)}.asset-workspace{display:grid;grid-template-columns:210px minmax(410px,1fr) 420px;gap:9px;min-height:calc(100vh - 348px)}.asset-scope,.asset-stream,.asset-inspector{padding:13px;min-height:0}.asset-scope{display:flex;flex-direction:column;gap:5px}.asset-scope>.panel-title button{border:0;background:none;color:var(--text-3)}.asset-scope>button{height:36px;padding:0 9px;display:grid;grid-template-columns:20px 1fr auto;align-items:center;border:1px solid transparent;border-radius:6px;background:transparent;color:var(--text-2);text-align:left;font-size:10px}.asset-scope>button em{font-style:normal;color:var(--text-3)}.asset-scope>button.active{color:var(--primary-2);border-color:var(--line);background:rgba(18,109,169,.13)}.scope-rule{margin-top:10px;padding:11px;border:1px solid rgba(41,182,222,.16);border-radius:7px;background:rgba(12,69,100,.08)}.scope-rule b{font-size:9px;color:var(--cyan)}.scope-rule p{color:var(--text-3);font-size:8px;line-height:1.6}.ragflow-link{margin-top:auto;display:flex;align-items:center;gap:7px;padding:10px;border:1px solid var(--line);border-radius:6px;color:var(--text-2);font-size:9px}.asset-stream{display:flex;flex-direction:column;gap:6px;overflow:hidden}.asset-stream>header{height:42px;display:flex;align-items:center;gap:10px;border-bottom:1px solid var(--line)}.asset-stream>header label{flex:1;display:flex;align-items:center;gap:8px;color:var(--text-3)}.asset-stream>header input{width:100%;border:0;outline:0;background:none;color:var(--text-1)}.asset-stream>header>span{color:var(--text-3);font-size:8px}.asset-card{display:grid;grid-template-columns:40px 1fr 65px;gap:10px;padding:10px;border:1px solid transparent;border-radius:7px;background:rgba(5,23,35,.35);color:var(--text-1);text-align:left}.asset-card:hover,.asset-card.active{border-color:rgba(37,164,222,.31);background:rgba(13,67,96,.16)}.asset-icon{width:37px;height:37px;display:grid;place-items:center;border-radius:8px;color:var(--cyan);background:rgba(24,151,213,.1)}.asset-main{min-width:0;display:grid;gap:3px}.asset-main>small{color:var(--cyan);font-size:7px;letter-spacing:.6px}.asset-main>b{font-size:10px}.asset-main>p{margin:0;color:var(--text-3);font-size:8px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap}.asset-main>i{display:flex;gap:4px;font-style:normal}.asset-main>i em{padding:2px 4px;border:1px solid var(--line);border-radius:3px;color:#688699;font-size:7px;font-style:normal}.asset-signal{display:grid;justify-items:end;align-content:center;gap:4px}.asset-signal>i{width:6px;height:6px;border-radius:50%;background:var(--warning)}.asset-signal>i.ready{background:var(--success);box-shadow:0 0 7px var(--success)}.asset-signal>i.error{background:var(--danger)}.asset-signal b{font-size:7px;color:var(--text-3)}.asset-signal small{font-size:8px;color:var(--text-2)}.asset-inspector{overflow:auto}.asset-inspector>header{display:flex;justify-content:space-between;gap:10px;padding-bottom:10px;border-bottom:1px solid var(--line)}.asset-inspector>header span{color:var(--cyan);font-size:8px}.asset-inspector h3{margin:5px 0 0;font-size:14px}.asset-inspector>header button{border:1px solid var(--line);border-radius:6px;background:transparent;color:var(--text-2)}.asset-status{display:grid;grid-template-columns:auto 1fr auto;align-items:center;gap:8px;padding:9px 0}.asset-status>span{font-size:8px;color:var(--warning)}.asset-status>span.ready{color:var(--success)}.asset-status>span.error{color:var(--danger)}.asset-status p{margin:0;color:var(--text-3);font-size:8px}.asset-status button{display:flex;align-items:center;gap:4px;border:0;background:none;color:var(--primary-2);font-size:8px}.asset-meta{display:grid;grid-template-columns:repeat(3,1fr);gap:6px}.asset-meta span{padding:8px;display:grid;gap:3px;border:1px solid var(--line);border-radius:6px;color:var(--text-3);font-size:7px}.asset-meta b{color:var(--text-1);font-size:15px}.page-index{margin-top:11px}.page-index>.panel-title em,.media-grid>.panel-title em{font-style:normal;color:var(--cyan);font-size:8px}.page-index>div:last-child{display:flex;gap:5px;overflow:auto;padding-bottom:4px}.page-index button{flex:0 0 115px;padding:7px;display:grid;grid-template-columns:20px 1fr;gap:3px;border:1px solid var(--line);border-radius:5px;background:rgba(4,21,32,.5);color:var(--text-2);text-align:left}.page-index button.active{border-color:var(--cyan);background:rgba(20,127,170,.14)}.page-index button b{grid-row:1/3;color:var(--cyan);font:600 9px ui-monospace}.page-index button span{font-size:8px;white-space:nowrap;overflow:hidden;text-overflow:ellipsis}.page-index button small{font-size:7px;color:var(--text-3)}.page-intelligence{margin-top:8px;display:grid;grid-template-columns:150px 1fr;gap:10px;padding:8px;border:1px solid var(--line);border-radius:7px}.page-intelligence>img{width:150px;aspect-ratio:16/9;object-fit:cover;border-radius:4px;background:#07131c}.page-intelligence>div{display:grid;align-content:start;gap:5px}.page-intelligence span{color:var(--cyan);font-size:7px;letter-spacing:1px}.page-intelligence b{font-size:9px}.page-intelligence p{max-height:90px;overflow:auto;margin:0;color:var(--text-3);font-size:8px;line-height:1.55;white-space:pre-line}.media-grid{margin-top:10px}.media-grid article{display:grid;grid-template-columns:92px 1fr;gap:8px;margin-top:5px;padding:6px;border:1px solid var(--line);border-radius:6px}.media-grid img,.media-grid video,.media-grid article>a{width:92px;height:52px;object-fit:cover;border-radius:3px;background:#06121b}.media-grid article>a{display:grid;place-items:center;color:var(--primary-2);font-size:7px}.media-grid article>span{display:grid;align-content:center;gap:5px}.media-grid article b{font-size:8px;overflow-wrap:anywhere}.media-grid article small{color:var(--text-3);font-size:7px}.semantic{margin-top:10px;padding:10px;border:1px solid rgba(42,167,218,.18);border-radius:7px;background:rgba(11,72,105,.08)}.semantic b{font-size:9px;color:var(--cyan)}.semantic p{color:var(--text-3);font-size:8px;line-height:1.5}.semantic div{display:flex;gap:4px;flex-wrap:wrap}.semantic span{color:var(--primary-2);font-size:7px}.empty,.empty-inspector{display:grid;place-items:center;align-content:center;gap:8px;text-align:center;color:var(--text-3)}.empty p,.empty-inspector p{max-width:320px;font-size:9px}.dialog-mask{position:fixed;inset:0;z-index:100;display:grid;place-items:center;background:rgba(0,5,10,.82);backdrop-filter:blur(5px)}.upload-dialog{width:min(900px,92vw);padding:20px}.upload-dialog>header{display:flex;justify-content:space-between;border-bottom:1px solid var(--line);padding-bottom:13px}.upload-dialog>header span{color:var(--cyan);font-size:8px;letter-spacing:1px}.upload-dialog h2{margin:5px 0 0}.upload-dialog>header button{border:0;background:none;color:var(--text-2)}.upload-form{display:grid;grid-template-columns:280px 1fr;gap:16px;padding:16px 0}.drop-zone{min-height:220px;display:grid;place-items:center;align-content:center;gap:9px;border:1px dashed rgba(55,183,232,.35);border-radius:8px;background:rgba(13,72,104,.08);color:var(--cyan)}.drop-zone b{color:var(--text-1);font-size:11px;max-width:230px;overflow-wrap:anywhere}.drop-zone span,.drop-zone small{color:var(--text-3);font-size:8px;text-align:center}.fields{display:grid;grid-template-columns:1fr 1fr;gap:10px}.fields label{display:grid;gap:6px;color:var(--text-2);font-size:9px}.fields .full{grid-column:1/-1}.reject-note{padding:10px;display:grid;grid-template-columns:100px 1fr;gap:4px;border:1px solid rgba(255,180,65,.18);border-radius:6px}.reject-note b{grid-row:1/3;color:var(--warning);font-size:9px}.reject-note span{font-size:8px}.reject-note small{color:var(--text-3);font-size:8px}.upload-dialog>footer{display:flex;justify-content:flex-end;gap:8px;padding-top:12px}.page-error{color:var(--danger)}@media(max-width:1250px){.asset-hero{grid-template-columns:1fr 1fr}.asset-workspace{grid-template-columns:190px 1fr}.asset-inspector{grid-column:1/-1;max-height:500px}}@media(max-width:800px){.asset-hero,.asset-workspace,.upload-form{grid-template-columns:1fr}.ppt-flow{flex-wrap:wrap}.metric-grid{grid-template-columns:1fr 1fr}.asset-scope{min-height:260px}.fields{grid-template-columns:1fr}.fields .full{grid-column:auto}}
.parse-progress{padding:8px;margin-bottom:8px;border:1px solid var(--line);border-radius:6px}.parse-progress>div{display:flex;justify-content:space-between;color:var(--cyan);font-size:8px}.parse-progress>i{display:block;height:4px;margin:7px 0;background:#0b2535;border-radius:3px}.parse-progress em{display:block;height:100%;background:linear-gradient(90deg,var(--primary),var(--cyan));border-radius:3px}.parse-progress small{color:var(--text-3);font-size:7px}.advanced-toggle{grid-column:1/-1;border:0;background:transparent;color:var(--primary-2);text-align:left;font-size:9px}
</style>
