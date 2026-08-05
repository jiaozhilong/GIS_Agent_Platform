import { defineStore } from 'pinia'
import { ref } from 'vue'
import { api } from '@/api/services'
import type { RegisterRequest, UserProfile } from '@/api/contracts'

export const useAuthStore = defineStore('auth', () => {
  const cachedUser = localStorage.getItem('gis-agent-user')
  const user = ref<UserProfile | null>(cachedUser ? JSON.parse(cachedUser) as UserProfile : null)
  const loading = ref(false)
  const error = ref('')

  async function login(account: string, password: string) {
    loading.value = true
    error.value = ''
    try {
      const result = await api.login({ account, password })
      localStorage.setItem('gis-agent-token', result.accessToken)
      localStorage.setItem('gis-agent-user', JSON.stringify(result.user))
      user.value = result.user
      return true
    } catch (e) {
      error.value = e instanceof Error ? e.message : '登录失败'
      return false
    } finally { loading.value = false }
  }

  async function register(body: RegisterRequest) {
    loading.value = true
    error.value = ''
    try {
      const result = await api.register(body)
      localStorage.setItem('gis-agent-token', result.accessToken)
      localStorage.setItem('gis-agent-user', JSON.stringify(result.user))
      user.value = result.user
      return true
    } catch (e) {
      error.value = e instanceof Error ? e.message : '注册失败'
      return false
    } finally { loading.value = false }
  }

  async function hydrate() {
    if (!localStorage.getItem('gis-agent-token')) return
    try {
      user.value = await api.me()
      localStorage.setItem('gis-agent-user', JSON.stringify(user.value))
    } catch {
      logout()
    }
  }

  function logout() {
    localStorage.removeItem('gis-agent-token')
    localStorage.removeItem('gis-agent-user')
    user.value = null
  }

  return { user, loading, error, login, register, hydrate, logout }
})
