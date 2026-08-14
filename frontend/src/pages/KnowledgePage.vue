<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { IconCloudUpload, IconDatabase, IconFileSearch, IconRefresh, IconSearch, IconX } from '@tabler/icons-vue'
import AppShell from '@/components/layout/AppShell.vue'
import { api } from '@/api/services'
import type { CreateKnowledgeBaseRequest, KnowledgeBase, KnowledgeDocument } from '@/api/contracts'

const bases = ref<KnowledgeBase[]>([])
const documents = ref<KnowledgeDocument[]>([])
const keyword = ref('')
const selected = ref<KnowledgeBase | null>(null)
const syncing = ref(false)
const uploading = ref(false)
const loadingDocs = ref(false)
const createOpen = ref(false)
const saving = ref(false)
const notice = ref('')
const error = ref('')
const fileInput = ref<HTMLInputElement | null>(null)
const form = reactive<CreateKnowledgeBaseRequest>({ name: '', description: '', knowledgeType: 'INDUSTRY_SOLUTION', chunkMethod: 'manual' })

const filtered = computed(() => bases.value.filter(item => `${item.name}${item.description}`.includes(keyword.value)))

const selectBase = async (base: KnowledgeBase) => {
  selected.value = base
  loadingDocs.value = true
  error.value = ''
  try { documents.value = await api.knowledgeDocuments(base.id) }
  catch (e) { error.value = e instanceof Error ? e.message : '文档加载失败' }
  finally { loadingDocs.value = false }
}

const load = async () => {
  bases.value = await api.knowledgeBases()
  const current = bases.value.find(item => item.id === selected.value?.id) || bases.value[0] || null
  if (current) await selectBase(current)
}

const sync = async () => {
  syncing.value = true; error.value = ''; notice.value = ''
  try {
    const result = await api.syncKnowledgeBases()
    bases.value = result.knowledgeBases
    notice.value = `已同步 ${result.knowledgeBaseCount} 个知识库、${result.documentCount} 份文档、${result.chunkCount} 个切片`
    const current = bases.value.find(item => item.id === selected.value?.id) || bases.value[0]
    if (current) await selectBase(current)
  } catch (e) { error.value = e instanceof Error ? e.message : 'RAGFlow 同步失败' }
  finally { syncing.value = false }
}

const create = async () => {
  if (!form.name.trim()) { error.value = '请输入知识库名称'; return }
  saving.value = true; error.value = ''
  try {
    const created = await api.createKnowledgeBase(form)
    createOpen.value = false
    await sync()
    await selectBase(created)
    notice.value = `知识库“${created.name}”已在 RAGFlow 创建`
  } catch (e) { error.value = e instanceof Error ? e.message : '知识库创建失败' }
  finally { saving.value = false }
}

const chooseFile = () => fileInput.value?.click()
const upload = async (event: Event) => {
  const file = (event.target as HTMLInputElement).files?.[0]
  if (!file || !selected.value) return
  uploading.value = true; error.value = ''; notice.value = ''
  try {
    documents.value = await api.uploadKnowledgeDocument(selected.value.id, file)
    notice.value = `${file.name} 已上传，RAGFlow 解析任务已创建`
    await sync()
  } catch (e) { error.value = e instanceof Error ? e.message : '文档上传失败' }
  finally { uploading.value = false; (event.target as HTMLInputElement).value = '' }
}

onMounted(async () => { try { await load() } catch (e) { error.value = e instanceof Error ? e.message : '知识库加载失败' } })
</script>

