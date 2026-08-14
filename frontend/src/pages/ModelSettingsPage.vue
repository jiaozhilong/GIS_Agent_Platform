<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { IconCheck, IconCpu, IconKey, IconPlugConnected, IconRefresh, IconRoute, IconSparkles } from '@tabler/icons-vue'
import AppShell from '@/components/layout/AppShell.vue'
import { api } from '@/api/services'
import type { ModelConfig } from '@/api/contracts'

const configs = ref<ModelConfig[]>([])
const testing = ref('')
const saved = ref('')
const error = ref('')
const notice = ref('')
const testResult = ref<ModelConfig | null>(null)
const draft = reactive({ baseUrl: '', modelName: '', apiKey: '', enabled: false })
const knowledgeConfigs = computed(() => configs.value.filter(item => item.category === 'KNOWLEDGE_ENGINE'))
const persistedPlatform = computed(() => configs.value.find(item => item.provider === 'PLATFORM_LLM'))
const platformConfig = computed(() => testResult.value || persistedPlatform.value)
const replace = (updated: ModelConfig) => { const index = configs.value.findIndex(item => item.provider === updated.provider); if (index >= 0) configs.value[index] = updated }
const applyDraft = (config: ModelConfig) => { draft.baseUrl = config.baseUrl || ''; draft.modelName = config.modelName || ''; draft.apiKey = ''; draft.enabled = config.enabled }
const request = () => ({ baseUrl: draft.baseUrl.trim(), modelName: draft.modelName.trim(), apiKey: draft.apiKey || undefined, enabled: draft.enabled })
const load = async () => { try { configs.value = await api.modelConfigs(); const config = persistedPlatform.value; if (config) applyDraft(config) } catch (e) { error.value = e instanceof Error ? e.message : '配置加载失败' } }
const test = async (provider: ModelConfig['provider']) => { testing.value = provider; error.value = ''; notice.value = ''; try { replace(await api.testModelConfig(provider)) } catch (e) { error.value = e instanceof Error ? e.message : '连接测试失败' } finally { testing.value = '' } }
const testPlatform = async () => {
  if (!draft.baseUrl.trim() || !draft.modelName.trim() || (!draft.apiKey && !persistedPlatform.value?.maskedApiKey)) { error.value = '请填写服务地址、模型名称和 API Key 后再测试'; return }
  testing.value = 'PLATFORM_LLM'; error.value = ''; notice.value = ''; testResult.value = null
  try {
    testResult.value = await api.testModelConfig('PLATFORM_LLM', request())
    if (testResult.value.status === 'HEALTHY') notice.value = '连接成功：当前服务地址、API Key 和模型名称均可用'
    else error.value = testResult.value.message || '连接测试未通过，请检查服务地址、API Key 和模型名称'
  } catch (e) { error.value = e instanceof Error ? e.message : '连接测试失败' }
  finally { testing.value = '' }
}
const savePlatform = async () => {
  if (draft.enabled && (!draft.baseUrl.trim() || !draft.modelName.trim() || (!draft.apiKey && !persistedPlatform.value?.maskedApiKey))) { error.value = '启用模型前必须填写服务地址、模型名称和 API Key'; return }
  saved.value = 'PLATFORM_LLM'; error.value = ''; notice.value = ''; testResult.value = null
  try {
    const savedConfig = await api.saveModelConfig('PLATFORM_LLM', request())
    replace(savedConfig)
    draft.apiKey = ''
    if (savedConfig.baseUrl && savedConfig.modelName && savedConfig.maskedApiKey) {
      testResult.value = await api.testModelConfig('PLATFORM_LLM', request())
      replace(testResult.value)
      notice.value = testResult.value.status === 'HEALTHY'
        ? (draft.enabled ? '配置已保存并启用，后续方案将使用该平台模型' : '配置已保存且连接正常；启用后即可用于方案生成')
        : ''
      if (testResult.value.status !== 'HEALTHY') error.value = `配置已保存，但连接测试未通过：${testResult.value.message || '请检查参数'}`
    } else {
      notice.value = '配置已保存；当前未启用，方案生成将回退到 RAGFlow Assistant'
    }
  } catch (e) { error.value = e instanceof Error ? e.message : '配置保存失败' }
  finally { saved.value = '' }
}
onMounted(load)
</script>

