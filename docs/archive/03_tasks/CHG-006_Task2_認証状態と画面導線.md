# CHG-006 Task2: 認証状態と画面導線（実装タスク）

要件: `docs/01_requirements/CHG-006_Task2_認証状態と画面導線.md`
設計: `docs/02_designs/CHG-006_Task2_認証状態と画面導線.md`
作成日: 2026-02-12

---

## 検証コマンド

```bash
# フロントエンド開発サーバー起動
cd frontend
npm run dev

# TypeScriptビルド確認
npm run build

# ESLintチェック
npm run lint
```

**動作確認**:
1. http://localhost:5173 にアクセス
2. ヘッダーに「Login」「Register」リンクが表示されることを確認
3. 会員登録 → ログイン → ログアウトの一連のフローを確認
4. ページリロード後もログイン状態が復元されることを確認
5. 未ログイン時でも商品閲覧・カート・注文が可能なことを確認

---

## 実装タスク一覧

1. [型定義の追加](#task-1-型定義の追加)
2. [API関数の追加](#task-2-api関数の追加)
3. [AuthContextの作成](#task-3-authcontextの作成)
4. [App.tsxの変更](#task-4-apptsx-の変更)
5. [Layoutの変更](#task-5-layout-の変更)
6. [LoginPageの作成](#task-6-loginpage-の作成)
7. [RegisterPageの作成](#task-7-registerpage-の作成)

---

## Task 1: 型定義の追加

**ファイル**: `frontend/src/types/api.ts`

**挿入位置**: ファイル末尾に追加

**コード**:

```typescript
// ============================================
// 認証関連の型定義
// ============================================

// ユーザー情報
export interface User {
  id: number
  email: string
  displayName: string
  createdAt: string
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
```

**参考**: 既存の`Product`, `Cart`などの型定義パターンを踏襲

---

## Task 2: API関数の追加

**ファイル**: `frontend/src/lib/api.ts`

### 2-1. 型のインポート追加

**挿入位置**: ファイル冒頭のimport文に追加

**変更前**:
```typescript
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
} from '../types/api'
```

**変更後**:
```typescript
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
```

### 2-2. fetchApi関数の拡張

**挿入位置**: `fetchApi`関数内、ヘッダー設定部分（`if (!headers.has('Content-Type'))`の後）

**変更前**:
```typescript
async function fetchApi<T>(
  endpoint: string,
  options: RequestInit = {}
): Promise<ApiResponse<T>> {
  const url = `${API_BASE_URL}${endpoint}`
  const headers = new Headers(options.headers)
  if (!headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json')
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

    const data = await response.json()
    return data
  } catch (error) {
    // ... 既存のエラーハンドリング
  }
}
```

**変更後**:
```typescript
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
```

### 2-3. 認証API関数の追加

**挿入位置**: ファイル末尾、`export { getSessionId }`の前に追加

**コード**:

```typescript
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
```

**参考**: 既存の`getCart`, `addToCart`などのAPI関数パターンを踏襲

---

## Task 3: AuthContextの作成

**ファイル**: `frontend/src/contexts/AuthContext.tsx`（新規作成）

**コード全体**:

```typescript
import { createContext, useContext, useState, useEffect } from 'react'
import type { ReactNode } from 'react'
import type { User } from '../types/api'
import * as api from '../lib/api'

interface AuthContextType {
  user: User | null
  token: string | null
  isAuthenticated: boolean
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

  // 初期化: localStorageからトークンを復元し、検証
  useEffect(() => {
    refreshUser()
  }, [])

  // 401エラー時のハンドリング
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
      const response = await api.getCurrentUser()
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
      const response = await api.register({ email, displayName, password })
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
      const response = await api.login({ email, password })
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
      await api.logout()
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

  return (
    <AuthContext.Provider
      value={{
        user,
        token,
        isAuthenticated: !!user,
        loading,
        error,
        register,
        login,
        logout,
        refreshUser,
        clearError,
      }}
    >
      {children}
    </AuthContext.Provider>
  )
}

// カスタムフック
// eslint-disable-next-line react-refresh/only-export-components
export function useAuth() {
  const context = useContext(AuthContext)
  if (context === undefined) {
    throw new Error('useAuth must be used within an AuthProvider')
  }
  return context
}
```

**参考**: `frontend/src/contexts/CartContext.tsx`のパターンを踏襲

---

## Task 4: App.tsx の変更

**ファイル**: `frontend/src/App.tsx`

### 4-1. AuthProviderのインポート追加

**挿入位置**: ファイル冒頭のimport文に追加

**変更前**:
```typescript
import { BrowserRouter, Routes, Route } from 'react-router'
import { CartProvider } from './contexts/CartContext'
import { ProductProvider } from './contexts/ProductContext'
import Layout from './components/Layout'
// ... 既存のページインポート
```

**変更後**:
```typescript
import { BrowserRouter, Routes, Route } from 'react-router'
import { AuthProvider } from './contexts/AuthContext'
import { CartProvider } from './contexts/CartContext'
import { ProductProvider } from './contexts/ProductContext'
import Layout from './components/Layout'
// ... 既存のページインポート
import LoginPage from './pages/LoginPage'
import RegisterPage from './pages/RegisterPage'
```

### 4-2. AuthProviderでラップ

**挿入位置**: `export default function App()`内

**変更前**:
```typescript
export default function App() {
  return (
    <ProductProvider>
      <CartProvider>
        <BrowserRouter>
          <Routes>
            <Route element={<Layout />}>
              <Route path="/" element={<HomePage />} />
              <Route path="/item" element={<ItemListPage />} />
              <Route path="/item/:id" element={<ItemDetailPage />} />
              <Route path="/order/cart" element={<CartPage />} />
              <Route path="/order/reg" element={<OrderConfirmPage />} />
              <Route path="/order/complete" element={<OrderCompletePage />} />
              <Route path="/order/:id" element={<OrderDetailPage />} />
              <Route path="/bo/item" element={<AdminItemPage />} />
              <Route path="/bo/order" element={<AdminOrderPage />} />
            </Route>
          </Routes>
        </BrowserRouter>
      </CartProvider>
    </ProductProvider>
  )
}
```

**変更後**:
```typescript
export default function App() {
  return (
    <AuthProvider>
      <ProductProvider>
        <CartProvider>
          <BrowserRouter>
            <Routes>
              <Route element={<Layout />}>
                <Route path="/" element={<HomePage />} />
                <Route path="/item" element={<ItemListPage />} />
                <Route path="/item/:id" element={<ItemDetailPage />} />
                <Route path="/order/cart" element={<CartPage />} />
                <Route path="/order/reg" element={<OrderConfirmPage />} />
                <Route path="/order/complete" element={<OrderCompletePage />} />
                <Route path="/order/:id" element={<OrderDetailPage />} />
                <Route path="/bo/item" element={<AdminItemPage />} />
                <Route path="/bo/order" element={<AdminOrderPage />} />

                {/* 認証画面 */}
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

**参考**: 既存のProvider階層構造を維持

---

## Task 5: Layout の変更

**ファイル**: `frontend/src/components/Layout.tsx`

### 5-1. useAuthのインポート追加

**挿入位置**: ファイル冒頭のimport文に追加

**変更前**:
```typescript
import { Link, Outlet } from 'react-router'
import { useCart } from '../contexts/CartContext'
```

**変更後**:
```typescript
import { Link, Outlet } from 'react-router'
import { useCart } from '../contexts/CartContext'
import { useAuth } from '../contexts/AuthContext'
```

### 5-2. 認証状態の取得

**挿入位置**: `export default function Layout()`の最初

**変更前**:
```typescript
export default function Layout() {
  const { totalQuantity } = useCart()
```

**変更後**:
```typescript
export default function Layout() {
  const { totalQuantity } = useCart()
  const { user, isAuthenticated, logout } = useAuth()
```

### 5-3. ヘッダーの右側ナビゲーション変更

**挿入位置**: ヘッダー内の右側ナビゲーション部分

**変更前**:
```typescript
{/* 右側ナビゲーション */}
<div className="flex items-center space-x-6 text-xs uppercase tracking-widest">
  <Link
    to="/order/cart"
    className="relative hover:text-zinc-600 transition-colors"
  >
    Cart
    {totalQuantity > 0 && (
      <span className="absolute -right-3 -top-2 flex h-5 w-5 items-center justify-center rounded-full bg-zinc-900 text-xs font-bold text-white">
        {totalQuantity}
      </span>
    )}
  </Link>
</div>
```

**変更後**:
```typescript
{/* 右側ナビゲーション */}
<div className="flex items-center space-x-6 text-xs uppercase tracking-widest">
  <Link
    to="/order/cart"
    className="relative hover:text-zinc-600 transition-colors"
  >
    Cart
    {totalQuantity > 0 && (
      <span className="absolute -right-3 -top-2 flex h-5 w-5 items-center justify-center rounded-full bg-zinc-900 text-xs font-bold text-white">
        {totalQuantity}
      </span>
    )}
  </Link>

  {/* 認証状態 */}
  {isAuthenticated ? (
    <>
      <span className="text-xs text-zinc-700">{user?.displayName}</span>
      <button
        onClick={logout}
        className="hover:text-zinc-600 transition-colors"
      >
        Logout
      </button>
    </>
  ) : (
    <>
      <Link to="/auth/login" className="hover:text-zinc-600 transition-colors">
        Login
      </Link>
      <Link to="/auth/register" className="hover:text-zinc-600 transition-colors">
        Register
      </Link>
    </>
  )}
</div>
```

---

## Task 6: LoginPage の作成

**ファイル**: `frontend/src/pages/LoginPage.tsx`（新規作成）

**コード全体**:

```typescript
import { useState } from 'react'
import { useNavigate, Link } from 'react-router'
import { useAuth } from '../contexts/AuthContext'

export default function LoginPage() {
  const navigate = useNavigate()
  const { login, loading, error, clearError } = useAuth()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    clearError()

    try {
      await login(email, password)
      navigate('/') // ログイン成功後にトップページへ
    } catch (err) {
      // エラーはAuthContextで管理されている
    }
  }

  return (
    <div className="mx-auto max-w-md px-6 py-12">
      <h1 className="mb-8 text-center font-serif text-3xl tracking-wider">
        Login
      </h1>

      {error && (
        <div className="mb-6 rounded border border-red-200 bg-red-50 p-4 text-sm text-red-700">
          {error}
        </div>
      )}

      <form onSubmit={handleSubmit} className="space-y-6">
        <div>
          <label className="block text-sm font-medium text-zinc-700 mb-2">
            Email
          </label>
          <input
            type="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            required
            className="w-full rounded border border-zinc-300 px-4 py-3 text-sm focus:border-zinc-900 focus:outline-none"
          />
        </div>

        <div>
          <label className="block text-sm font-medium text-zinc-700 mb-2">
            Password
          </label>
          <input
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            required
            className="w-full rounded border border-zinc-300 px-4 py-3 text-sm focus:border-zinc-900 focus:outline-none"
          />
        </div>

        <button
          type="submit"
          disabled={loading}
          className="w-full rounded bg-zinc-900 px-4 py-3 text-sm uppercase tracking-widest text-white hover:bg-zinc-800 disabled:opacity-50 transition-colors"
        >
          {loading ? 'Logging in...' : 'Login'}
        </button>
      </form>

      <p className="mt-8 text-center text-sm text-zinc-600">
        アカウントをお持ちでない方は{' '}
        <Link to="/auth/register" className="text-zinc-900 underline hover:text-zinc-700">
          会員登録
        </Link>
      </p>
    </div>
  )
}
```

**参考**: 既存のページコンポーネント（`CartPage.tsx`など）のスタイルパターンを踏襲

---

## Task 7: RegisterPage の作成

**ファイル**: `frontend/src/pages/RegisterPage.tsx`（新規作成）

**コード全体**:

```typescript
import { useState } from 'react'
import { useNavigate, Link } from 'react-router'
import { useAuth } from '../contexts/AuthContext'

export default function RegisterPage() {
  const navigate = useNavigate()
  const { register, loading, error, clearError } = useAuth()
  const [email, setEmail] = useState('')
  const [displayName, setDisplayName] = useState('')
  const [password, setPassword] = useState('')
  const [passwordConfirm, setPasswordConfirm] = useState('')
  const [validationError, setValidationError] = useState<string | null>(null)

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    clearError()
    setValidationError(null)

    if (password !== passwordConfirm) {
      setValidationError('パスワードが一致しません')
      return
    }

    if (password.length < 8) {
      setValidationError('パスワードは8文字以上で入力してください')
      return
    }

    try {
      await register(email, displayName, password)
      navigate('/') // 登録成功後にトップページへ
    } catch (err) {
      // エラーはAuthContextで管理されている
    }
  }

  const displayError = validationError || error

  return (
    <div className="mx-auto max-w-md px-6 py-12">
      <h1 className="mb-8 text-center font-serif text-3xl tracking-wider">
        Register
      </h1>

      {displayError && (
        <div className="mb-6 rounded border border-red-200 bg-red-50 p-4 text-sm text-red-700">
          {displayError}
        </div>
      )}

      <form onSubmit={handleSubmit} className="space-y-6">
        <div>
          <label className="block text-sm font-medium text-zinc-700 mb-2">
            Email
          </label>
          <input
            type="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            required
            className="w-full rounded border border-zinc-300 px-4 py-3 text-sm focus:border-zinc-900 focus:outline-none"
          />
        </div>

        <div>
          <label className="block text-sm font-medium text-zinc-700 mb-2">
            Display Name
          </label>
          <input
            type="text"
            value={displayName}
            onChange={(e) => setDisplayName(e.target.value)}
            required
            maxLength={100}
            className="w-full rounded border border-zinc-300 px-4 py-3 text-sm focus:border-zinc-900 focus:outline-none"
          />
        </div>

        <div>
          <label className="block text-sm font-medium text-zinc-700 mb-2">
            Password
          </label>
          <input
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            required
            minLength={8}
            className="w-full rounded border border-zinc-300 px-4 py-3 text-sm focus:border-zinc-900 focus:outline-none"
          />
          <p className="mt-1 text-xs text-zinc-500">8文字以上で入力してください</p>
        </div>

        <div>
          <label className="block text-sm font-medium text-zinc-700 mb-2">
            Password (Confirm)
          </label>
          <input
            type="password"
            value={passwordConfirm}
            onChange={(e) => setPasswordConfirm(e.target.value)}
            required
            minLength={8}
            className="w-full rounded border border-zinc-300 px-4 py-3 text-sm focus:border-zinc-900 focus:outline-none"
          />
        </div>

        <button
          type="submit"
          disabled={loading}
          className="w-full rounded bg-zinc-900 px-4 py-3 text-sm uppercase tracking-widest text-white hover:bg-zinc-800 disabled:opacity-50 transition-colors"
        >
          {loading ? 'Registering...' : 'Register'}
        </button>
      </form>

      <p className="mt-8 text-center text-sm text-zinc-600">
        すでにアカウントをお持ちの方は{' '}
        <Link to="/auth/login" className="text-zinc-900 underline hover:text-zinc-700">
          ログイン
        </Link>
      </p>
    </div>
  )
}
```

**参考**: `LoginPage.tsx`と同様のスタイルパターン

---

## 実装の順序

1. **型定義** → **API関数** → **AuthContext** の順で実装（依存関係の順）
2. **App.tsx** → **Layout** → **ページコンポーネント** の順で実装（外側から内側へ）
3. 各ステップでTypeScriptのビルドエラーがないことを確認

---

## テスト手順

### 1. 会員登録フロー

1. http://localhost:5173/auth/register にアクセス
2. フォームに入力:
   - Email: `test@example.com`
   - Display Name: `テストユーザー`
   - Password: `password123`
   - Password (Confirm): `password123`
3. 「Register」ボタンをクリック
4. 成功: トップページにリダイレクト、ヘッダーに「テストユーザー」と「Logout」が表示される
5. 開発者ツール → Application → Local Storage:
   - `authToken`: トークン文字列（UUID形式）が保存されている
   - `authUser`: ユーザー情報のJSON文字列が保存されている

### 2. ログアウトフロー

1. ヘッダーの「Logout」ボタンをクリック
2. ヘッダーに「Login」「Register」が表示される
3. Local Storageから`authToken`, `authUser`が削除されている

### 3. ログインフロー

1. http://localhost:5173/auth/login にアクセス
2. フォームに入力:
   - Email: `test@example.com`
   - Password: `password123`
3. 「Login」ボタンをクリック
4. 成功: トップページにリダイレクト、ヘッダーに「テストユーザー」と「Logout」が表示される

### 4. ログイン状態の復元

1. ログイン状態でページをリロード（Ctrl+R）
2. リロード後もヘッダーに「テストユーザー」と「Logout」が表示される
3. ログイン状態が維持されている

### 5. 認証エラーのテスト

1. ログインフォームで間違ったパスワードを入力
2. 「メールアドレスまたはパスワードが正しくありません」エラーが表示される
3. 会員登録フォームで既に存在するメールアドレスを入力
4. 「このメールアドレスは既に登録されています」エラーが表示される

### 6. 既存導線維持の確認

1. ログアウト状態で以下の操作が可能なことを確認:
   - 商品一覧の閲覧（/item）
   - 商品詳細の閲覧（/item/:id）
   - カートへの商品追加
   - 注文の完了

### 7. トークン無効時の挙動

1. 開発者ツール → Application → Local Storage → `authToken`の値を適当な文字列に変更
2. ページをリロード
3. 自動的にログアウト状態になる（ヘッダーに「Login」「Register」が表示される）
4. Local Storageから`authToken`, `authUser`が削除されている

---

## 注意事項

- **バックエンドの起動**: Task1で実装された認証APIが起動していることを確認
- **CORS設定**: バックエンドのCORS設定で`http://localhost:5173`が許可されていることを確認
- **データベース**: バックエンドのデータベースに`users`テーブルと`auth_tokens`テーブルが作成されていることを確認
- **ESLintエラー**: `useEffect`の依存配列に関する警告が出る場合は、適切に対処（`eslint-disable-next-line`など）

---

## トラブルシューティング

### ログイン後にトークンが保存されない
- ブラウザのLocalStorageが有効か確認
- プライベートブラウジングモードでないか確認

### 401エラーが連続して発生する
- バックエンドのトークン検証ロジックを確認
- トークンの形式（`Bearer <token>`）が正しいか確認

### ページリロード後にログイン状態が復元されない
- `AuthContext`の初期化ロジック（`useEffect`）が実行されているか確認
- `api.getCurrentUser()`のレスポンスを確認

### TypeScriptのビルドエラー
- 型定義が正しくインポートされているか確認
- `npm install`で依存関係が最新か確認
