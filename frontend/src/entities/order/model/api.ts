import { fetchApi } from '@shared/api/client'
import { APP_MODE } from '@shared/config/env'
import type { ApiResponse } from '@shared/types/api'
import type { Order, CreateOrderRequest } from './types'

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

export async function getAllOrders(): Promise<ApiResponse<Order[]>> {
  const response = await fetchApi<{ orders?: Order[] } | Order[]>('/api/order', {}, 'bo')
  if (!response.success || !response.data) {
    return response as ApiResponse<Order[]>
  }
  if (Array.isArray(response.data)) {
    return response as ApiResponse<Order[]>
  }
  return {
    success: true,
    data: (response.data as { orders?: Order[] }).orders ?? [],
  }
}