<template>
  <AppShell title="知识库管理" subtitle="管理 RAGFlow Dataset、文档解析、切片策略与元数据">
    <div class="kb-toolbar"><label><IconSearch :size="16"/><input v-model="keyword" placeholder="搜索知识库"/></label><button class="secondary-button" :disabled="syncing" @click="sync"><IconRefresh :size="15"/>{{ syncing ? '同步中...' : '同步 RAGFlow' }}</button><button class="primary-button" @click="createOpen = true"><IconDatabase :size="16"/>新建知识库</button></div>
    <div v-if="notice" class="notice">{{ notice }}</div><div v-if="error" class="error">{{ error }}</div>
    <div class="kb-layout">
      <section class="kb-list"><button v-for="base in filtered" :key="base.id" :class="['kb-card tech-panel',{active:selected?.id===base.id}]" @click="selectBase(base)"><span class="db-icon"><IconDatabase :size="22"/></span><span><b>{{base.name}}</b><small>{{base.description}}</small><em>{{base.documentCount}} 份文档 · {{base.chunkCount}} 个切片</em></span><i :class="base.status.toLowerCase()">{{base.status}}</i></button></section>
      <section v-if="selected" class="kb-detail tech-panel"><div class="panel-title"><span>{{selected.name}}</span><span class="tag"><i class="status-dot"/>RAGFlow 实时数据</span></div><div class="summary"><div><span>文档数</span><b>{{selected.documentCount}}</b></div><div><span>切片数</span><b>{{selected.chunkCount}}</b></div><div><span>向量模型</span><b>{{selected.embeddingModel}}</b></div><div><span>切片策略</span><b>{{selected.chunkMethod}}</b></div></div><div class="meta"><h3>推荐元数据</h3><span v-for="item in ['knowledge_type','industry','product','product_version','gis_domain','region','status','confidentiality','effective_date']" :key="item" class="tag">{{item}}</span></div><div class="documents"><div class="panel-title"><span>RAGFlow 文档</span><button class="ghost-button" @click="selectBase(selected)"><IconFileSearch :size="15"/>刷新状态</button></div><div v-if="loadingDocs" class="doc-empty">正在读取解析状态...</div><div v-else-if="!documents.length" class="doc-empty">当前知识库还没有文档</div><div v-for="doc in documents" :key="doc.id" class="doc-row"><span><b>{{doc.name}}</b><small>{{doc.createdAt || '时间未知'}}</small></span><span class="score">{{doc.status}}</span><em>{{doc.chunkCount}} 个切片</em></div></div><input ref="fileInput" hidden type="file" accept=".pdf,.doc,.docx,.ppt,.pptx,.xls,.xlsx,.md,.txt" @change="upload"/><button class="upload-zone" :disabled="uploading" @click="chooseFile"><IconCloudUpload :size="30"/><b>{{uploading?'正在上传并创建解析任务...':'上传资料到当前知识库'}}</b><span>文件会直接发送到 RAGFlow，并自动启动解析</span></button></section>
    </div>

    <div v-if="createOpen" class="dialog-mask"><form class="dialog tech-panel" @submit.prevent="create"><header><h2>新建 RAGFlow 知识库</h2><button type="button" @click="createOpen=false"><IconX/></button></header><div class="form"><label>名称 *<input v-model="form.name" class="tech-input" placeholder="例如 06-政策标准知识库"/></label><label>类型<select v-model="form.knowledgeType" class="tech-select"><option value="PRODUCT_TECH">产品技术</option><option value="INDUSTRY_SOLUTION">行业方案</option><option value="PROJECT_CASE">项目案例</option><option value="TROUBLESHOOTING">故障排查</option><option value="TEMPLATE">模板资料</option></select></label><label>切片方式<select v-model="form.chunkMethod" class="tech-select"><option value="manual">Manual</option><option value="qa">Q&A</option><option value="presentation">Presentation</option></select></label><label>说明<textarea v-model="form.description" class="tech-textarea"/></label></div><p v-if="error" class="dialog-error">{{error}}</p><footer><button type="button" class="ghost-button" @click="createOpen=false">取消</button><button class="primary-button" :disabled="saving">{{saving?'正在创建...':'确认创建'}}</button></footer></form></div>
  </AppShell>
</template>

