import { fetchApi } from '@shared/api/client'
import { APP_MODE } from '@shared/config/env'
import type { ApiResponse } from '@shared/types/api'
import type { Order, CreateOrderRequest, AdminOrderSearchParams, AdminOrderListResponse } from './types'

export async function createOrder(request: CreateOrderRequest): Promise<ApiResponse<Order>> {
  return fetchApi<Order>('/api/orders', {
    method: 'POST',
    body: JSON.stringify(request),
  })
}

export async function getOrderById(id: number): Promise<ApiResponse<Order>> {
  return fetchApi<Order>(`/api/orders/${id}`)
}

export async function getOrderHistory(): Promise<ApiResponse<Order[]>> {
  return fetchApi<Order[]>('/api/orders/history')
}

export async function cancelOrder(orderId: number): Promise<ApiResponse<Order>> {
  const endpoint =
    APP_MODE === 'admin' ? `/api/order/${orderId}/cancel` : `/api/orders/${orderId}/cancel`
  return fetchApi<Order>(endpoint, { method: 'POST' })
}

export async function confirmOrder(orderId: number): Promise<ApiResponse<Order>> {
  return fetchApi<Order>(`/api/order/${orderId}/confirm`, { method: 'POST' }, 'bo')
}

export async function shipOrder(orderId: number): Promise<ApiResponse<Order>> {
  return fetchApi<Order>(`/api/order/${orderId}/ship`, { method: 'POST' }, 'bo')
}

export async function deliverOrder(orderId: number): Promise<ApiResponse<Order>> {
  return fetchApi<Order>(`/api/order/${orderId}/deliver`, { method: 'POST' }, 'bo')
}

export async function retryAllocation(orderId: number): Promise<ApiResponse<Order>> {
  return fetchApi<Order>(`/api/admin/orders/${orderId}/allocation/retry`, { method: 'POST' }, 'bo')
}

export async function getAllOrders(
  params: AdminOrderSearchParams = {}
): Promise<ApiResponse<AdminOrderListResponse>> {
  const query = new URLSearchParams()
  if (params.orderNumber) query.set('orderNumber', params.orderNumber)
  if (params.customerEmail) query.set('customerEmail', params.customerEmail)
  if (params.statuses && params.statuses.length > 0) query.set('statuses', params.statuses.join(','))
  if (params.dateFrom) query.set('dateFrom', params.dateFrom)
  if (params.dateTo) query.set('dateTo', params.dateTo)
  if (params.totalPriceMin !== undefined) query.set('totalPriceMin', String(params.totalPriceMin))
  if (params.totalPriceMax !== undefined) query.set('totalPriceMax', String(params.totalPriceMax))
  if (params.allocationIncomplete !== undefined) query.set('allocationIncomplete', String(params.allocationIncomplete))
  if (params.unshipped !== undefined) query.set('unshipped', String(params.unshipped))
  query.set('page', String(params.page ?? 1))
  query.set('limit', String(params.limit ?? 20))
  return fetchApi<AdminOrderListResponse>(`/api/order?${query.toString()}`, {}, 'bo')
}
