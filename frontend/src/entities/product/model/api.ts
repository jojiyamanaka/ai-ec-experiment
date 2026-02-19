import { fetchApi } from '@shared/api/client'
import type { ApiResponse } from '@shared/types/api'
import type { Product, ProductListResponse, UpdateProductRequest } from './types'

export async function getItems(page = 1, limit = 20): Promise<ApiResponse<ProductListResponse>> {
  return fetchApi<ProductListResponse>(`/api/products?page=${page}&limit=${limit}`)
}

export async function getItemById(id: number): Promise<ApiResponse<Product>> {
  return fetchApi<Product>(`/api/products/${id}`)
}

export async function updateItem(
  id: number,
  updates: UpdateProductRequest
): Promise<ApiResponse<Product>> {
  return fetchApi<Product>(`/api/item/${id}`, {
    method: 'PUT',
    body: JSON.stringify(updates),
  }, 'bo')
}
