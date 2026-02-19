import { fetchApi } from '@shared/api/client'
import type { ApiResponse } from '@shared/types/api'
import type { Cart, AddToCartRequest, UpdateQuantityRequest } from './types'

export async function getCart(): Promise<ApiResponse<Cart>> {
  return fetchApi<Cart>('/api/cart')
}

export async function addToCart(request: AddToCartRequest): Promise<ApiResponse<Cart>> {
  return fetchApi<Cart>('/api/cart/items', {
    method: 'POST',
    body: JSON.stringify(request),
  })
}

export async function updateCartItemQuantity(
  itemId: number,
  request: UpdateQuantityRequest
): Promise<ApiResponse<Cart>> {
  return fetchApi<Cart>(`/api/cart/items/${itemId}`, {
    method: 'PUT',
    body: JSON.stringify(request),
  })
}

export async function removeFromCart(itemId: number): Promise<ApiResponse<Cart>> {
  return fetchApi<Cart>(`/api/cart/items/${itemId}`, { method: 'DELETE' })
}
