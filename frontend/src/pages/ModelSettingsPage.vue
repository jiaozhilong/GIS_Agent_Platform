<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { IconCheck, IconPlugConnected, IconRefresh } from '@tabler/icons-vue'
import AppShell from '@/components/layout/AppShell.vue'
import { api } from '@/api/services'
import type { ModelConfig } from '@/api/contracts'

const configs = ref<ModelConfig[]>([])
const testing = ref('')
const saved = ref('')
const error = ref('')

const replace = (updated: ModelConfig) => {
  const index = configs.value.findIndex(item => item.provider === updated.provider)
  if (index >= 0) configs.value[index] = updated
}
const load = async () => { try { configs.value = await api.modelConfigs() } catch (e) { error.value = e instanceof Error ? e.message : '配置加载失败' } }
const test = async (provider: ModelConfig['provider']) => {
  testing.value = provider; error.value = ''
  try { replace(await api.testModelConfig(provider)) }
  catch (e) { error.value = e instanceof Error ? e.message : '连接测试失败' }
  finally { testing.value = '' }
}
const save = async (config: ModelConfig) => {
  saved.value = config.provider; error.value = ''
  try { replace(await api.saveModelConfig(config.provider, { baseUrl: config.baseUrl, modelName: config.modelName })) }
  catch (e) { error.value = e instanceof Error ? e.message : '配置保存失败' }
  finally { saved.value = '' }
}
onMounted(load)
</script>

<template>
  <AppShell title="模型与中台配置" subtitle="检测 DeepSeek、RAGFlow 和本地 BGE-M3 的真实连接状态">
    <div class="settings-intro tech-panel"><IconPlugConnected :size="22"/><div><b>配置安全边界</b><span>API Key 只保存在 Spring Boot 本地配置中；页面展示脱敏值，并通过后端执行真实连通性检测。</span></div></div>
    <p v-if="error" class="error">{{error}}</p>
    <div class="model-grid"><section v-for="config in configs" :key="config.provider" class="model-card tech-panel"><div class="model-head"><span><i :class="['status-dot',{bad:config.status!=='HEALTHY'}]"/><b>{{config.displayName}}</b></span><em :class="config.status.toLowerCase()">{{config.status}}</em></div><label>服务地址<input v-model="config.baseUrl" class="tech-input"/></label><label>模型名称<input v-model="config.modelName" class="tech-input"/></label><label v-if="config.maskedApiKey">API Key<input :value="config.maskedApiKey" class="tech-input" type="password" readonly/></label><div class="message">{{config.message || '等待检测'}}</div><div class="details"><span>最后检测</span><b>{{config.lastCheckedAt?.slice(0,19).replace('T',' ') || '尚未检测'}}</b></div><footer><button class="ghost-button" :disabled="testing===config.provider" @click="test(config.provider)"><IconRefresh :size="15"/>{{testing===config.provider?'检测中...':'测试连接'}}</button><button class="primary-button" :disabled="saved===config.provider" @click="save(config)"><IconCheck :size="15"/>{{saved===config.provider?'保存中...':'保存配置'}}</button></footer></section></div>
  </AppShell>
</template>

<style scoped>
.settings-intro{padding:14px;display:flex;gap:12px;align-items:center;margin-bottom:12px;color:var(--primary-2)}.settings-intro div{display:grid;gap:5px}.settings-intro span{color:var(--text-3);font-size:11px}.error{padding:10px;color:var(--danger);border:1px solid rgba(255,90,110,.25)}.model-grid{display:grid;grid-template-columns:repeat(3,minmax(280px,1fr));gap:10px}.model-card{padding:18px;display:flex;flex-direction:column;gap:15px;min-height:390px}.model-head{display:flex;justify-content:space-between;align-items:center;padding-bottom:12px;border-bottom:1px solid var(--line)}.model-head>span{display:flex;align-items:center;gap:8px}.model-head em{font-style:normal;font-size:10px}.model-head em.healthy{color:var(--success)}.model-head em.unavailable,.model-head em.unconfigured{color:var(--danger)}.status-dot.bad{background:var(--danger);box-shadow:0 0 8px var(--danger)}.model-card label{display:grid;gap:7px;color:var(--text-2);font-size:11px}.message{padding:9px;border:1px solid var(--line);border-radius:5px;color:var(--text-3);font-size:11px;background:rgba(10,34,50,.25)}.details{margin-top:auto;display:flex;justify-content:space-between;color:var(--text-3);font-size:10px}.details b{font-weight:500}.model-card footer{display:flex;justify-content:flex-end;gap:8px}@media(max-width:1100px){.model-grid{grid-template-columns:1fr 1fr}}@media(max-width:760px){.model-grid{grid-template-columns:1fr}}
</style>
