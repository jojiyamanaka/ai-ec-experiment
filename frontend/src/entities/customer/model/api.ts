import { fetchApi, get, post, put } from '@shared/api/client'
import type { ApiResponse } from '@shared/types/api'
import type {
  User,
  UserAddress,
  AuthResponse,
  RegisterRequest,
  LoginRequest,
  CreateMemberRequest,
  UpdateMemberRequest,
  UpdateMyProfileRequest,
  UpsertAddressRequest,
} from './types'

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
  return fetchApi<User>('/api/members/me')
}

export async function updateMyProfile(request: UpdateMyProfileRequest): Promise<ApiResponse<User>> {
  return fetchApi<User>('/api/members/me', {
    method: 'PUT',
    body: JSON.stringify(request),
  })
}

export async function addMyAddress(request: UpsertAddressRequest): Promise<ApiResponse<UserAddress>> {
  return fetchApi<UserAddress>('/api/members/me/addresses', {
    method: 'POST',
    body: JSON.stringify(request),
  })
}

export async function updateMyAddress(
  addressId: number,
  request: UpsertAddressRequest
): Promise<ApiResponse<UserAddress>> {
  return fetchApi<UserAddress>(`/api/members/me/addresses/${addressId}`, {
    method: 'PUT',
    body: JSON.stringify(request),
  })
}

export async function deleteMyAddress(addressId: number): Promise<ApiResponse<{ success: boolean }>> {
  return fetchApi<{ success: boolean }>(`/api/members/me/addresses/${addressId}`, {
    method: 'DELETE',
  })
}

export async function getAdminMembers(): Promise<ApiResponse<User[]>> {
  return get<User[]>('/admin/members', 'bo')
}

export async function getAdminMemberById(id: number): Promise<ApiResponse<User>> {
  return get<User>(`/admin/members/${id}`, 'bo')
}

export async function createAdminMember(request: CreateMemberRequest): Promise<ApiResponse<User>> {
  return post<User>('/admin/members', request, 'bo')
}

export async function updateAdminMember(
  id: number,
  request: UpdateMemberRequest
): Promise<ApiResponse<User>> {
  return put<User>(`/admin/members/${id}`, request, 'bo')
}

export async function updateAdminMemberStatus(
  id: number,
  isActive: boolean
): Promise<ApiResponse<User>> {
  return put<User>(`/admin/members/${id}/status`, { isActive }, 'bo')
}
