import type {
  ApiResponse,
  Product,
  ProductListResponse,
  Cart,
  Order,
  AddToCartRequest,
  UpdateQuantityRequest,
  UpdateProductRequest,
  CreateOrderRequest,
  User,
  AuthResponse,
  RegisterRequest,
  LoginRequest,
} from '../types/api'

const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080'

// セッションIDの生成・取得
const getSessionId = (): string => {
  let sessionId = localStorage.getItem('sessionId')
  if (!sessionId) {
    sessionId = `session-${Date.now()}-${Math.random().toString(36).substring(7)}`
    localStorage.setItem('sessionId', sessionId)
  }
  return sessionId
}

// 共通fetch関数
async function fetchApi<T>(
  endpoint: string,
  options: RequestInit = {}
): Promise<ApiResponse<T>> {
  const url = `${API_BASE_URL}${endpoint}`
  const headers = new Headers(options.headers)
  if (!headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json')
  }

  // 認証トークンの自動付与
  const token = localStorage.getItem('authToken')
  if (token && !endpoint.includes('/auth/login') && !endpoint.includes('/auth/register')) {
    headers.set('Authorization', `Bearer ${token}`)
  }

  // カート・注文関連のエンドポイントにはセッションIDを付与
  if (endpoint.includes('/order')) {
    headers.set('X-Session-Id', getSessionId())
  }

  try {
    const response = await fetch(url, {
      ...options,
      headers,
    })

    // 401エラーのハンドリング
    if (!response.ok && response.status === 401) {
      localStorage.removeItem('authToken')
      localStorage.removeItem('authUser')
      window.dispatchEvent(new Event('auth:unauthorized'))
    }

    const data = await response.json()
    return data
  } catch (error) {
    console.error('API Error:', error)
    return {
      success: false,
      error: {
        code: 'NETWORK_ERROR',
        message: 'ネットワークエラーが発生しました',
      },
    }
  }
}

// ============================================
// 商品 API
// ============================================

/**
 * 商品一覧取得
 */
export async function getItems(
  page = 1,
  limit = 20
): Promise<ApiResponse<ProductListResponse>> {
  return fetchApi<ProductListResponse>(`/api/item?page=${page}&limit=${limit}`)
}

/**
 * 商品詳細取得
 */
export async function getItemById(id: number): Promise<ApiResponse<Product>> {
  return fetchApi<Product>(`/api/item/${id}`)
}

/**
 * 商品更新（管理用）
 */
export async function updateItem(
  id: number,
  updates: UpdateProductRequest
): Promise<ApiResponse<Product>> {
  return fetchApi<Product>(`/api/item/${id}`, {
    method: 'PUT',
    body: JSON.stringify(updates),
  })
}

// ============================================
// カート API
// ============================================

/**
 * カート取得
 */
export async function getCart(): Promise<ApiResponse<Cart>> {
  return fetchApi<Cart>('/api/order/cart')
}

/**
 * カートに商品追加
 */
export async function addToCart(
  request: AddToCartRequest
): Promise<ApiResponse<Cart>> {
  return fetchApi<Cart>('/api/order/cart/items', {
    method: 'POST',
    body: JSON.stringify(request),
  })
}

/**
 * カート内商品の数量変更
 */
export async function updateCartItemQuantity(
  itemId: number,
  request: UpdateQuantityRequest
): Promise<ApiResponse<Cart>> {
  return fetchApi<Cart>(`/api/order/cart/items/${itemId}`, {
    method: 'PUT',
    body: JSON.stringify(request),
  })
}

/**
 * カートから商品削除
 */
export async function removeFromCart(itemId: number): Promise<ApiResponse<Cart>> {
  return fetchApi<Cart>(`/api/order/cart/items/${itemId}`, {
    method: 'DELETE',
  })
}

// ============================================
// 注文 API
// ============================================

/**
 * 注文作成
 */
export async function createOrder(
  request: CreateOrderRequest
): Promise<ApiResponse<Order>> {
  return fetchApi<Order>('/api/order', {
    method: 'POST',
    body: JSON.stringify(request),
  })
}

/**
 * 注文詳細取得
 */
export async function getOrderById(id: number): Promise<ApiResponse<Order>> {
  return fetchApi<Order>(`/api/order/${id}`)
}

/**
 * 会員の注文履歴を取得
 */
export async function getOrderHistory(): Promise<ApiResponse<Order[]>> {
  return fetchApi<Order[]>('/api/order/history')
}

/**
 * 注文キャンセル
 */
export async function cancelOrder(orderId: number): Promise<ApiResponse<Order>> {
  return fetchApi<Order>(`/api/order/${orderId}/cancel`, {
    method: 'POST',
  })
}

/**
 * 注文確認（管理者用）
 */
export async function confirmOrder(orderId: number): Promise<ApiResponse<Order>> {
  return fetchApi<Order>(`/api/order/${orderId}/confirm`, {
    method: 'POST',
  })
}

/**
 * 注文発送（管理者用）
 */
export async function shipOrder(orderId: number): Promise<ApiResponse<Order>> {
  return fetchApi<Order>(`/api/order/${orderId}/ship`, {
    method: 'POST',
  })
}

/**
 * 注文配達完了（管理者用）
 */
export async function deliverOrder(orderId: number): Promise<ApiResponse<Order>> {
  return fetchApi<Order>(`/api/order/${orderId}/deliver`, {
    method: 'POST',
  })
}

/**
 * 全注文取得（管理者用）
 */
export async function getAllOrders(): Promise<ApiResponse<Order[]>> {
  return fetchApi<Order[]>('/api/order')
}

// ============================================
// 認証 API
// ============================================

/**
 * 会員登録
 */
export async function register(
  request: RegisterRequest
): Promise<ApiResponse<AuthResponse>> {
  return fetchApi<AuthResponse>('/api/auth/register', {
    method: 'POST',
    body: JSON.stringify(request),
  })
}

/**
 * ログイン
 */
export async function login(
  request: LoginRequest
): Promise<ApiResponse<AuthResponse>> {
  return fetchApi<AuthResponse>('/api/auth/login', {
    method: 'POST',
    body: JSON.stringify(request),
  })
}

/**
 * ログアウト
 */
export async function logout(): Promise<ApiResponse<{ message: string }>> {
  return fetchApi<{ message: string }>('/api/auth/logout', {
    method: 'POST',
  })
}

/**
 * 会員情報取得（トークン検証）
 */
export async function getCurrentUser(): Promise<ApiResponse<User>> {
  return fetchApi<User>('/api/auth/me')
}

/**
 * セッションIDを取得（外部公開用）
 */
export { getSessionId }
