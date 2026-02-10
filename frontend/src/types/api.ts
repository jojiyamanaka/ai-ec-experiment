// API レスポンスの型定義

export interface Product {
  id: number
  name: string
  price: number
  image: string
  description: string
  stock: number
  isPublished: boolean
}

export interface CartItem {
  id: number
  product: Product
  quantity: number
}

export interface Cart {
  items: CartItem[]
  totalQuantity: number
  totalPrice: number
}

export interface OrderItem {
  product: Product
  quantity: number
  subtotal: number
}

export interface Order {
  orderId: number
  orderNumber: string
  items: OrderItem[]
  totalPrice: number
  status: string
  createdAt: string
  updatedAt?: string
}

// API レスポンス共通型
export interface ApiResponse<T> {
  success: boolean
  data?: T
  error?: {
    code: string
    message: string
    details?: unknown
  }
}

// リクエスト型
export interface AddToCartRequest {
  productId: number
  quantity?: number
}

export interface UpdateQuantityRequest {
  quantity: number
}

export interface UpdateProductRequest {
  price?: number
  stock?: number
  isPublished?: boolean
}

export interface CreateOrderRequest {
  cartId: string
}

export interface ProductListResponse {
  items: Product[]
  total: number
  page: number
  limit: number
}
