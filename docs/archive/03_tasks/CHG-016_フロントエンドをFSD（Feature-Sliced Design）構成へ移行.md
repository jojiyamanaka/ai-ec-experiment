# CHG-016: フロントエンドをFSD（Feature-Sliced Design）構成へ移行 - 実装タスク

要件: `docs/01_requirements/CHG-016_フロントエンドをFSD（Feature-Sliced Design）構成へ移行.md`
設計: `docs/02_designs/CHG-016_フロントエンドをFSD（Feature-Sliced Design）構成へ移行.md`
作成日: 2026-02-18

検証コマンド:
```bash
cd frontend
npm run build        # 型チェック + ビルド（エラーなし必須）
npm run lint         # ESLintルール違反チェック（エラーなし必須）
npm run dev:customer # 顧客画面起動確認
npm run dev:admin    # 管理画面起動確認
```

---

## タスク一覧

### フロントエンド（設定）

---

- [ ] **T-1**: ESLintプラグインのインストール

  パス: `frontend/`（パッケージルート）

  実行コマンド:
  ```bash
  cd frontend
  npm install -D eslint-plugin-boundaries eslint-plugin-import
  ```

---

- [ ] **T-2**: Vite パスエイリアス + TypeScript パス設定

  パス: `frontend/vite.config.ts`

  変更前（現在）:
  ```ts
  import { defineConfig, loadEnv } from 'vite'
  import react from '@vitejs/plugin-react'
  import tailwindcss from '@tailwindcss/vite'

  // https://vite.dev/config/
  export default defineConfig(({ mode }) => {
    const env = loadEnv(mode, process.cwd(), '')
    const appMode = env.VITE_APP_MODE || (mode === 'admin' ? 'admin' : 'customer')

    return {
      plugins: [react(), tailwindcss()],
      server: {
        host: '0.0.0.0',
        port: appMode === 'admin' ? 5174 : 5173,
        watch: {
          usePolling: true,
        },
      },
    }
  })
  ```

  変更後:
  ```ts
  import path from 'path'
  import { defineConfig, loadEnv } from 'vite'
  import react from '@vitejs/plugin-react'
  import tailwindcss from '@tailwindcss/vite'

  // https://vite.dev/config/
  export default defineConfig(({ mode }) => {
    const env = loadEnv(mode, process.cwd(), '')
    const appMode = env.VITE_APP_MODE || (mode === 'admin' ? 'admin' : 'customer')

    return {
      plugins: [react(), tailwindcss()],
      resolve: {
        alias: {
          '@app': path.resolve(__dirname, 'src/app'),
          '@pages': path.resolve(__dirname, 'src/pages'),
          '@widgets': path.resolve(__dirname, 'src/widgets'),
          '@features': path.resolve(__dirname, 'src/features'),
          '@entities': path.resolve(__dirname, 'src/entities'),
          '@shared': path.resolve(__dirname, 'src/shared'),
        },
      },
      server: {
        host: '0.0.0.0',
        port: appMode === 'admin' ? 5174 : 5173,
        watch: {
          usePolling: true,
        },
      },
    }
  })
  ```

  パス: `frontend/tsconfig.app.json`

  変更前（現在の `"include": ["src"]` の直前に挿入）:
  ```json
  {
    "compilerOptions": {
      "tsBuildInfoFile": "./node_modules/.tmp/tsconfig.app.tsbuildinfo",
      "target": "ES2022",
      "useDefineForClassFields": true,
      "lib": ["ES2022", "DOM", "DOM.Iterable"],
      "module": "ESNext",
      "types": ["vite/client"],
      "skipLibCheck": true,
      "moduleResolution": "bundler",
      "allowImportingTsExtensions": true,
      "verbatimModuleSyntax": true,
      "moduleDetection": "force",
      "noEmit": true,
      "jsx": "react-jsx",
      "strict": true,
      "noUnusedLocals": true,
      "noUnusedParameters": true,
      "erasableSyntaxOnly": true,
      "noFallthroughCasesInSwitch": true,
      "noUncheckedSideEffectImports": true
    },
    "include": ["src"]
  }
  ```

  変更後（`paths` を `compilerOptions` に追加）:
  ```json
  {
    "compilerOptions": {
      "tsBuildInfoFile": "./node_modules/.tmp/tsconfig.app.tsbuildinfo",
      "target": "ES2022",
      "useDefineForClassFields": true,
      "lib": ["ES2022", "DOM", "DOM.Iterable"],
      "module": "ESNext",
      "types": ["vite/client"],
      "skipLibCheck": true,
      "moduleResolution": "bundler",
      "allowImportingTsExtensions": true,
      "verbatimModuleSyntax": true,
      "moduleDetection": "force",
      "noEmit": true,
      "jsx": "react-jsx",
      "strict": true,
      "noUnusedLocals": true,
      "noUnusedParameters": true,
      "erasableSyntaxOnly": true,
      "noFallthroughCasesInSwitch": true,
      "noUncheckedSideEffectImports": true,
      "paths": {
        "@app/*": ["src/app/*"],
        "@pages/*": ["src/pages/*"],
        "@widgets/*": ["src/widgets/*"],
        "@features/*": ["src/features/*"],
        "@entities/*": ["src/entities/*"],
        "@shared/*": ["src/shared/*"]
      }
    },
    "include": ["src"]
  }
  ```

---

