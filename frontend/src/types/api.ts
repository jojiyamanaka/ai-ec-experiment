// 共通型定義を再エクスポート
export type {
  ApiResponse,
  ProductDto,
  CartDto,
  CartItemDto,
  OrderDto,
  OrderItemDto,
  UserDto,
} from '@app/shared'

// フロントエンド固有の型定義はそのまま

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
  userId?: number
  userEmail?: string
  userDisplayName?: string
  items: OrderItem[]
  totalPrice: number
  status: string
  statusLabel?: string
  createdAt: string
  updatedAt?: string
}

// 在庫不足商品の詳細情報
export interface StockShortageDetail {
  productId: number
  productName: string
  requestedQuantity: number
  availableStock: number
}

export interface UnavailableProductDetail {
  productId: number
  productName: string
}

// APIエラー情報
export interface ApiError {
  code: string
  message: string
  details?: StockShortageDetail[] | UnavailableProductDetail[]
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

// ============================================
// 認証関連の型定義
// ============================================

// ユーザー情報
export interface User {
  id: number
  email: string
  displayName: string
  isActive?: boolean
  createdAt: string
  updatedAt?: string
}

// 認証レスポンス
export interface AuthResponse {
  user: User
  token: string
  expiresAt: string
}

// 会員登録リクエスト
export interface RegisterRequest {
  email: string
  displayName: string
  password: string
}

// ログインリクエスト
export interface LoginRequest {
  email: string
  password: string
}
