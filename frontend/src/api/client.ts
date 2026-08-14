import axios, { AxiosError, type AxiosProgressEvent } from 'axios'
import { mockRequest } from './mock'
import type { ApiErrorPayload, ApiResponse } from './contracts'

const useMocks = import.meta.env.VITE_USE_MOCKS === 'true'
const instance = axios.create({ baseURL: import.meta.env.VITE_API_BASE_URL || '/api/v1', timeout: 240000 })

instance.interceptors.request.use((config) => {
  const token = localStorage.getItem('gis-agent-token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  config.headers['X-Request-Id'] = crypto.randomUUID()
  return config
})

instance.interceptors.response.use(response => response, (error: unknown) => {
  const axiosError = error as AxiosError<ApiErrorPayload>
  const requestUrl = axiosError.config?.url || ''
  if (axiosError.response?.status === 401 && !requestUrl.endsWith('/auth/login')) {
    localStorage.removeItem('gis-agent-token')
    localStorage.removeItem('gis-agent-user')
    sessionStorage.setItem('gis-agent-session-message', '登录状态已失效，请重新登录后继续操作')
    if (window.location.pathname !== '/login') window.location.replace('/login?reason=expired')
  }
  return Promise.reject(error)
})

export class ApiClientError extends Error {
  constructor(public payload: ApiErrorPayload, public status?: number) { super(payload.message) }
}

function toClientError(error: unknown, fallback: string) {
  const axiosError = error as AxiosError<ApiErrorPayload>
  const payload = axiosError.response?.data ?? {
    code: 'UPSTREAM_ERROR' as const,
    message: fallback,
    requestId: crypto.randomUUID(),
    timestamp: new Date().toISOString()
  }
  return new ApiClientError(payload, axiosError.response?.status)
}

export async function apiRequest<T>(method: 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE', path: string, body?: unknown): Promise<T> {
  if (useMocks) return (await mockRequest<T>(method, path, body)).data
  try {
    const response = await instance.request<ApiResponse<T>>({ method, url: path, data: body })
    if (response.status === 204) return undefined as T
    return response.data.data
  } catch (error) {
    throw toClientError(error, '服务暂时不可用，请稍后重试')
  }
}

export async function apiUpload<T>(path: string, file: File): Promise<T> {
  const form = new FormData()
  form.append('file', file)
  try {
    const response = await instance.post<ApiResponse<T>>(path, form)
    return response.data.data
  } catch (error) {
    throw toClientError(error, '文件上传失败')
  }
}

export async function apiUploadWithFields<T>(path: string, file: File, fields: object,
                                              onProgress?: (percent: number) => void): Promise<T> {
  const form = new FormData()
  form.append('file', file)
  Object.entries(fields).forEach(([key, value]) => { if (value !== undefined && value !== null) form.append(key, String(value)) })
  try {
    const response = await instance.post<ApiResponse<T>>(path, form, {
      timeout: 7200000,
      onUploadProgress: (event: AxiosProgressEvent) => {
        if (event.total && onProgress) onProgress(Math.min(100, Math.round(event.loaded * 100 / event.total)))
      }
    })
    return response.data.data
  } catch (error) {
    throw toClientError(error, '知识资产上传失败')
  }
}

export async function apiBlob(path: string): Promise<Blob> {
  try { return (await instance.get(path, { responseType: 'blob' })).data as Blob }
  catch (error) { throw toClientError(error, '资产内容读取失败') }
}
