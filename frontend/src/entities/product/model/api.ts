import { fetchApi } from '@shared/api/client'
import type { ApiResponse } from '@shared/types/api'
import type {
  CreateProductCategoryRequest,
  CreateProductRequest,
  Product,
  AdminProductSearchParams,
  ProductCategory,
  ProductInventory,
  ProductListResponse,
  UpdateProductCategoryRequest,
  UpdateProductInventoryRequest,
  UpdateProductRequest,
} from './types'

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

export async function getAdminItems(params: AdminProductSearchParams = {}): Promise<ApiResponse<ProductListResponse>> {
  const query = new URLSearchParams()
  query.set('page', String(params.page ?? 1))
  query.set('limit', String(params.limit ?? 100))
  if (params.keyword) query.set('keyword', params.keyword)
  if (params.categoryId !== undefined) query.set('categoryId', String(params.categoryId))
  if (params.isPublished !== undefined) query.set('isPublished', String(params.isPublished))
  if (params.inSalePeriod !== undefined) query.set('inSalePeriod', String(params.inSalePeriod))
  if (params.allocationType) query.set('allocationType', params.allocationType)
  if (params.stockThreshold !== undefined) query.set('stockThreshold', String(params.stockThreshold))
  if (params.zeroStockOnly !== undefined) query.set('zeroStockOnly', String(params.zeroStockOnly))
  return fetchApi<ProductListResponse>(`/api/admin/items?${query.toString()}`, {}, 'bo')
}

export async function getAdminItemById(id: number): Promise<ApiResponse<Product>> {
  return fetchApi<Product>(`/api/admin/items/${id}`, {}, 'bo')
}

export async function createAdminItem(
  payload: CreateProductRequest
): Promise<ApiResponse<Product>> {
  return fetchApi<Product>(`/api/admin/items`, {
    method: 'POST',
    body: JSON.stringify(payload),
  }, 'bo')
}

export async function updateAdminItem(
  id: number,
  payload: UpdateProductRequest
): Promise<ApiResponse<Product>> {
  return fetchApi<Product>(`/api/admin/items/${id}`, {
    method: 'PUT',
    body: JSON.stringify(payload),
  }, 'bo')
}

export async function getAdminItemInventory(id: number): Promise<ApiResponse<ProductInventory>> {
  return fetchApi<ProductInventory>(`/api/admin/items/${id}/inventory`, {}, 'bo')
}

export async function updateAdminItemInventory(
  id: number,
  payload: UpdateProductInventoryRequest
): Promise<ApiResponse<ProductInventory>> {
  return fetchApi<ProductInventory>(`/api/admin/items/${id}/inventory`, {
    method: 'PUT',
    body: JSON.stringify(payload),
  }, 'bo')
}

export async function getAdminItemCategories(): Promise<ApiResponse<ProductCategory[]>> {
  return fetchApi<ProductCategory[]>(`/api/admin/item-categories`, {}, 'bo')
}

export async function createAdminItemCategory(
  payload: CreateProductCategoryRequest
): Promise<ApiResponse<ProductCategory>> {
  return fetchApi<ProductCategory>(`/api/admin/item-categories`, {
    method: 'POST',
    body: JSON.stringify(payload),
  }, 'bo')
}

export async function updateAdminItemCategory(
  id: number,
  payload: UpdateProductCategoryRequest
): Promise<ApiResponse<ProductCategory>> {
  return fetchApi<ProductCategory>(`/api/admin/item-categories/${id}`, {
    method: 'PUT',
    body: JSON.stringify(payload),
  }, 'bo')
}