- [ ] **T-3**: ESLint FSDレイヤー依存ルール設定

  パス: `frontend/eslint.config.js`

  変更前（現在の全内容）:
  ```js
  import js from '@eslint/js'
  import globals from 'globals'
  import reactHooks from 'eslint-plugin-react-hooks'
  import reactRefresh from 'eslint-plugin-react-refresh'
  import tseslint from 'typescript-eslint'
  import { defineConfig, globalIgnores } from 'eslint/config'

  export default defineConfig([
    globalIgnores(['dist']),
    {
      files: ['**/*.{ts,tsx}'],
      extends: [
        js.configs.recommended,
        tseslint.configs.recommended,
        reactHooks.configs.flat.recommended,
        reactRefresh.configs.vite,
      ],
      languageOptions: {
        ecmaVersion: 2020,
        globals: globals.browser,
      },
    },
  ])
  ```

  変更後:
  ```js
  import js from '@eslint/js'
  import globals from 'globals'
  import reactHooks from 'eslint-plugin-react-hooks'
  import reactRefresh from 'eslint-plugin-react-refresh'
  import tseslint from 'typescript-eslint'
  import boundaries from 'eslint-plugin-boundaries'
  import importPlugin from 'eslint-plugin-import'
  import { defineConfig, globalIgnores } from 'eslint/config'

  const FSD_LAYERS = ['app', 'pages', 'widgets', 'features', 'entities', 'shared']

  export default defineConfig([
    globalIgnores(['dist']),
    {
      files: ['**/*.{ts,tsx}'],
      extends: [
        js.configs.recommended,
        tseslint.configs.recommended,
        reactHooks.configs.flat.recommended,
        reactRefresh.configs.vite,
      ],
      plugins: { boundaries, import: importPlugin },
      settings: {
        'boundaries/elements': FSD_LAYERS.map((layer) => ({
          type: layer,
          pattern: `src/${layer}/*`,
        })),
      },
      rules: {
        // ルール①: レイヤー間の依存方向（上位→下位のみ）
        'boundaries/element-types': [
          'error',
          {
            default: 'disallow',
            rules: [
              { from: 'app',      allow: ['pages', 'widgets', 'features', 'entities', 'shared'] },
              { from: 'pages',    allow: ['widgets', 'features', 'entities', 'shared'] },
              { from: 'widgets',  allow: ['features', 'entities', 'shared'] },
              { from: 'features', allow: ['entities', 'shared'] },
              { from: 'entities', allow: ['shared'] },
              { from: 'shared',   allow: [] },
            ],
          },
        ],
        // ルール②: スライス内部パスへの直接アクセス禁止（index.ts 経由を強制）
        'import/no-internal-modules': [
          'error',
          {
            allow: [
              '@app/**',
              '@pages/*/*',
              '@widgets/*',
              '@features/*',
              '@entities/*',
              '@shared/**',
            ],
          },
        ],
      },
      languageOptions: {
        ecmaVersion: 2020,
        globals: globals.browser,
      },
    },
  ])
  ```

---

### フロントエンド（shared レイヤー）

---

- [ ] **T-4**: `shared/types/api.ts` — 共通レスポンス型・エラー型を作成

  パス: `frontend/src/shared/types/api.ts`（新規作成）

  移行元: `src/types/api.ts` の `ApiError`, `StockShortageDetail`, `UnavailableProductDetail` と `ApiResponse`

  ```ts
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

  // APIレスポンス共通型
  export interface ApiResponse<T = unknown> {
    success: boolean
    data?: T
    error?: ApiError
  }
  ```

---

- [ ] **T-5**: `shared/config/env.ts` — アプリ設定定数を作成

  パス: `frontend/src/shared/config/env.ts`（新規作成）

  移行元: `src/lib/api.ts` 冒頭の定数部分（1〜2行目）

  ```ts
  export const APP_MODE = import.meta.env.VITE_APP_MODE === 'admin' ? 'admin' : 'customer'
  export const API_BASE_URL =
    import.meta.env.VITE_API_URL ||
    (APP_MODE === 'admin' ? 'http://localhost:3002' : 'http://localhost:3001')
  ```

---

- [ ] **T-6**: `shared/api/client.ts` — 純粋HTTPクライアントを作成

  パス: `frontend/src/shared/api/client.ts`（新規作成）

  移行元: `src/lib/api.ts` の `fetchApi`, `get`, `post`, `put`, `getSessionId`（エンドポイント正規化ロジック含む）

  ```ts
  import type { ApiResponse } from '@shared/types/api'
  import { APP_MODE, API_BASE_URL } from '@shared/config/env'

  export type AuthContext = 'auto' | 'customer' | 'bo'

  // ⚠ 技術的負債: bo_token/authToken の localStorage 参照がここに残る
  // → Phase 2 で token getter コールバックを注入する形に分離予定
  export const getSessionId = (): string => {
    let sessionId = localStorage.getItem('sessionId')
    if (!sessionId) {
      sessionId = `session-${Date.now()}-${Math.random().toString(36).substring(7)}`
      localStorage.setItem('sessionId', sessionId)
    }
    return sessionId
  }

  export async function fetchApi<T>(
    endpoint: string,
    options: RequestInit = {},
    authContext: AuthContext = 'auto'
  ): Promise<ApiResponse<T>> {
    const url = `${API_BASE_URL}${endpoint}`
    const headers = new Headers(options.headers)
    if (!headers.has('Content-Type')) {
      headers.set('Content-Type', 'application/json')
    }

    const boToken = localStorage.getItem('bo_token')
    const isBoEndpoint = endpoint.startsWith('/api/bo') || endpoint.startsWith('/api/bo-auth')
    const shouldUseBoAuthContext =
      authContext === 'bo' || (authContext === 'auto' && APP_MODE === 'admin')

    if (shouldUseBoAuthContext) {
      if (boToken && !endpoint.endsWith('/bo-auth/login')) {
        headers.set('Authorization', `Bearer ${boToken}`)
      }
    } else if (!isBoEndpoint) {
      const token = localStorage.getItem('authToken')
      if (token && !endpoint.includes('/auth/login') && !endpoint.includes('/auth/register')) {
        headers.set('Authorization', `Bearer ${token}`)
      }
    }

    if (endpoint.includes('/order')) {
      headers.set('X-Session-Id', getSessionId())
    }

    try {
      const response = await fetch(url, { ...options, headers })

      if (!response.ok && response.status === 401) {
        if (shouldUseBoAuthContext || isBoEndpoint) {
          localStorage.removeItem('bo_token')
          window.dispatchEvent(new Event('bo-auth:unauthorized'))
        } else {
          localStorage.removeItem('authToken')
          localStorage.removeItem('authUser')
          window.dispatchEvent(new Event('auth:unauthorized'))
        }
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

  function normalizeEndpoint(endpoint: string): string {
    return endpoint.startsWith('/api') ? endpoint : `/api${endpoint}`
  }

  export async function get<T>(
    endpoint: string,
    authContext: AuthContext = 'auto'
  ): Promise<ApiResponse<T>> {
    return fetchApi<T>(normalizeEndpoint(endpoint), {}, authContext)
  }

  export async function post<T>(
    endpoint: string,
    body?: unknown,
    authContext: AuthContext = 'auto'
  ): Promise<ApiResponse<T>> {
    return fetchApi<T>(
      normalizeEndpoint(endpoint),
      { method: 'POST', body: body === undefined ? undefined : JSON.stringify(body) },
      authContext
    )
  }

  export async function put<T>(
    endpoint: string,
    body?: unknown,
    authContext: AuthContext = 'auto'
  ): Promise<ApiResponse<T>> {
    return fetchApi<T>(
      normalizeEndpoint(endpoint),
      { method: 'PUT', body: body === undefined ? undefined : JSON.stringify(body) },
      authContext
    )
  }
  ```

---

- [ ] **T-7**: `shared/ui/Pagination.tsx` + `shared/lib/errorMessages.ts` を作成

  **① パス: `frontend/src/shared/ui/Pagination.tsx`（新規作成）**

  移行元: `src/components/Pagination.tsx`（内容そのまま、import なし）

  `src/components/Pagination.tsx` の全内容をコピーし、ファイルを新規作成する。

  **② パス: `frontend/src/shared/lib/errorMessages.ts`（新規作成）**

  移行元: `src/lib/errorMessages.ts`（内容そのまま）

  `src/lib/errorMessages.ts` の全内容をコピーし、ファイルを新規作成する。

---

### フロントエンド（entities レイヤー）

---

- [ ] **T-8**: `entities/product/` — 商品エンティティを作成

  **① パス: `frontend/src/entities/product/model/types.ts`（新規作成）**

  移行元: `src/types/api.ts` の `Product`, `ProductListResponse`, `UpdateProductRequest`

  ```ts
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
  ```

  **② パス: `frontend/src/entities/product/model/api.ts`（新規作成）**

  移行元: `src/lib/api.ts` の `getItems`, `getItemById`, `updateItem`

  ```ts
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
  ```

  **③ パス: `frontend/src/entities/product/model/ProductContext.tsx`（新規作成）**

  移行元: `src/contexts/ProductContext.tsx`（import パスを更新）

  変更点:
  - `import type { Product, UpdateProductRequest } from '../types/api'` → `import type { Product, UpdateProductRequest } from './types'`
  - `import * as api from '../lib/api'` → `import { getItems, updateItem } from './api'`
  - `import { getUserFriendlyMessage } from '../lib/errorMessages'` → `import { getUserFriendlyMessage } from '@shared/lib/errorMessages'`
  - `await api.getItems()` → `await getItems()`
  - `await api.updateItem(id, updates)` → `await updateItem(id, updates)`

  ```tsx
  import { createContext, useContext, useState, useEffect } from 'react'
  import type { ReactNode } from 'react'
  import type { Product, UpdateProductRequest } from './types'
  import { getItems, updateItem } from './api'
  import { getUserFriendlyMessage } from '@shared/lib/errorMessages'

  interface ProductContextType {
    products: Product[]
    loading: boolean
    error: string | null
    refreshProducts: () => Promise<void>
    getPublishedProducts: () => Product[]
    updateProduct: (id: number, updates: UpdateProductRequest) => Promise<void>
  }

  const ProductContext = createContext<ProductContextType | undefined>(undefined)

  export function ProductProvider({ children }: { children: ReactNode }) {
    const [products, setProducts] = useState<Product[]>([])
    const [loading, setLoading] = useState(true)
    const [error, setError] = useState<string | null>(null)

    const refreshProducts = async () => {
      setLoading(true)
      setError(null)
      try {
        const response = await getItems()
        if (response.success && response.data) {
          setProducts(response.data.items)
        } else {
          const message = response.error?.code
            ? getUserFriendlyMessage(response.error.code)
            : '商品の取得に失敗しました'
          setError(message)
        }
      } catch (err) {
        setError('商品の取得中にエラーが発生しました')
        console.error(err)
      } finally {
        setLoading(false)
      }
    }

    useEffect(() => {
      refreshProducts()
    }, [])

    const getPublishedProducts = () => products.filter((product) => product.isPublished)

    const updateProduct = async (id: number, updates: UpdateProductRequest) => {
      try {
        const response = await updateItem(id, updates)
        if (response.success && response.data) {
          setProducts((prevProducts) =>
            prevProducts.map((product) => (product.id === id ? response.data! : product))
          )
        } else {
          const message = response.error?.code
            ? getUserFriendlyMessage(response.error.code)
            : '商品の更新に失敗しました'
          throw new Error(message)
        }
      } catch (err) {
        console.error('商品更新エラー:', err)
        throw err
      }
    }

    return (
      <ProductContext.Provider
        value={{ products, loading, error, refreshProducts, getPublishedProducts, updateProduct }}
      >
        {children}
      </ProductContext.Provider>
    )
  }

  // eslint-disable-next-line react-refresh/only-export-components
  export function useProducts() {
    const context = useContext(ProductContext)
    if (context === undefined) {
      throw new Error('useProducts must be used within a ProductProvider')
    }
    return context
  }
  ```

  **④ パス: `frontend/src/entities/product/ui/ProductCard.tsx`（新規作成）**

  移行元: `src/components/ProductCard.tsx`（import パスを更新）

  変更点:
  - `import type { Product } from '../types/api'` → `import type { Product } from '../model/types'`

  それ以外は `src/components/ProductCard.tsx` の全内容をコピー。

  **⑤ パス: `frontend/src/entities/product/index.ts`（新規作成）**

  ```ts
  export type { Product, ProductListResponse, UpdateProductRequest } from './model/types'
  export { useProducts, ProductProvider } from './model/ProductContext'
  export { getItems, getItemById, updateItem } from './model/api'
  export { default as ProductCard } from './ui/ProductCard'
  ```

---

- [ ] **T-9**: `entities/cart/` — カートエンティティを作成

  **① パス: `frontend/src/entities/cart/model/types.ts`（新規作成）**

  移行元: `src/types/api.ts` の `CartItem`, `Cart`, `AddToCartRequest`, `UpdateQuantityRequest`

  ```ts
  import type { Product } from '@entities/product'

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

  export interface AddToCartRequest {
    productId: number
    quantity?: number
  }

  export interface UpdateQuantityRequest {
    quantity: number
  }
  ```

  **② パス: `frontend/src/entities/cart/model/api.ts`（新規作成）**

  移行元: `src/lib/api.ts` の `getCart`, `addToCart`, `updateCartItemQuantity`, `removeFromCart`

  ```ts
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
  ```

  **③ パス: `frontend/src/entities/cart/index.ts`（新規作成）**

  ```ts
  export type { Cart, CartItem, AddToCartRequest, UpdateQuantityRequest } from './model/types'
  export { getCart, addToCart, updateCartItemQuantity, removeFromCart } from './model/api'
  ```

---

- [ ] **T-10**: `entities/order/` — 注文エンティティを作成

  **① パス: `frontend/src/entities/order/model/types.ts`（新規作成）**

  移行元: `src/types/api.ts` の `OrderItem`, `Order`, `CreateOrderRequest`

  ```ts
  import type { Product } from '@entities/product'

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

  export interface CreateOrderRequest {
    cartId: string
  }
  ```

  **② パス: `frontend/src/entities/order/model/api.ts`（新規作成）**

  移行元: `src/lib/api.ts` の注文関連関数

  ```ts
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
  ```

  **③ パス: `frontend/src/entities/order/index.ts`（新規作成）**

  ```ts
  export type { Order, OrderItem, CreateOrderRequest } from './model/types'
  export {
    createOrder,
    getOrderById,
    getOrderHistory,
    cancelOrder,
    confirmOrder,
    shipOrder,
    deliverOrder,
    getAllOrders,
  } from './model/api'
  ```

---

- [ ] **T-11**: `entities/customer/` + `entities/bo-user/` — 認証エンティティを作成

  **① パス: `frontend/src/entities/customer/model/types.ts`（新規作成）**

  移行元: `src/types/api.ts` の `User`, `AuthResponse`, `RegisterRequest`, `LoginRequest`

  ```ts
  export interface User {
    id: number
    email: string
    displayName: string
    isActive?: boolean
    createdAt: string
    updatedAt?: string
  }

  export interface AuthResponse {
    user: User
    token: string
    expiresAt: string
  }

  export interface RegisterRequest {
    email: string
    displayName: string
    password: string
  }

  export interface LoginRequest {
    email: string
    password: string
  }
  ```

  **② パス: `frontend/src/entities/customer/model/api.ts`（新規作成）**

  移行元: `src/lib/api.ts` の `register`, `login`, `logout`, `getCurrentUser`

  ```ts
  import { fetchApi } from '@shared/api/client'
  import type { ApiResponse } from '@shared/types/api'
  import type { User, AuthResponse, RegisterRequest, LoginRequest } from './types'

  export async function register(request: RegisterRequest): Promise<ApiResponse<AuthResponse>> {
    return fetchApi<AuthResponse>('/api/auth/register', {
      method: 'POST',
      body: JSON.stringify(request),
    })
  }

  export async function login(request: LoginRequest): Promise<ApiResponse<AuthResponse>> {
    return fetchApi<AuthResponse>('/api/auth/login', {
      method: 'POST',
      body: JSON.stringify(request),
    })
  }

  export async function logout(): Promise<ApiResponse<{ message: string }>> {
    return fetchApi<{ message: string }>('/api/auth/logout', { method: 'POST' })
  }

  export async function getCurrentUser(): Promise<ApiResponse<User>> {
    return fetchApi<User>('/api/auth/me')
  }
  ```

  **③ パス: `frontend/src/entities/customer/index.ts`（新規作成）**

  ```ts
  export type { User, AuthResponse, RegisterRequest, LoginRequest } from './model/types'
  export { register, login, logout, getCurrentUser } from './model/api'
  ```

  **④ パス: `frontend/src/entities/bo-user/model/types.ts`（新規作成）**

  移行元: `src/lib/api.ts` の `BoUser`, `BoAuthResponse`, `BoLoginRequest`（interface として定義済み）

  ```ts
  export interface BoUser {
    id: number
    email: string
    displayName: string
    permissionLevel: 'SUPER_ADMIN' | 'ADMIN' | 'OPERATOR'
    lastLoginAt?: string
    isActive: boolean
    createdAt: string
    updatedAt: string
  }

  export interface BoAuthResponse {
    user: BoUser
    token: string
    expiresAt: string
  }

  export interface BoLoginRequest {
    email: string
    password: string
  }
  ```

  **⑤ パス: `frontend/src/entities/bo-user/model/api.ts`（新規作成）**

  移行元: `src/lib/api.ts` の `boLogin`, `boLogout`, `getBoUser`

  ```ts
  import { post, get } from '@shared/api/client'
  import type { ApiResponse } from '@shared/types/api'
  import type { BoUser, BoAuthResponse } from './types'

  export async function boLogin(
    email: string,
    password: string
  ): Promise<ApiResponse<BoAuthResponse>> {
    return post<BoAuthResponse>('/bo-auth/login', { email, password })
  }

  export async function boLogout(): Promise<ApiResponse<{ message: string }>> {
    return post<{ message: string }>('/bo-auth/logout', {})
  }

  export async function getBoUser(): Promise<ApiResponse<BoUser>> {
    return get<BoUser>('/bo-auth/me')
  }
  ```

  **⑥ パス: `frontend/src/entities/bo-user/index.ts`（新規作成）**

  ```ts
  export type { BoUser, BoAuthResponse, BoLoginRequest } from './model/types'
  export { boLogin, boLogout, getBoUser } from './model/api'
  ```

---

### フロントエンド（features レイヤー）

---

- [ ] **T-12**: `features/auth/` — 顧客認証 Context を移動

  **① パス: `frontend/src/features/auth/model/AuthContext.tsx`（新規作成）**

  移行元: `src/contexts/AuthContext.tsx`（import パスを更新）

  変更点:
  - `import type { User } from '../types/api'` → `import type { User } from '@entities/customer'`
  - `import * as api from '../lib/api'` → `import { getCurrentUser, register, login, logout } from '@entities/customer'`
  - `await api.getCurrentUser()` → `await getCurrentUser()`
  - `await api.register({...})` → `await register({...})`
  - `await api.login({...})` → `await login({...})`
  - `await api.logout()` → `await logout()`

  ```tsx
  import { createContext, useContext, useState, useEffect } from 'react'
  import type { ReactNode } from 'react'
  import type { User } from '@entities/customer'
  import { getCurrentUser, register as apiRegister, login as apiLogin, logout as apiLogout } from '@entities/customer'

  interface AuthContextType {
    user: User | null
    token: string | null
    isAuthenticated: boolean
    isAdmin: boolean
    loading: boolean
    error: string | null
    register: (email: string, displayName: string, password: string) => Promise<void>
    login: (email: string, password: string) => Promise<void>
    logout: () => Promise<void>
    refreshUser: () => Promise<void>
    clearError: () => void
  }

  const AuthContext = createContext<AuthContextType | undefined>(undefined)

  export function AuthProvider({ children }: { children: ReactNode }) {
    const [user, setUser] = useState<User | null>(null)
    const [token, setToken] = useState<string | null>(null)
    const [loading, setLoading] = useState(true)
    const [error, setError] = useState<string | null>(null)

    useEffect(() => {
      refreshUser()
    }, [])

    useEffect(() => {
      const handleUnauthorized = () => {
        setUser(null)
        setToken(null)
      }
      window.addEventListener('auth:unauthorized', handleUnauthorized)
      return () => window.removeEventListener('auth:unauthorized', handleUnauthorized)
    }, [])

    const refreshUser = async () => {
      const savedToken = localStorage.getItem('authToken')
      if (!savedToken) {
        setLoading(false)
        return
      }
      try {
        const response = await getCurrentUser()
        if (response.success && response.data) {
          setUser(response.data)
          setToken(savedToken)
        } else {
          localStorage.removeItem('authToken')
          localStorage.removeItem('authUser')
        }
      } catch (err) {
        console.error('トークン検証エラー:', err)
        localStorage.removeItem('authToken')
        localStorage.removeItem('authUser')
      } finally {
        setLoading(false)
      }
    }

    const register = async (email: string, displayName: string, password: string) => {
      setLoading(true)
      setError(null)
      try {
        const response = await apiRegister({ email, displayName, password })
        if (response.success && response.data) {
          const { user, token } = response.data
          setUser(user)
          setToken(token)
          localStorage.setItem('authToken', token)
          localStorage.setItem('authUser', JSON.stringify(user))
        } else {
          throw new Error(response.error?.message || '登録に失敗しました')
        }
      } catch (err) {
        const message = err instanceof Error ? err.message : '登録エラー'
        setError(message)
        throw err
      } finally {
        setLoading(false)
      }
    }

    const login = async (email: string, password: string) => {
      setLoading(true)
      setError(null)
      try {
        const response = await apiLogin({ email, password })
        if (response.success && response.data) {
          const { user, token } = response.data
          setUser(user)
          setToken(token)
          localStorage.setItem('authToken', token)
          localStorage.setItem('authUser', JSON.stringify(user))
        } else {
          throw new Error(response.error?.message || 'ログインに失敗しました')
        }
      } catch (err) {
        const message = err instanceof Error ? err.message : 'ログインエラー'
        setError(message)
        throw err
      } finally {
        setLoading(false)
      }
    }

    const logout = async () => {
      setLoading(true)
      try {
        await apiLogout()
      } catch (err) {
        console.error('ログアウトエラー:', err)
      } finally {
        setUser(null)
        setToken(null)
        localStorage.removeItem('authToken')
        localStorage.removeItem('authUser')
        setLoading(false)
      }
    }

    const clearError = () => setError(null)
    const isAuthenticated = !!user && !!token
    const isAdmin = false

    return (
      <AuthContext.Provider
        value={{ user, token, isAuthenticated, isAdmin, loading, error, register, login, logout, refreshUser, clearError }}
      >
        {children}
      </AuthContext.Provider>
    )
  }

  // eslint-disable-next-line react-refresh/only-export-components
  export function useAuth() {
    const context = useContext(AuthContext)
    if (context === undefined) {
      throw new Error('useAuth must be used within an AuthProvider')
    }
    return context
  }
  ```

  **② パス: `frontend/src/features/auth/index.ts`（新規作成）**

  ```ts
  export { useAuth, AuthProvider } from './model/AuthContext'
  ```

---

- [ ] **T-13**: `features/bo-auth/` + `features/cart/` — BoAuth・Cart Context を移動

  **① パス: `frontend/src/features/bo-auth/model/BoAuthContext.tsx`（新規作成）**

  移行元: `src/contexts/BoAuthContext.tsx`（import パスを更新）

  変更点:
  - `import * as api from '../lib/api'` → `import { boLogin as apiBoLogin, boLogout as apiBoLogout, getBoUser } from '@entities/bo-user'`
  - `import type { BoUser } from ... (api.BoUser)` → `import type { BoUser } from '@entities/bo-user'`
  - `api.getBoUser()` → `getBoUser()`
  - `api.boLogin(email, password)` → `apiBoLogin(email, password)`
  - `api.boLogout()` → `apiBoLogout()`

  ```tsx
  import { createContext, useContext, useState, useEffect } from 'react'
  import type { ReactNode } from 'react'
  import type { BoUser } from '@entities/bo-user'
  import { boLogin as apiBoLogin, boLogout as apiBoLogout, getBoUser } from '@entities/bo-user'

  interface BoAuthContextType {
    boUser: BoUser | null
    loading: boolean
    error: string | null
    boLogin: (email: string, password: string) => Promise<void>
    boLogout: () => Promise<void>
    clearError: () => void
  }

  const BoAuthContext = createContext<BoAuthContextType | undefined>(undefined)

  export function BoAuthProvider({ children }: { children: ReactNode }) {
    const [boUser, setBoUser] = useState<BoUser | null>(null)
    const [loading, setLoading] = useState(false)
    const [error, setError] = useState<string | null>(null)

    useEffect(() => {
      const token = localStorage.getItem('bo_token')
      if (token) {
        getBoUser().then((response) => {
          if (response.success && response.data) {
            setBoUser(response.data)
          } else {
            localStorage.removeItem('bo_token')
          }
        })
      }
    }, [])

    useEffect(() => {
      const handleUnauthorized = () => {
        setBoUser(null)
        setError(null)
      }
      window.addEventListener('bo-auth:unauthorized', handleUnauthorized)
      return () => window.removeEventListener('bo-auth:unauthorized', handleUnauthorized)
    }, [])

    const boLogin = async (email: string, password: string) => {
      setLoading(true)
      setError(null)
      try {
        const response = await apiBoLogin(email, password)
        if (response.success && response.data) {
          localStorage.setItem('bo_token', response.data.token)
          setBoUser(response.data.user)
        } else {
          const message = response.error?.message || 'ログインに失敗しました'
          setError(message)
          throw new Error(message)
        }
      } catch (err) {
        const message = err instanceof Error ? err.message : 'ログインに失敗しました'
        setError(message)
        throw err
      } finally {
        setLoading(false)
      }
    }

    const boLogout = async () => {
      try {
        await apiBoLogout()
      } finally {
        localStorage.removeItem('bo_token')
        setBoUser(null)
      }
    }

    const clearError = () => setError(null)

    return (
      <BoAuthContext.Provider value={{ boUser, loading, error, boLogin, boLogout, clearError }}>
        {children}
      </BoAuthContext.Provider>
    )
  }

  export function useBoAuth() {
    const context = useContext(BoAuthContext)
    if (!context) {
      throw new Error('useBoAuth must be used within BoAuthProvider')
    }
    return context
  }
  ```

  **② パス: `frontend/src/features/bo-auth/index.ts`（新規作成）**

  ```ts
  export { useBoAuth, BoAuthProvider } from './model/BoAuthContext'
  ```

  **③ パス: `frontend/src/features/cart/model/CartContext.tsx`（新規作成）**

  移行元: `src/contexts/CartContext.tsx`（import パスを更新）

  変更点:
  - `import type { Product, CartItem as ApiCartItem } from '../types/api'` → `import type { Product } from '@entities/product'`, `import type { CartItem } from '@entities/cart'`
  - `import * as api from '../lib/api'` → `import { getCart, addToCart, updateCartItemQuantity, removeFromCart } from '@entities/cart'`
  - `import { getUserFriendlyMessage } from '../lib/errorMessages'` → `import { getUserFriendlyMessage } from '@shared/lib/errorMessages'`
  - `export type CartItem = ApiCartItem` を削除（`CartItem` は `@entities/cart` から直接 import する）
  - `await api.getCart()` → `await getCart()` （同様に他の api 呼び出しも）

  ```tsx
  import { createContext, useContext, useState, useEffect } from 'react'
  import type { ReactNode } from 'react'
  import type { Product } from '@entities/product'
  import type { Cart, CartItem, AddToCartRequest, UpdateQuantityRequest } from '@entities/cart'
  import { getCart, addToCart, updateCartItemQuantity, removeFromCart } from '@entities/cart'
  import { getUserFriendlyMessage } from '@shared/lib/errorMessages'

  interface CartContextType {
    items: CartItem[]
    totalQuantity: number
    totalPrice: number
    loading: boolean
    error: string | null
    addToCart: (product: Product, quantity?: number) => Promise<void>
    removeFromCart: (itemId: number) => Promise<void>
    updateQuantity: (itemId: number, quantity: number) => Promise<void>
    refreshCart: () => Promise<void>
    clearCart: () => void
  }

  const CartContext = createContext<CartContextType | undefined>(undefined)

  export function CartProvider({ children }: { children: ReactNode }) {
    const [items, setItems] = useState<CartItem[]>([])
    const [totalQuantity, setTotalQuantity] = useState(0)
    const [totalPrice, setTotalPrice] = useState(0)
    const [loading, setLoading] = useState(false)
    const [error, setError] = useState<string | null>(null)

    const refreshCart = async () => {
      setLoading(true)
      setError(null)
      try {
        const response = await getCart()
        if (response.success && response.data) {
          setItems(response.data.items)
          setTotalQuantity(response.data.totalQuantity)
          setTotalPrice(response.data.totalPrice)
        } else {
          const message = response.error?.code
            ? getUserFriendlyMessage(response.error.code)
            : 'カートの取得に失敗しました'
          setError(message)
        }
      } catch (err) {
        setError('カートの取得中にエラーが発生しました')
        console.error(err)
      } finally {
        setLoading(false)
      }
    }

    useEffect(() => {
      refreshCart()
    }, [])

    const addToCartHandler = async (product: Product, quantity = 1) => {
      setLoading(true)
      setError(null)
      try {
        const request: AddToCartRequest = { productId: product.id, quantity }
        const response = await addToCart(request)
        if (response.success && response.data) {
          setItems(response.data.items)
          setTotalQuantity(response.data.totalQuantity)
          setTotalPrice(response.data.totalPrice)
        } else {
          const message = response.error?.code
            ? getUserFriendlyMessage(response.error.code)
            : 'カートへの追加に失敗しました'
          throw new Error(message)
        }
      } catch (err) {
        setError(err instanceof Error ? err.message : 'エラーが発生しました')
        console.error('カート追加エラー:', err)
        throw err
      } finally {
        setLoading(false)
      }
    }

    const removeFromCartHandler = async (itemId: number) => {
      setLoading(true)
      setError(null)
      try {
        const response = await removeFromCart(itemId)
        if (response.success && response.data) {
          setItems(response.data.items)
          setTotalQuantity(response.data.totalQuantity)
          setTotalPrice(response.data.totalPrice)
        } else {
          const message = response.error?.code
            ? getUserFriendlyMessage(response.error.code)
            : 'カートからの削除に失敗しました'
          throw new Error(message)
        }
      } catch (err) {
        setError(err instanceof Error ? err.message : 'エラーが発生しました')
        console.error('カート削除エラー:', err)
        throw err
      } finally {
        setLoading(false)
      }
    }

    const updateQuantity = async (itemId: number, quantity: number) => {
      if (quantity <= 0) {
        await removeFromCartHandler(itemId)
        return
      }
      if (quantity > 9) return

      setLoading(true)
      setError(null)
      try {
        const request: UpdateQuantityRequest = { quantity }
        const response = await updateCartItemQuantity(itemId, request)
        if (response.success && response.data) {
          setItems(response.data.items)
          setTotalQuantity(response.data.totalQuantity)
          setTotalPrice(response.data.totalPrice)
        } else {
          const message = response.error?.code
            ? getUserFriendlyMessage(response.error.code)
            : '数量の変更に失敗しました'
          throw new Error(message)
        }
      } catch (err) {
        setError(err instanceof Error ? err.message : 'エラーが発生しました')
        console.error('数量更新エラー:', err)
        throw err
      } finally {
        setLoading(false)
      }
    }

    const clearCart = () => {
      setItems([])
      setTotalQuantity(0)
      setTotalPrice(0)
    }

    return (
      <CartContext.Provider
        value={{
          items,
          totalQuantity,
          totalPrice,
          loading,
          error,
          addToCart: addToCartHandler,
          removeFromCart: removeFromCartHandler,
          updateQuantity,
          refreshCart,
          clearCart,
        }}
      >
        {children}
      </CartContext.Provider>
    )
  }

  // eslint-disable-next-line react-refresh/only-export-components
  export function useCart() {
    const context = useContext(CartContext)
    if (context === undefined) {
      throw new Error('useCart must be used within a CartProvider')
    }
    return context
  }
  ```

  **④ パス: `frontend/src/features/cart/index.ts`（新規作成）**

  ```ts
  export { useCart, CartProvider } from './model/CartContext'
  ```

---

### フロントエンド（widgets レイヤー）

---

- [ ] **T-14**: `widgets/` — Layout・Sidebar コンポーネントを移動

  **① パス: `frontend/src/widgets/CustomerLayout/CustomerLayout.tsx`（新規作成）**

  移行元: `src/components/Layout.tsx`（import パスを更新）

  変更点:
  - `import { useCart } from '../contexts/CartContext'` → `import { useCart } from '@features/cart'`
  - `import { useAuth } from '../contexts/AuthContext'` → `import { useAuth } from '@features/auth'`

  それ以外は `src/components/Layout.tsx` の全内容をコピー（関数名は `Layout` のままでよい）。

  **② パス: `frontend/src/widgets/CustomerLayout/index.ts`（新規作成）**

  ```ts
  export { default as CustomerLayout } from './CustomerLayout'
  ```

  **③ パス: `frontend/src/widgets/AdminLayout/AdminLayout.tsx`（新規作成）**

  移行元: `src/components/AdminLayout.tsx`（import パスを更新）

  変更点:
  - `import AdminSidebar from './AdminSidebar'` → `import { AdminSidebar } from '@widgets/AdminSidebar'`

  それ以外は `src/components/AdminLayout.tsx` の全内容をコピー。

  **④ パス: `frontend/src/widgets/AdminLayout/index.ts`（新規作成）**

  ```ts
  export { default as AdminLayout } from './AdminLayout'
  ```

  **⑤ パス: `frontend/src/widgets/AdminSidebar/AdminSidebar.tsx`（新規作成）**

  移行元: `src/components/AdminSidebar.tsx`（import なし）

  `src/components/AdminSidebar.tsx` の全内容をコピー。

  **⑥ パス: `frontend/src/widgets/AdminSidebar/index.ts`（新規作成）**

  ```ts
  export { default as AdminSidebar } from './AdminSidebar'
  ```

---

### フロントエンド（pages レイヤー）

---

- [ ] **T-15**: `pages/customer/` — 顧客向けページを移動（10ファイル）

  各ページを新パスに作成し、相対 import を `@` エイリアスに書き換える。

  **移行対象と import 変更一覧:**

  | 旧パス | 新パス |
  |--------|--------|
  | `src/pages/HomePage.tsx` | `src/pages/customer/HomePage/index.tsx` |
  | `src/pages/ItemListPage.tsx` | `src/pages/customer/ItemListPage/index.tsx` |
  | `src/pages/ItemDetailPage.tsx` | `src/pages/customer/ItemDetailPage/index.tsx` |
  | `src/pages/CartPage.tsx` | `src/pages/customer/CartPage/index.tsx` |
  | `src/pages/OrderConfirmPage.tsx` | `src/pages/customer/OrderConfirmPage/index.tsx` |
  | `src/pages/OrderCompletePage.tsx` | `src/pages/customer/OrderCompletePage/index.tsx` |
  | `src/pages/OrderDetailPage.tsx` | `src/pages/customer/OrderDetailPage/index.tsx` |
  | `src/pages/OrderHistoryPage.tsx` | `src/pages/customer/OrderHistoryPage/index.tsx` |
  | `src/pages/LoginPage.tsx` | `src/pages/customer/LoginPage/index.tsx` |
  | `src/pages/RegisterPage.tsx` | `src/pages/customer/RegisterPage/index.tsx` |

  **ページごとの import 書き換え:**

  **`HomePage/index.tsx`**:
  ```ts
  // 変更前
  import ProductCard from '../components/ProductCard'
  import { useProducts } from '../contexts/ProductContext'
  // 変更後
  import { ProductCard, useProducts } from '@entities/product'
  ```

  **`ItemListPage/index.tsx`**:
  ```ts
  // 変更前
  import ProductCard from '../components/ProductCard'
  import Pagination from '../components/Pagination'
  import * as api from '../lib/api'
  import type { Product } from '../types/api'
  // 変更後
  import { ProductCard, getItems } from '@entities/product'
  import type { Product } from '@entities/product'
  import Pagination from '@shared/ui/Pagination'
  // api.getItems(page, ITEMS_PER_PAGE) → getItems(page, ITEMS_PER_PAGE) に変更
  ```

  **`ItemDetailPage/index.tsx`**:
  ```ts
  // 変更前
  import { useCart } from '../contexts/CartContext'
  import { useProducts } from '../contexts/ProductContext'
  // 変更後
  import { useCart } from '@features/cart'
  import { useProducts } from '@entities/product'
  ```

  **`CartPage/index.tsx`**:
  ```ts
  // 変更前
  import { useCart } from '../contexts/CartContext'
  // 変更後
  import { useCart } from '@features/cart'
  ```

  **`OrderConfirmPage/index.tsx`**:
  ```ts
  // 変更前
  import { useCart } from '../contexts/CartContext'
  import * as api from '../lib/api'
  import type { ApiError, StockShortageDetail, UnavailableProductDetail } from '../types/api'
  import { getUserFriendlyMessage } from '../lib/errorMessages'
  // 変更後
  import { useCart } from '@features/cart'
  import { createOrder } from '@entities/order'
  import { getSessionId } from '@shared/api/client'
  import type { ApiError, StockShortageDetail, UnavailableProductDetail } from '@shared/types/api'
  import { getUserFriendlyMessage } from '@shared/lib/errorMessages'
  // api.createOrder({cartId: api.getSessionId()}) →
  // createOrder({ cartId: getSessionId() }) に変更
  ```

  **`OrderCompletePage/index.tsx`**:
  ```ts
  // 変更前
  import type { CartItem } from '../contexts/CartContext'
  // 変更後
  import type { CartItem } from '@entities/cart'
  ```

  **`OrderDetailPage/index.tsx`**:
  ```ts
  // 変更前
  import * as api from '../lib/api'
  import type { Order } from '../types/api'
  // 変更後
  import { getOrderById, cancelOrder } from '@entities/order'
  import type { Order } from '@entities/order'
  // api.getOrderById(...) → getOrderById(...) に変更
  // api.cancelOrder(...) → cancelOrder(...) に変更
  ```

  **`OrderHistoryPage/index.tsx`**:
  ```ts
  // 変更前
  import { useAuth } from '../contexts/AuthContext'
  import * as api from '../lib/api'
  import type { Order } from '../types/api'
  // 変更後
  import { useAuth } from '@features/auth'
  import { getOrderHistory } from '@entities/order'
  import type { Order } from '@entities/order'
  // api.getOrderHistory() → getOrderHistory() に変更
  ```

  **`LoginPage/index.tsx`**:
  ```ts
  // 変更前
  import { useAuth } from '../contexts/AuthContext'
  // 変更後
  import { useAuth } from '@features/auth'
  ```

  **`RegisterPage/index.tsx`**:
  ```ts
  // 変更前
  import { useAuth } from '../contexts/AuthContext'
  // 変更後
  import { useAuth } from '@features/auth'
  ```

---

- [ ] **T-16**: `pages/admin/` — 管理向けページを移動（5ファイル）

  **移行対象と import 変更一覧:**

  | 旧パス | 新パス |
  |--------|--------|
  | `src/pages/AdminItemPage.tsx` | `src/pages/admin/AdminItemPage/index.tsx` |
  | `src/pages/AdminOrderPage.tsx` | `src/pages/admin/AdminOrderPage/index.tsx` |
  | `src/pages/AdminInventoryPage.tsx` | `src/pages/admin/AdminInventoryPage/index.tsx` |
  | `src/pages/AdminMembersPage.tsx` | `src/pages/admin/AdminMembersPage/index.tsx` |
  | `src/pages/BoLoginPage.tsx` | `src/pages/admin/BoLoginPage/index.tsx` |

  **ページごとの import 書き換え:**

  **`AdminItemPage/index.tsx`**:
  ```ts
  // 変更前
  import { useProducts } from '../contexts/ProductContext'
  import type { UpdateProductRequest } from '../types/api'
  // 変更後
  import { useProducts } from '@entities/product'
  import type { UpdateProductRequest } from '@entities/product'
  ```

  **`AdminOrderPage/index.tsx`**:
  ```ts
  // 変更前
  import * as api from '../lib/api'
  import type { Order } from '../types/api'
  // 変更後
  import { getAllOrders, confirmOrder, shipOrder, deliverOrder, cancelOrder } from '@entities/order'
  import type { Order } from '@entities/order'
  // api.getAllOrders() → getAllOrders() 等に変更
  ```

  **`AdminInventoryPage/index.tsx`**:
  ```ts
  // 変更前
  import * as api from '../lib/api'
  // 変更後
  import { get, post } from '@shared/api/client'
  // api.get<InventoryItem[]>(...) → get<InventoryItem[]>(...) に変更
  // api.post(...) → post(...) に変更
  ```

  **`AdminMembersPage/index.tsx`**:
  ```ts
  // 変更前
  import * as api from '../lib/api'
  import type { User } from '../types/api'
  // 変更後
  import { get, put } from '@shared/api/client'
  import type { User } from '@entities/customer'
  // api.get<User[]>(...) → get<User[]>(...) に変更
  // api.put(...) → put(...) に変更
  ```

  **`BoLoginPage/index.tsx`**:
  ```ts
  // 変更前
  import { useBoAuth } from '../contexts/BoAuthContext'
  // 変更後
  import { useBoAuth } from '@features/bo-auth'
  ```

---

### フロントエンド（app レイヤー）

---

- [ ] **T-17**: `app/router/customer.tsx` + `app/router/admin.tsx` を作成

  **① パス: `frontend/src/app/router/customer.tsx`（新規作成）**

  現在の `App.tsx` 内の customer 分岐（50〜76行目）を抜き出して関数コンポーネント化する。

  ```tsx
  import { BrowserRouter, Routes, Route } from 'react-router'
  import { AuthProvider } from '@features/auth'
  import { CartProvider } from '@features/cart'
  import { ProductProvider } from '@entities/product'
  import { CustomerLayout } from '@widgets/CustomerLayout'
  import HomePage from '@pages/customer/HomePage'
  import ItemListPage from '@pages/customer/ItemListPage'
  import ItemDetailPage from '@pages/customer/ItemDetailPage'
  import CartPage from '@pages/customer/CartPage'
  import OrderConfirmPage from '@pages/customer/OrderConfirmPage'
  import OrderCompletePage from '@pages/customer/OrderCompletePage'
  import OrderDetailPage from '@pages/customer/OrderDetailPage'
  import OrderHistoryPage from '@pages/customer/OrderHistoryPage'
  import LoginPage from '@pages/customer/LoginPage'
  import RegisterPage from '@pages/customer/RegisterPage'

  export function CustomerRouter() {
    return (
      <AuthProvider>
        <ProductProvider>
          <CartProvider>
            <BrowserRouter>
              <Routes>
                <Route element={<CustomerLayout />}>
                  <Route path="/" element={<HomePage />} />
                  <Route path="/item" element={<ItemListPage />} />
                  <Route path="/item/:id" element={<ItemDetailPage />} />
                  <Route path="/order/cart" element={<CartPage />} />
                  <Route path="/order/reg" element={<OrderConfirmPage />} />
                  <Route path="/order/complete" element={<OrderCompletePage />} />
                  <Route path="/order/:id" element={<OrderDetailPage />} />
                  <Route path="/order/history" element={<OrderHistoryPage />} />
                  <Route path="/auth/login" element={<LoginPage />} />
                  <Route path="/auth/register" element={<RegisterPage />} />
                </Route>
              </Routes>
            </BrowserRouter>
          </CartProvider>
        </ProductProvider>
      </AuthProvider>
    )
  }
  ```

  **② パス: `frontend/src/app/router/admin.tsx`（新規作成）**

  現在の `App.tsx` 内の admin 分岐（27〜48行目）を抜き出して関数コンポーネント化する。

  ```tsx
  import { BrowserRouter, Routes, Route } from 'react-router'
  import { BoAuthProvider } from '@features/bo-auth'
  import { ProductProvider } from '@entities/product'
  import { AdminLayout } from '@widgets/AdminLayout'
  import AdminItemPage from '@pages/admin/AdminItemPage'
  import AdminOrderPage from '@pages/admin/AdminOrderPage'
  import AdminInventoryPage from '@pages/admin/AdminInventoryPage'
  import AdminMembersPage from '@pages/admin/AdminMembersPage'
  import BoLoginPage from '@pages/admin/BoLoginPage'

  export function AdminRouter() {
    return (
      <ProductProvider>
        <BoAuthProvider>
          <BrowserRouter>
            <Routes>
              <Route path="/" element={<BoLoginPage />} />
              <Route path="/bo/login" element={<BoLoginPage />} />
              <Route path="/bo" element={<AdminLayout />}>
                <Route path="item" element={<AdminItemPage />} />
                <Route path="order" element={<AdminOrderPage />} />
                <Route path="inventory" element={<AdminInventoryPage />} />
                <Route path="members" element={<AdminMembersPage />} />
              </Route>
            </Routes>
          </BrowserRouter>
        </BoAuthProvider>
      </ProductProvider>
    )
  }
  ```

---

- [ ] **T-18**: `app/App.tsx` を作成、`main.tsx` の import を更新

  **① パス: `frontend/src/app/App.tsx`（新規作成）**

  ```tsx
  import { CustomerRouter } from './router/customer'
  import { AdminRouter } from './router/admin'

  export default function App() {
    const appMode = import.meta.env.VITE_APP_MODE === 'admin' ? 'admin' : 'customer'
    return appMode === 'admin' ? <AdminRouter /> : <CustomerRouter />
  }
  ```

  **② パス: `frontend/src/main.tsx`（変更）**

  変更前:
  ```tsx
  import App from './App.tsx'
  ```

  変更後:
  ```tsx
  import App from './app/App.tsx'
  ```

---

- [ ] **T-19**: 旧ファイルを削除

  以下のファイルをすべて削除する（`npm run build` でエラーがないことを確認してから行う）:

  ```bash
  # 旧 App.tsx
  rm frontend/src/App.tsx

  # 旧 contexts/
  rm -r frontend/src/contexts/

  # 旧 components/
  rm -r frontend/src/components/

  # 旧 pages/（フラット構成）
  rm frontend/src/pages/HomePage.tsx
  rm frontend/src/pages/ItemListPage.tsx
  rm frontend/src/pages/ItemDetailPage.tsx
  rm frontend/src/pages/CartPage.tsx
  rm frontend/src/pages/OrderConfirmPage.tsx
  rm frontend/src/pages/OrderCompletePage.tsx
  rm frontend/src/pages/OrderDetailPage.tsx
  rm frontend/src/pages/OrderHistoryPage.tsx
  rm frontend/src/pages/LoginPage.tsx
  rm frontend/src/pages/RegisterPage.tsx
  rm frontend/src/pages/AdminItemPage.tsx
  rm frontend/src/pages/AdminOrderPage.tsx
  rm frontend/src/pages/AdminInventoryPage.tsx
  rm frontend/src/pages/AdminMembersPage.tsx
  rm frontend/src/pages/BoLoginPage.tsx

  # 旧 lib/api.ts + lib/errorMessages.ts
  rm frontend/src/lib/api.ts
  rm frontend/src/lib/errorMessages.ts
  # lib/ ディレクトリが空になれば削除
  rmdir frontend/src/lib/

  # 旧 types/api.ts
  rm frontend/src/types/api.ts
  # types/ ディレクトリが空になれば削除
  rmdir frontend/src/types/
  ```

---

## 実装順序

```
T-1（npm install）
T-2（vite.config + tsconfig）← 並行可能
  ↓ （T-2 完了後: エイリアスが解決されるようになる）
T-4（shared/types/api.ts）
T-5（shared/config/env.ts）← T-4 と並行可能
  ↓
T-6（shared/api/client.ts）← T-4, T-5 が必要
T-7（shared/ui, shared/lib）← T-4 完了後（型依存のみ）
  ↓
T-8（entities/product）← T-4, T-6, T-7 が必要
T-9（entities/cart）← T-4, T-6, T-8(product 型) が必要
T-10（entities/order）← T-4, T-5, T-6 が必要
T-11（entities/customer + bo-user）← T-4, T-6 が必要
  ↓
T-12（features/auth）← T-11(customer) が必要
T-13（features/bo-auth + cart）← T-11(bo-user), T-9(cart) が必要
  ↓
T-14（widgets）← T-12, T-13 が必要
T-15（pages/customer）← T-8, T-9, T-10, T-11, T-12, T-13, T-7 が必要
T-16（pages/admin）← T-8, T-10, T-11, T-13, T-6 が必要
  ↓
T-17（app/router）← T-14, T-15, T-16 が必要
T-18（app/App.tsx + main.tsx）← T-17 が必要
  ↓
（npm run build でエラーなし確認）
  ↓
T-19（旧ファイル削除）
T-3（eslint.config.js）← T-1 でプラグイン install 済みなら並行可能。最後に lint チェック
```

---

## Final Gate（全タスク完了後に必ず実行し、結果をこのファイルに貼り付けること）

```bash
cd frontend && npm run build
cd frontend && npm run lint
```

**Final Gate 結果:** `docker run --rm --dns 1.1.1.1 --dns 8.8.8.8 ... npm run build` でビルド成功、`docker run --rm --dns 1.1.1.1 --dns 8.8.8.8 ... npm run lint` で lint エラーなし（いずれも frontend ワークスペースで実行）

---

## テスト手順

実装後に以下を確認:

1. **ビルド通過**
   ```bash
   cd frontend && npm run build
   ```
   エラーなしで完了すること

2. **Lint 通過**
   ```bash
   cd frontend && npm run lint
   ```
   `boundaries/element-types` と `import/no-internal-modules` のエラーなしで完了すること

3. **顧客画面の動作確認**（`npm run dev:customer`）
   - トップページ → 商品一覧 → 商品詳細 の画面遷移
   - 「カートに追加」→ カートページで数量変更・削除
   - ログイン → 注文確定 → 注文完了
   - 注文履歴ページの一覧表示
   - 未ログイン時に注文ページへアクセス → ログインへリダイレクト

4. **管理画面の動作確認**（`npm run dev:admin`）
   - BoLoginPage でログイン → 管理画面トップへ遷移
   - 商品管理ページで価格/在庫/公開状態の更新
   - 注文管理ページで注文一覧表示・ステータス変更
   - 在庫ページの表示・調整操作
   - 会員ページの一覧表示

## Review Packet
### 変更サマリ（10行以内）
- FSD 構成（`app/pages/widgets/features/entities/shared`）を新規作成し、既存フロントコードを指定先へ移設。
- API・型定義を `shared` / `entities` に分割し、旧 `src/lib/api.ts` / `src/types/api.ts` を廃止。
- Context（Auth/BoAuth/Cart/Product）を `features` / `entities` へ移設し import を alias 化。
- customer/admin ページ15ファイルを `src/pages/customer|admin/*/index.tsx` に移設。
- `app/router/customer.tsx` / `app/router/admin.tsx` / `app/App.tsx` を新設し `main.tsx` を更新。
- `vite.config.ts` / `tsconfig.app.json` / `eslint.config.js` を task 指示に合わせて更新。
- 旧 `App.tsx` / `contexts` / `components` / フラット `pages` / `types/api.ts` / `lib/api.ts` / `lib/errorMessages.ts` を削除。

### 変更ファイル一覧
- `frontend/vite.config.ts`
- `frontend/tsconfig.app.json`
- `frontend/eslint.config.js`
- `frontend/package.json`
- `frontend/src/main.tsx`
- `frontend/src/app/**`
- `frontend/src/shared/**`
- `frontend/src/entities/**`
- `frontend/src/features/**`
- `frontend/src/widgets/**`
- `frontend/src/pages/customer/**`
- `frontend/src/pages/admin/**`
- 削除: `frontend/src/App.tsx`, `frontend/src/contexts/**`, `frontend/src/components/**`, `frontend/src/pages/*.tsx`（旧15ファイル）, `frontend/src/lib/api.ts`, `frontend/src/lib/errorMessages.ts`, `frontend/src/types/api.ts`

### リスクと未解決
- ローカル `npm` 実行が環境要因で待機し続けるため、Final Gate は Docker 経由で実施。
- `tsconfig.app.json` は `paths` 追加に伴い `baseUrl` を最小追加してビルド通過。
- `src/lib/tracing.ts` が残るため `src/lib/` は空ディレクトリにならず、`rmdir` は未実施（task の「空なら削除」条件に従い現状維持）。
- `npm run dev:customer` / `npm run dev:admin` の手動シナリオは未実施。

### テスト結果（PASS/FAIL、失敗時は30行以内）
- [PASS] `cd frontend && npm run build`（Docker 実行）
- [PASS] `cd frontend && npm run lint`（Docker 実行）
- [FAIL] `cd frontend && npm run dev:customer` 手動シナリオ未実施
- [FAIL] `cd frontend && npm run dev:admin` 手動シナリオ未実施
