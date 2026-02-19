import { post, get } from '@shared/api/client'
import type { ApiResponse } from '@shared/types/api'
import type { BoUser, BoAuthResponse } from './types'

export async function boLogin(
  email: string,
  password: string
): Promise<ApiResponse<BoAuthResponse>> {
  return post<BoAuthResponse>('/bo-auth/login', { email, password })
}

export async function boLogout(): Promise<ApiResponse<{ message: string }>> {
  return post<{ message: string }>('/bo-auth/logout', {})
}

export async function getBoUser(): Promise<ApiResponse<BoUser>> {
  return get<BoUser>('/bo-auth/me')
}