<template>
  <AppShell title="模型路由" subtitle="将知识引擎与平台方案生成模型解耦，按职责编排模型能力">
    <section class="routing-hero">
      <div class="route-node source"><IconPlugConnected/><span><b>RAGFlow</b><small>知识解析与检索</small></span></div><span class="route-line"><i/></span><div class="route-core"><IconRoute/><b>MODEL ROUTER</b><small>证据与生成分层</small></div><span class="route-line"><i/></span><div class="route-node target"><IconSparkles/><span><b>{{ draft.enabled ? (draft.modelName || '待配置模型') : 'RAGFlow Assistant' }}</b><small>方案语言生成</small></span></div>
    </section>
    <div v-if="notice" class="notice">{{ notice }}</div><p v-if="error" class="error">{{ error }}</p>

    <section class="section-head"><div><span>KNOWLEDGE ENGINE</span><h2>RAGFlow 内部能力</h2><p>以下连接用于文档解析、向量召回和回退生成，由 RAGFlow 与本地环境维护。</p></div><span class="readonly-tag">只读检测</span></section>
    <div class="knowledge-grid">
      <article v-for="config in knowledgeConfigs" :key="config.provider" class="model-card tech-panel">
        <header><span class="model-icon"><IconCpu :size="18"/></span><div><b>{{ config.displayName }}</b><small>{{ config.modelName || '等待读取' }}</small></div><em :class="config.status.toLowerCase()">{{ config.status }}</em></header>
        <dl><dt>服务地址</dt><dd>{{ config.baseUrl }}</dd><dt v-if="config.maskedApiKey">凭据</dt><dd v-if="config.maskedApiKey">{{ config.maskedApiKey }}</dd></dl><p>{{ config.message }}</p>
        <footer><span>{{ config.lastCheckedAt?.slice(0,19).replace('T',' ') || '尚未检测' }}</span><button class="ghost-button" :disabled="testing===config.provider" @click="test(config.provider)"><IconRefresh :size="14"/>{{ testing===config.provider?'检测中':'测试连接' }}</button></footer>
      </article>
    </div>

    <section class="section-head platform-title"><div><span>PLATFORM GENERATION</span><h2>平台方案生成模型</h2><p>支持 GPT、DeepSeek、Qwen 等 OpenAI Chat Completions 兼容服务；可先测试当前参数，再保存并应用。</p></div><label class="switch"><input v-model="draft.enabled" type="checkbox"/><i/><span>{{ draft.enabled ? '保存后启用' : '保存后停用' }}</span></label></section>
    <section v-if="platformConfig" :class="['platform-card tech-panel',{enabled:draft.enabled}]">
      <div class="platform-status"><span class="model-icon large"><IconSparkles :size="25"/></span><div><span class="eyebrow">PLATFORM LANGUAGE MODEL</span><h3>{{ draft.modelName || '尚未选择模型' }}</h3><p>{{ platformConfig.message || '等待配置或测试' }}</p></div><em :class="platformConfig.status.toLowerCase()">{{ platformConfig.status }}</em></div>
      <div class="platform-form"><label>OpenAI 兼容服务地址<input v-model="draft.baseUrl" class="tech-input" placeholder="https://api.openai.com/v1" @input="testResult=null"/></label><label>模型名称<input v-model="draft.modelName" class="tech-input" placeholder="例如 gpt-5、deepseek-chat、qwen-plus" @input="testResult=null"/></label><label>API Key<div class="key-input"><IconKey :size="16"/><input v-model="draft.apiKey" type="password" :placeholder="persistedPlatform?.maskedApiKey ? `${persistedPlatform.maskedApiKey}（已安全保存，留空保持不变）` : '输入 API Key'" @input="testResult=null"/></div></label></div>
      <div class="routing-note"><IconRoute :size="17"/><span><b>保存与使用规则</b><small>“测试当前参数”直接验证输入框内容，不要求预先保存；“保存并应用”写入数据库并自动复测。API Key 加密保存且不回显明文。</small></span></div>
      <footer><button class="ghost-button" :disabled="testing==='PLATFORM_LLM' || saved==='PLATFORM_LLM'" @click="testPlatform"><IconRefresh :size="15"/>{{ testing==='PLATFORM_LLM'?'正在请求模型...':'测试当前参数' }}</button><button class="primary-button" :disabled="saved==='PLATFORM_LLM' || testing==='PLATFORM_LLM'" @click="savePlatform"><IconCheck :size="15"/>{{ saved==='PLATFORM_LLM'?'保存并验证中...':'保存并应用' }}</button></footer>
    </section>
  </AppShell>
</template>

