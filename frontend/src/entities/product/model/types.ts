export type AllocationType = 'REAL' | 'FRAME'

export interface Product {
  id: number
  productCode: string
  name: string
  categoryId: number | null
  categoryName: string
  price: number
  image: string
  description: string
  allocationType: AllocationType
  effectiveStock: number
  isPublished: boolean
  publishStartAt: string | number | null
  publishEndAt: string | number | null
  saleStartAt: string | number | null
  saleEndAt: string | number | null
}

export interface ProductListResponse {
  items: Product[]
  total: number
  page: number
  limit: number
}

export interface AdminProductSearchParams {
  keyword?: string
  categoryId?: number
  isPublished?: boolean
  inSalePeriod?: boolean
  allocationType?: AllocationType
  stockThreshold?: number
  zeroStockOnly?: boolean
  page?: number
  limit?: number
}

export interface CreateProductRequest {
  productCode: string
  name: string
  description?: string
  categoryId: number
  price: number
  allocationType?: AllocationType
  isPublished?: boolean
  publishStartAt?: string | null
  publishEndAt?: string | null
  saleStartAt?: string | null
  saleEndAt?: string | null
  image?: string
}

export interface UpdateProductRequest {
  name?: string
  description?: string
  categoryId?: number
  price?: number
  allocationType?: AllocationType
  isPublished?: boolean
  publishStartAt?: string | null
  publishEndAt?: string | null
  saleStartAt?: string | null
  saleEndAt?: string | null
  image?: string
}

export interface ProductCategory {
  id: number
  name: string
  displayOrder: number
  isPublished: boolean
}

export interface CreateProductCategoryRequest {
  name: string
  displayOrder?: number
  isPublished?: boolean
}

export interface UpdateProductCategoryRequest {
  name?: string
  displayOrder?: number
  isPublished?: boolean
}

export interface ProductInventory {
  productId: number
  allocationType: AllocationType
  locationStock: {
    locationId: number
    availableQty: number
    committedQty: number
    remainingQty: number
  }
  salesLimit: {
    frameLimitQty: number
    consumedQty: number
    remainingQty: number
  }
}

export interface UpdateProductInventoryRequest {
  allocationType: AllocationType
  locationStock: {
    availableQty: number
  }
  salesLimit: {
    frameLimitQty: number
  }
}
