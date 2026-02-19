export interface Product {
  id: number
  name: string
  price: number
  image: string
  description: string
  stock: number
  isPublished: boolean
}

export interface ProductListResponse {
  items: Product[]
  total: number
  page: number
  limit: number
}

export interface UpdateProductRequest {
  price?: number
  stock?: number
  isPublished?: boolean
}
