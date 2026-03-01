import { fetchApi } from '@shared/api/client'
import type { ApiResponse } from '@shared/types/api'
import type {
  CreateReturnRequest,
  RejectReturnRequest,
  ReturnListResponse,
  ReturnShipment,
} from './types'

export async function requestReturn(
  orderId: number,
  request: CreateReturnRequest
): Promise<ApiResponse<ReturnShipment>> {
  return fetchApi<ReturnShipment>(`/api/orders/${orderId}/return`, {
    method: 'POST',
    body: JSON.stringify(request),
  })
}

export async function getReturn(orderId: number, authContext: 'customer' | 'bo' = 'customer'): Promise<ApiResponse<ReturnShipment>> {
  const endpoint =
    authContext === 'bo'
      ? `/api/admin/orders/${orderId}/return`
      : `/api/orders/${orderId}/return`
  return fetchApi<ReturnShipment>(endpoint, {}, authContext)
}

export async function approveReturn(orderId: number): Promise<ApiResponse<ReturnShipment>> {
  return fetchApi<ReturnShipment>(`/api/admin/orders/${orderId}/return/approve`, { method: 'POST' }, 'bo')
}

export async function rejectReturn(
  orderId: number,
  request: RejectReturnRequest
): Promise<ApiResponse<ReturnShipment>> {
  return fetchApi<ReturnShipment>(
    `/api/admin/orders/${orderId}/return/reject`,
    {
      method: 'POST',
      body: JSON.stringify(request),
    },
    'bo'
  )
}

export async function confirmReturn(orderId: number): Promise<ApiResponse<ReturnShipment>> {
  return fetchApi<ReturnShipment>(`/api/admin/orders/${orderId}/return/confirm`, { method: 'POST' }, 'bo')
}

export async function getAllReturns(status?: string): Promise<ApiResponse<ReturnListResponse>> {
  const query = new URLSearchParams()
  if (status) {
    query.set('status', status)
  }
  const suffix = query.toString()
  return fetchApi<ReturnListResponse>(suffix ? `/api/admin/returns?${suffix}` : '/api/admin/returns', {}, 'bo')
}
