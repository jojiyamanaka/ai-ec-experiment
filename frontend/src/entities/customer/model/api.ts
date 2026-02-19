import { fetchApi } from '@shared/api/client'
import type { ApiResponse } from '@shared/types/api'
import type { User, AuthResponse, RegisterRequest, LoginRequest } from './types'

export async function register(request: RegisterRequest): Promise<ApiResponse<AuthResponse>> {
  return fetchApi<AuthResponse>('/api/auth/register', {
    method: 'POST',
    body: JSON.stringify(request),
  })
}

export async function login(request: LoginRequest): Promise<ApiResponse<AuthResponse>> {
  return fetchApi<AuthResponse>('/api/auth/login', {
    method: 'POST',
    body: JSON.stringify(request),
  })
}

export async function logout(): Promise<ApiResponse<{ message: string }>> {
  return fetchApi<{ message: string }>('/api/auth/logout', { method: 'POST' })
}

export async function getCurrentUser(): Promise<ApiResponse<User>> {
  return fetchApi<User>('/api/auth/me')
}