<style scoped>
.routing-hero{min-height:118px;padding:20px 34px;display:grid;grid-template-columns:minmax(190px,1fr) minmax(70px,.45fr) 170px minmax(70px,.45fr) minmax(190px,1fr);align-items:center;border:1px solid rgba(53,174,232,.22);border-radius:12px;background:radial-gradient(circle at 50% 50%,rgba(25,175,232,.13),transparent 25%),linear-gradient(110deg,rgba(7,28,43,.95),rgba(3,14,23,.92));overflow:hidden}.route-node{display:flex;align-items:center;gap:12px;padding:13px;border:1px solid var(--line);border-radius:9px;background:rgba(4,18,29,.68);color:var(--primary-2)}.route-node span{display:grid;gap:4px}.route-node b{color:var(--text-1)}.route-node small{color:var(--text-3);font-size:10px}.route-core{width:148px;height:76px;justify-self:center;display:grid;place-items:center;align-content:center;gap:4px;border:1px solid rgba(42,212,239,.38);border-radius:50%;color:var(--cyan);background:radial-gradient(circle,rgba(22,139,212,.2),rgba(3,17,28,.9));box-shadow:0 0 32px rgba(20,177,225,.12)}.route-core b{font-size:10px;letter-spacing:1px}.route-core small{color:var(--text-3);font-size:8px}.route-line{height:1px;background:linear-gradient(90deg,var(--line),var(--cyan),var(--line));position:relative}.route-line i{position:absolute;top:-2px;width:5px;height:5px;border-radius:50%;background:var(--cyan);box-shadow:0 0 8px var(--cyan);animation:flow 2.2s infinite linear}.section-head{display:flex;align-items:end;justify-content:space-between;margin:20px 2px 10px}.section-head>div>span,.eyebrow{color:var(--cyan);font-size:9px;letter-spacing:1.7px}.section-head h2{font-size:17px;margin:5px 0 4px}.section-head p{margin:0;color:var(--text-3);font-size:10px}.readonly-tag{padding:5px 9px;border:1px solid var(--line);border-radius:999px;color:var(--text-3);font-size:9px}.knowledge-grid{display:grid;grid-template-columns:repeat(3,1fr);gap:10px}.model-card{min-height:222px;padding:16px;display:flex;flex-direction:column;gap:13px}.model-card header{display:grid;grid-template-columns:38px 1fr auto;align-items:center;gap:9px}.model-icon{width:36px;height:36px;display:grid;place-items:center;border:1px solid rgba(45,171,232,.28);border-radius:9px;color:var(--cyan);background:rgba(20,121,195,.09)}.model-card header>div{display:grid;gap:4px}.model-card header small{color:var(--text-3);font-size:9px}.model-card em,.platform-status>em{font-style:normal;font-size:9px}.healthy{color:var(--success)}.unavailable,.unconfigured{color:var(--warning)}.model-card dl{display:grid;grid-template-columns:68px 1fr;gap:8px;margin:0;padding:10px;border:1px solid var(--line);border-radius:6px;font-size:9px}.model-card dt{color:var(--text-3)}.model-card dd{margin:0;color:var(--text-2);overflow-wrap:anywhere}.model-card>p{margin:0;color:var(--text-3);font-size:10px}.model-card footer{margin-top:auto;display:flex;align-items:center;justify-content:space-between}.model-card footer>span{color:var(--text-3);font-size:9px}.platform-title{margin-top:26px}.switch{display:flex;align-items:center;gap:8px;color:var(--text-2);font-size:10px}.switch input{display:none}.switch i{width:38px;height:20px;border-radius:99px;background:#172c3a;position:relative;transition:.2s}.switch i:after{content:'';position:absolute;width:14px;height:14px;left:3px;top:3px;border-radius:50%;background:#668092;transition:.2s}.switch input:checked+i{background:rgba(25,157,220,.42)}.switch input:checked+i:after{transform:translateX(18px);background:var(--cyan);box-shadow:0 0 9px var(--cyan)}.platform-card{padding:19px;display:grid;grid-template-columns:240px minmax(480px,1fr) 250px;gap:18px;align-items:center;transition:.2s}.platform-card.enabled{border-color:rgba(46,183,229,.32);box-shadow:0 0 35px rgba(20,157,211,.07)}.platform-status{display:grid;grid-template-columns:50px 1fr auto;gap:10px;align-items:center}.model-icon.large{width:48px;height:48px}.platform-status h3{margin:4px 0}.platform-status p{margin:0;color:var(--text-3);font-size:9px}.platform-form{display:grid;grid-template-columns:1fr 1fr;gap:10px}.platform-form label{display:grid;gap:6px;color:var(--text-2);font-size:10px}.platform-form label:last-child{grid-column:1/-1}.key-input{height:40px;padding:0 11px;display:flex;align-items:center;gap:8px;border:1px solid var(--line);border-radius:6px;background:rgba(2,10,17,.72);color:var(--text-3)}.key-input input{flex:1;border:0;outline:0;background:transparent;color:var(--text-1)}.routing-note{display:flex;gap:9px;align-items:center;padding:11px;border:1px solid var(--line);border-radius:7px;color:var(--primary-2);background:rgba(18,98,152,.08)}.routing-note span{display:grid;gap:5px}.routing-note small{color:var(--text-3);font-size:9px;line-height:1.5}.platform-card>footer{grid-column:1/-1;display:flex;justify-content:flex-end;gap:8px;padding-top:12px;border-top:1px solid var(--line)}.notice,.error{padding:10px 12px;margin:10px 0;border-radius:5px}.notice{color:var(--success);border:1px solid rgba(44,230,160,.2)}.error{color:var(--danger);border:1px solid rgba(255,90,110,.25)}@keyframes flow{from{left:0}to{left:100%}}@media(max-width:1200px){.platform-card{grid-template-columns:1fr 1fr}.routing-note{grid-column:1/-1}}@media(max-width:900px){.routing-hero{grid-template-columns:1fr}.route-line{display:none}.route-core{margin:8px}.knowledge-grid,.platform-card{grid-template-columns:1fr}.platform-form{grid-template-columns:1fr}.platform-form label:last-child{grid-column:auto}}
</style>
