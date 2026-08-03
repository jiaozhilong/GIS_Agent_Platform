<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { IconCheck, IconPlugConnected, IconRefresh } from '@tabler/icons-vue'
import AppShell from '@/components/layout/AppShell.vue'
import { api } from '@/api/services'
import type { ModelConfig } from '@/api/contracts'
const configs=ref<ModelConfig[]>([]);const testing=ref('');const saved=ref('')
onMounted(async()=>{configs.value=await api.modelConfigs()})
const test=(provider:string)=>{testing.value=provider;setTimeout(()=>testing.value='',800)}
const save=(provider:string)=>{saved.value=provider;setTimeout(()=>saved.value='',1000)}
</script>
<template>
  <AppShell title="模型与中台配置" subtitle="管理 DeepSeek、RAGFlow 和本地 BGE-M3 连接参数">
    <div class="settings-intro tech-panel"><IconPlugConnected :size="22"/><div><b>前后端接口边界</b><span>密钥只保存在 Spring Boot 后端；前端只提交配置标识和可编辑地址，不持久化明文 API Key。</span></div></div>
    <div class="model-grid"><section v-for="config in configs" :key="config.provider" class="model-card tech-panel"><div class="model-head"><span><i class="status-dot"/><b>{{config.displayName}}</b></span><em>{{config.status}}</em></div><label>服务地址<input v-model="config.baseUrl" class="tech-input"/></label><label>模型名称<input v-model="config.modelName" class="tech-input"/></label><label v-if="config.provider==='DEEPSEEK'">API Key<input :value="config.maskedApiKey" class="tech-input" type="password"/></label><div class="details"><span>最后检测</span><b>{{config.lastCheckedAt?.slice(0,19).replace('T',' ')}}</b></div><footer><button class="ghost-button" @click="test(config.provider)"><IconRefresh :size="15"/>{{testing===config.provider?'检测中...':'测试连接'}}</button><button class="primary-button" @click="save(config.provider)"><IconCheck :size="15"/>{{saved===config.provider?'已保存':'保存配置'}}</button></footer></section></div>
  </AppShell>
</template>
<style scoped>
.settings-intro{padding:14px;display:flex;gap:12px;align-items:center;margin-bottom:12px;color:var(--primary-2)}.settings-intro div{display:grid;gap:5px}.settings-intro span{color:var(--text-3);font-size:11px}.model-grid{display:grid;grid-template-columns:repeat(3,minmax(280px,1fr));gap:10px}.model-card{padding:18px;display:flex;flex-direction:column;gap:15px;min-height:370px}.model-head{display:flex;justify-content:space-between;align-items:center;padding-bottom:12px;border-bottom:1px solid var(--line)}.model-head>span{display:flex;align-items:center;gap:8px}.model-head em{font-style:normal;color:var(--success);font-size:10px}.model-card label{display:grid;gap:7px;color:var(--text-2);font-size:11px}.details{margin-top:auto;display:flex;justify-content:space-between;color:var(--text-3);font-size:10px}.details b{font-weight:500}.model-card footer{display:flex;justify-content:flex-end;gap:8px}@media(max-width:1100px){.model-grid{grid-template-columns:1fr 1fr}}@media(max-width:760px){.model-grid{grid-template-columns:1fr}}
</style>
