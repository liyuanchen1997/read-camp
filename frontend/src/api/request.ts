import axios, { type AxiosRequestConfig } from 'axios'
import { TOKEN_KEY } from '@/stores/user'

/** 后端统一返回包装（doc/00-design.md §2） */
export interface ApiResult<T = unknown> {
  code: number
  message: string
  data: T
}

const request = axios.create({
  baseURL: '/api',
  timeout: 20000,
})

// 注入 token
request.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_KEY)
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// 统一解包 + 401 处理
request.interceptors.response.use(
  (response) => {
    const result = response.data as ApiResult
    if (result.code !== 0) {
      return Promise.reject(new Error(result.message || '请求失败'))
    }
    // 运行时返回解包后的 data，类型断言仅为满足 axios 拦截器签名
    return result.data as unknown as typeof response
  },
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem(TOKEN_KEY)
      // 跳转登录（保留原路径回跳）
      const redirect = encodeURIComponent(location.pathname + location.search)
      if (!location.pathname.startsWith('/login')) {
        location.href = `/login?redirect=${redirect}`
      }
    }
    return Promise.reject(error)
  },
)

/** 泛型请求助手：返回解包后的 data */
export function http<T = unknown>(config: AxiosRequestConfig): Promise<T> {
  return request(config) as unknown as Promise<T>
}

export default request