<style scoped>
.kb-toolbar{display:flex;gap:8px;justify-content:flex-end;margin-bottom:12px}.kb-toolbar>label{margin-right:auto;width:300px;height:39px;display:flex;align-items:center;gap:8px;padding:0 11px;border:1px solid var(--line);border-radius:6px;color:var(--text-3);background:rgba(3,13,22,.7)}.kb-toolbar input{flex:1;background:none;border:0;outline:0;color:var(--text-1)}.notice,.error{padding:10px 12px;margin-bottom:10px;border-radius:5px}.notice{color:var(--success);background:rgba(44,230,160,.08);border:1px solid rgba(44,230,160,.2)}.error{color:var(--danger);background:rgba(255,90,110,.08);border:1px solid rgba(255,90,110,.2)}.kb-layout{display:grid;grid-template-columns:360px minmax(0,1fr);gap:10px;min-height:calc(100vh - 142px)}.kb-list{display:flex;flex-direction:column;gap:8px;overflow:auto}.kb-card{min-height:92px;padding:12px;display:grid;grid-template-columns:40px 1fr auto;align-items:center;gap:10px;color:var(--text-2);text-align:left}.kb-card:hover,.kb-card.active{border-color:var(--line-strong);background:linear-gradient(135deg,rgba(13,50,74,.94),rgba(4,17,27,.94))}.db-icon{width:38px;height:38px;display:grid;place-items:center;border-radius:6px;background:rgba(25,134,255,.13);color:var(--primary-2)}.kb-card>span:nth-child(2){display:grid;gap:5px}.kb-card b{color:var(--text-1);font-size:12px}.kb-card small,.kb-card em{font-size:10px;color:var(--text-3);font-style:normal}.kb-card>i{font-style:normal;font-size:9px}.kb-card>i.ready{color:var(--success)}.kb-detail{padding:15px;display:flex;flex-direction:column;gap:15px}.summary{display:grid;grid-template-columns:repeat(4,1fr);gap:8px}.summary>div{padding:12px;border:1px solid var(--line);background:rgba(11,39,58,.2);display:grid;gap:7px}.summary span{color:var(--text-3);font-size:10px}.summary b{font-size:13px;overflow-wrap:anywhere}.meta{padding-bottom:12px;border-bottom:1px solid var(--line)}.meta h3{font-size:12px}.meta .tag{margin:0 5px 5px 0}.documents{flex:1}.doc-row{min-height:54px;border-bottom:1px solid var(--line);display:grid;grid-template-columns:1fr 80px 90px;align-items:center;gap:10px}.doc-row>span:first-child{display:grid;gap:4px}.doc-row small,.doc-row em{color:var(--text-3);font-size:10px;font-style:normal}.doc-empty{padding:28px;text-align:center;color:var(--text-3)}.upload-zone{width:100%;min-height:90px;border:1px dashed var(--line-strong);border-radius:7px;display:grid;place-items:center;align-content:center;gap:6px;color:var(--primary-2);background:rgba(25,134,255,.045)}.upload-zone span{font-size:10px;color:var(--text-3)}
.dialog-mask{position:fixed;inset:0;z-index:35;display:grid;place-items:center;background:rgba(0,5,10,.76);backdrop-filter:blur(5px)}.dialog{width:min(520px,calc(100vw - 32px));padding:0}.dialog header,.dialog footer{padding:16px 20px;display:flex;align-items:center;justify-content:space-between;border-bottom:1px solid var(--line)}.dialog header h2{margin:0}.dialog header button{border:0;background:transparent;color:var(--text-3)}.dialog footer{border:0;border-top:1px solid var(--line);justify-content:flex-end;gap:8px}.form{padding:18px 20px;display:grid;gap:13px}.form label{display:grid;gap:6px;color:var(--text-2)}.dialog-error{padding:0 20px;color:var(--danger)}@media(max-width:1000px){.kb-layout{grid-template-columns:1fr}.summary{grid-template-columns:repeat(2,1fr)}}
</style>
