import type { ApiResponse } from '@shared/types/api'
import { APP_MODE, API_BASE_URL } from '@shared/config/env'

export type AuthContext = 'auto' | 'customer' | 'bo'

// ⚠ 技術的負債: bo_token/authToken の localStorage 参照がここに残る
// → Phase 2 で token getter コールバックを注入する形に分離予定
export const getSessionId = (): string => {
  let sessionId = localStorage.getItem('sessionId')
  if (!sessionId) {
    sessionId = `session-${Date.now()}-${Math.random().toString(36).substring(7)}`
    localStorage.setItem('sessionId', sessionId)
  }
  return sessionId
}

export async function fetchApi<T>(
  endpoint: string,
  options: RequestInit = {},
  authContext: AuthContext = 'auto'
): Promise<ApiResponse<T>> {
  const url = `${API_BASE_URL}${endpoint}`
  const headers = new Headers(options.headers)
  if (!headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json')
  }

  const boToken = localStorage.getItem('bo_token')
  const isBoEndpoint = endpoint.startsWith('/api/bo') || endpoint.startsWith('/api/bo-auth')
  const shouldUseBoAuthContext =
    authContext === 'bo' || (authContext === 'auto' && APP_MODE === 'admin')

  if (shouldUseBoAuthContext) {
    if (boToken && !endpoint.endsWith('/bo-auth/login')) {
      headers.set('Authorization', `Bearer ${boToken}`)
    }
  } else if (!isBoEndpoint) {
    const token = localStorage.getItem('authToken')
    if (token && !endpoint.includes('/auth/login') && !endpoint.includes('/auth/register')) {
      headers.set('Authorization', `Bearer ${token}`)
    }
  }

  if (endpoint.includes('/order')) {
    headers.set('X-Session-Id', getSessionId())
  }

  try {
    const response = await fetch(url, { ...options, headers })

    if (!response.ok && response.status === 401) {
      if (shouldUseBoAuthContext || isBoEndpoint) {
        localStorage.removeItem('bo_token')
        window.dispatchEvent(new Event('bo-auth:unauthorized'))
      } else {
        localStorage.removeItem('authToken')
        localStorage.removeItem('authUser')
        window.dispatchEvent(new Event('auth:unauthorized'))
      }
    }

    const data = await response.json()
    return data
  } catch (error) {
    console.error('API Error:', error)
    return {
      success: false,
      error: {
        code: 'NETWORK_ERROR',
        message: 'ネットワークエラーが発生しました',
      },
    }
  }
}

function normalizeEndpoint(endpoint: string): string {
  return endpoint.startsWith('/api') ? endpoint : `/api${endpoint}`
}

export async function get<T>(
  endpoint: string,
  authContext: AuthContext = 'auto'
): Promise<ApiResponse<T>> {
  return fetchApi<T>(normalizeEndpoint(endpoint), {}, authContext)
}

export async function post<T>(
  endpoint: string,
  body?: unknown,
  authContext: AuthContext = 'auto'
): Promise<ApiResponse<T>> {
  return fetchApi<T>(
    normalizeEndpoint(endpoint),
    { method: 'POST', body: body === undefined ? undefined : JSON.stringify(body) },
    authContext
  )
}

export async function put<T>(
  endpoint: string,
  body?: unknown,
  authContext: AuthContext = 'auto'
): Promise<ApiResponse<T>> {
  return fetchApi<T>(
    normalizeEndpoint(endpoint),
    { method: 'PUT', body: body === undefined ? undefined : JSON.stringify(body) },
    authContext
  )
}
