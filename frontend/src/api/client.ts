import axios, { AxiosError } from 'axios'
import { mockRequest } from './mock'
import type { ApiErrorPayload, ApiResponse } from './contracts'

const useMocks = import.meta.env.VITE_USE_MOCKS !== 'false'
const instance = axios.create({ baseURL: import.meta.env.VITE_API_BASE_URL || '/api/v1', timeout: 30000 })

instance.interceptors.request.use((config) => {
  const token = localStorage.getItem('gis-agent-token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  config.headers['X-Request-Id'] = crypto.randomUUID()
  return config
})

export class ApiClientError extends Error {
  constructor(public payload: ApiErrorPayload, public status?: number) { super(payload.message) }
}

export async function apiRequest<T>(method: 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE', path: string, body?: unknown): Promise<T> {
  if (useMocks) return (await mockRequest<T>(method, path, body)).data
  try {
    const response = await instance.request<ApiResponse<T>>({ method, url: path, data: body })
    return response.data.data
  } catch (error) {
    const axiosError = error as AxiosError<ApiErrorPayload>
    const payload = axiosError.response?.data ?? { code: 'UPSTREAM_ERROR', message: '服务暂时不可用，请稍后重试', requestId: crypto.randomUUID(), timestamp: new Date().toISOString() }
    throw new ApiClientError(payload, axiosError.response?.status)
  }
}
