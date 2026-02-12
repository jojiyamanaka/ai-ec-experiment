# CHG-006 Task2: 認証状態と画面導線（設計）

要件: `docs/01_requirements/CHG-006_Task2_認証状態と画面導線.md`
作成日: 2026-02-12

---

## 1. 設計方針

Task1で実装された認証APIを活用し、フロントエンドに認証状態管理と画面導線を追加する。

- **認証状態管理**: `AuthContext`（React Context API）で認証状態を管理
- **永続化**: `localStorage`にトークンとユーザー情報を保存し、リロード後も復元
- **API連携**: `api.ts`を拡張し、認証が必要なAPIに`Authorization`ヘッダーを自動付与
- **既存導線維持**: 未ログイン時でも商品閲覧・カート・注文は継続可能
- **エラーハンドリング**: 401エラー時はトークンをクリアしてログイン導線へ誘導
- **既存パターン踏襲**: `CartContext`, `ProductContext`と同様のContext Providerパターン

---

## 2. アーキテクチャ

### 2-1. 認証状態管理（AuthContext）

既存の`CartContext`と同様のパターンで実装。

```tsx
interface AuthContextType {
  user: User | null              // 現在のログインユーザー（未ログイン時はnull）
  token: string | null           // 認証トークン
  isAuthenticated: boolean       // ログイン状態フラグ
  loading: boolean               // 認証処理中フラグ
  error: string | null           // エラーメッセージ

  // アクション
  register: (email: string, displayName: string, password: string) => Promise<void>
  login: (email: string, password: string) => Promise<void>
  logout: () => Promise<void>
  refreshUser: () => Promise<void>  // トークンからユーザー情報を再取得
  clearError: () => void
}
```

**状態の永続化**:
- `localStorage.setItem('authToken', token)` — トークンを保存
- `localStorage.setItem('authUser', JSON.stringify(user))` — ユーザー情報を保存
- 初回マウント時に`localStorage`から復元し、`refreshUser()`でトークン有効性を検証

**ライフサイクル**:
1. **初期化**: `useEffect`で`localStorage`からトークン・ユーザーを復元 → `refreshUser()`で検証
2. **ログイン/登録**: API呼び出し → トークン・ユーザーを`state`と`localStorage`に保存
3. **ログアウト**: トークン失効API呼び出し → `localStorage`クリア → `state`をnullにリセット
4. **401エラー**: API呼び出しで401エラー発生 → `localStorage`クリア → `state`をnullにリセット

### 2-2. API層の拡張（api.ts）

認証系APIの追加と`fetchApi`の拡張。

```typescript
// 認証系API追加
export async function register(request: RegisterRequest): Promise<ApiResponse<AuthResponse>>
export async function login(request: LoginRequest): Promise<ApiResponse<AuthResponse>>
export async function logout(): Promise<ApiResponse<{ message: string }>>
export async function getCurrentUser(): Promise<ApiResponse<User>>

// fetchApi の拡張 — Authorizationヘッダーの自動付与
async function fetchApi<T>(
  endpoint: string,
  options: RequestInit = {}
): Promise<ApiResponse<T>> {
  const headers = new Headers(options.headers)

  // 認証トークンがあれば自動付与（ログアウトエンドポイント以外）
  const token = localStorage.getItem('authToken')
  if (token && !endpoint.includes('/auth/login') && !endpoint.includes('/auth/register')) {
    headers.set('Authorization', `Bearer ${token}`)
  }

  // セッションIDの自動付与（既存）
  if (endpoint.includes('/order')) {
    headers.set('X-Session-Id', getSessionId())
  }

  // ... 既存のfetchロジック

  // 401エラーのハンドリング
  if (!response.ok && response.status === 401) {
    // トークンが無効な場合はlocalStorageをクリア
    localStorage.removeItem('authToken')
    localStorage.removeItem('authUser')
    // カスタムイベントを発火してAuthContextに通知
    window.dispatchEvent(new Event('auth:unauthorized'))
  }
}
```

**Authorizationヘッダーの自動付与ロジック**:
- トークンが存在し、かつログイン/登録以外のエンドポイントには自動で`Authorization: Bearer <token>`を付与
- 将来の会員専用API（注文履歴など）でも自動的に認証が適用される

**401エラーのハンドリング**:
- API呼び出しで401エラーが発生した場合、`localStorage`をクリアし、カスタムイベントを発火
- `AuthContext`がこのイベントをリッスンして状態をリセット

### 2-3. UI構成

#### 新規追加ページ
- `/auth/register` — 会員登録ページ
- `/auth/login` — ログインページ

#### 変更コンポーネント
- `Layout.tsx` — ヘッダーにログイン状態とログアウトボタンを追加

```tsx
// Layout.tsx の変更イメージ
export default function Layout() {
  const { totalQuantity } = useCart()
  const { user, isAuthenticated, logout } = useAuth()

  return (
    <header>
      {/* 既存の左側・中央ナビゲーション */}

      {/* 右側ナビゲーション */}
      <div className="flex items-center space-x-6">
        {/* カート（既存） */}
        <Link to="/order/cart">Cart ({totalQuantity})</Link>

        {/* 認証状態表示（新規） */}
        {isAuthenticated ? (
          <>
            <span className="text-sm">{user?.displayName}</span>
            <button onClick={logout}>Logout</button>
          </>
        ) : (
          <>
            <Link to="/auth/login">Login</Link>
            <Link to="/auth/register">Register</Link>
          </>
        )}
      </div>
    </header>
  )
}
```

---

## 3. データモデル

### 3-1. 型定義（types/api.ts への追加）

```typescript
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

### 3-2. localStorage のキー

| キー | 値 | 説明 |
|------|-----|------|
| `authToken` | `string` | 認証トークン（UUID形式） |
| `authUser` | `string` (JSON) | ユーザー情報（`User`オブジェクトをJSON化） |
| `sessionId` | `string` | セッションID（既存、ゲストカート用） |

---

## 4. API設計（フロントエンド側）

### 4-1. 会員登録

```typescript
// api.ts
export async function register(request: RegisterRequest): Promise<ApiResponse<AuthResponse>> {
  return fetchApi<AuthResponse>('/api/auth/register', {
    method: 'POST',
    body: JSON.stringify(request),
  })
}

// 使用例（AuthContext内）
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
    setError(err.message)
    throw err
  } finally {
    setLoading(false)
  }
}
```

### 4-2. ログイン

```typescript
// api.ts
export async function login(request: LoginRequest): Promise<ApiResponse<AuthResponse>> {
  return fetchApi<AuthResponse>('/api/auth/login', {
    method: 'POST',
    body: JSON.stringify(request),
  })
}

// 使用例（AuthContext内）
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
    setError(err.message)
    throw err
  } finally {
    setLoading(false)
  }
}
```

### 4-3. ログアウト

```typescript
// api.ts
export async function logout(): Promise<ApiResponse<{ message: string }>> {
  return fetchApi<{ message: string }>('/api/auth/logout', {
    method: 'POST',
  })
}

// 使用例（AuthContext内）
const logout = async () => {
  setLoading(true)
  try {
    // APIでトークンを失効
    await api.logout()
  } catch (err) {
    console.error('ログアウトエラー:', err)
  } finally {
    // 成功・失敗にかかわらず、ローカル状態はクリア
    setUser(null)
    setToken(null)
    localStorage.removeItem('authToken')
    localStorage.removeItem('authUser')
    setLoading(false)
  }
}
```

### 4-4. 会員情報取得（トークン検証）

```typescript
// api.ts
export async function getCurrentUser(): Promise<ApiResponse<User>> {
  return fetchApi<User>('/api/auth/me')
}

// 使用例（AuthContext初期化時）
const refreshUser = async () => {
  const token = localStorage.getItem('authToken')
  if (!token) {
    setUser(null)
    setToken(null)
    return
  }

  setLoading(true)
  try {
    const response = await api.getCurrentUser()
    if (response.success && response.data) {
      setUser(response.data)
      setToken(token)
      localStorage.setItem('authUser', JSON.stringify(response.data))
    } else {
      // トークンが無効な場合はクリア
      setUser(null)
      setToken(null)
      localStorage.removeItem('authToken')
      localStorage.removeItem('authUser')
    }
  } catch (err) {
    console.error('ユーザー情報取得エラー:', err)
    setUser(null)
    setToken(null)
    localStorage.removeItem('authToken')
    localStorage.removeItem('authUser')
  } finally {
    setLoading(false)
  }
}
```

---

## 5. フロントエンド実装

### 5-1. 新規追加: Context

**`frontend/src/contexts/AuthContext.tsx`**

```tsx
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
      setError(err instanceof Error ? err.message : '登録エラー')
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
      setError(err instanceof Error ? err.message : 'ログインエラー')
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

export function useAuth() {
  const context = useContext(AuthContext)
  if (context === undefined) {
    throw new Error('useAuth must be used within an AuthProvider')
  }
  return context
}
```

### 5-2. 変更: App.tsx

`AuthProvider`を`ProductProvider`、`CartProvider`と同じレベルで追加。

```tsx
import { AuthProvider } from './contexts/AuthContext' // 追加

export default function App() {
  return (
    <AuthProvider>  {/* 追加 */}
      <ProductProvider>
        <CartProvider>
          <BrowserRouter>
            <Routes>
              {/* 既存のルート */}

              {/* 認証画面（新規） */}
              <Route path="/auth/register" element={<RegisterPage />} />
              <Route path="/auth/login" element={<LoginPage />} />
            </Routes>
          </BrowserRouter>
        </CartProvider>
      </ProductProvider>
    </AuthProvider>  {/* 追加 */}
  )
}
```

**配置順序の理由**:
- `AuthProvider`を最外層に配置することで、`CartProvider`や`ProductProvider`内でも認証状態を参照可能
- 将来的に「ログインユーザーのカートのみ取得」のような要件にも対応しやすい

### 5-3. 変更: api.ts

```typescript
// 型定義のインポート追加
import type {
  // ... 既存の型
  User,
  AuthResponse,
  RegisterRequest,
  LoginRequest,
} from '../types/api'

// fetchApi の変更
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

  // セッションIDの自動付与（既存）
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
// 認証 API（新規追加）
// ============================================

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
  return fetchApi<{ message: string }>('/api/auth/logout', {
    method: 'POST',
  })
}

export async function getCurrentUser(): Promise<ApiResponse<User>> {
  return fetchApi<User>('/api/auth/me')
}
```

### 5-4. 変更: types/api.ts

認証関連の型を追加。

```typescript
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

### 5-5. 変更: Layout.tsx

ヘッダーに認証状態とログアウトボタンを追加。

```tsx
import { Link, Outlet } from 'react-router'
import { useCart } from '../contexts/CartContext'
import { useAuth } from '../contexts/AuthContext' // 追加

export default function Layout() {
  const { totalQuantity } = useCart()
  const { user, isAuthenticated, logout } = useAuth() // 追加

  return (
    <div className="flex min-h-screen flex-col bg-stone-50">
      <header className="fixed top-0 left-0 right-0 z-50 bg-white/80 backdrop-blur-md border-b border-stone-200">
        <div className="mx-auto flex max-w-7xl items-center justify-between px-6 h-20">
          {/* 左側ナビゲーション（既存） */}
          <nav className="hidden md:flex space-x-8 text-xs tracking-widest uppercase">
            <Link to="/item" className="hover:text-zinc-600 transition-colors">
              Collection
            </Link>
          </nav>

          {/* 中央ロゴ（既存） */}
          <Link to="/" className="font-serif text-2xl tracking-[0.2em] text-zinc-900">
            AI EC Shop
          </Link>

          {/* 右側ナビゲーション */}
          <div className="flex items-center space-x-6 text-xs uppercase tracking-widest">
            {/* カート（既存） */}
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

            {/* 認証状態（新規） */}
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
        </div>
      </header>

      {/* main, footer は既存のまま */}
      <main className="flex-1 pt-20">
        <Outlet />
      </main>
      <footer className="border-t border-stone-200 bg-stone-50">
        <div className="mx-auto max-w-7xl px-6 py-6">
          <div className="flex items-center justify-between">
            <p className="text-sm text-zinc-500">
              © 2025 AI EC Shop. All rights reserved.
            </p>
            <Link
              to="/bo/item"
              className="text-xs text-zinc-400 hover:text-zinc-600"
            >
              管理画面
            </Link>
          </div>
        </div>
      </footer>
    </div>
  )
}
```

### 5-6. 新規追加: ページコンポーネント

**`frontend/src/pages/LoginPage.tsx`**

```tsx
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
      <h1 className="mb-8 text-center font-serif text-3xl">Login</h1>

      {error && (
        <div className="mb-4 rounded bg-red-50 p-3 text-sm text-red-600">
          {error}
        </div>
      )}

      <form onSubmit={handleSubmit} className="space-y-6">
        <div>
          <label className="block text-sm font-medium text-zinc-700">
            Email
          </label>
          <input
            type="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            required
            className="mt-1 w-full rounded border border-zinc-300 px-3 py-2"
          />
        </div>

        <div>
          <label className="block text-sm font-medium text-zinc-700">
            Password
          </label>
          <input
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            required
            className="mt-1 w-full rounded border border-zinc-300 px-3 py-2"
          />
        </div>

        <button
          type="submit"
          disabled={loading}
          className="w-full rounded bg-zinc-900 px-4 py-3 text-sm uppercase tracking-widest text-white hover:bg-zinc-800 disabled:opacity-50"
        >
          {loading ? 'Logging in...' : 'Login'}
        </button>
      </form>

      <p className="mt-6 text-center text-sm text-zinc-600">
        アカウントをお持ちでない方は{' '}
        <Link to="/auth/register" className="text-zinc-900 underline">
          会員登録
        </Link>
      </p>
    </div>
  )
}
```

**`frontend/src/pages/RegisterPage.tsx`**

```tsx
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

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    clearError()

    if (password !== passwordConfirm) {
      alert('パスワードが一致しません')
      return
    }

    try {
      await register(email, displayName, password)
      navigate('/') // 登録成功後にトップページへ
    } catch (err) {
      // エラーはAuthContextで管理されている
    }
  }

  return (
    <div className="mx-auto max-w-md px-6 py-12">
      <h1 className="mb-8 text-center font-serif text-3xl">Register</h1>

      {error && (
        <div className="mb-4 rounded bg-red-50 p-3 text-sm text-red-600">
          {error}
        </div>
      )}

      <form onSubmit={handleSubmit} className="space-y-6">
        <div>
          <label className="block text-sm font-medium text-zinc-700">
            Email
          </label>
          <input
            type="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            required
            className="mt-1 w-full rounded border border-zinc-300 px-3 py-2"
          />
        </div>

        <div>
          <label className="block text-sm font-medium text-zinc-700">
            Display Name
          </label>
          <input
            type="text"
            value={displayName}
            onChange={(e) => setDisplayName(e.target.value)}
            required
            className="mt-1 w-full rounded border border-zinc-300 px-3 py-2"
          />
        </div>

        <div>
          <label className="block text-sm font-medium text-zinc-700">
            Password
          </label>
          <input
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            required
            minLength={8}
            className="mt-1 w-full rounded border border-zinc-300 px-3 py-2"
          />
          <p className="mt-1 text-xs text-zinc-500">8文字以上</p>
        </div>

        <div>
          <label className="block text-sm font-medium text-zinc-700">
            Password (Confirm)
          </label>
          <input
            type="password"
            value={passwordConfirm}
            onChange={(e) => setPasswordConfirm(e.target.value)}
            required
            minLength={8}
            className="mt-1 w-full rounded border border-zinc-300 px-3 py-2"
          />
        </div>

        <button
          type="submit"
          disabled={loading}
          className="w-full rounded bg-zinc-900 px-4 py-3 text-sm uppercase tracking-widest text-white hover:bg-zinc-800 disabled:opacity-50"
        >
          {loading ? 'Registering...' : 'Register'}
        </button>
      </form>

      <p className="mt-6 text-center text-sm text-zinc-600">
        すでにアカウントをお持ちの方は{' '}
        <Link to="/auth/login" className="text-zinc-900 underline">
          ログイン
        </Link>
      </p>
    </div>
  )
}
```

---

## 6. 処理フロー

### 6-1. 初回アクセス時

```
ユーザー → アプリ起動
  → AuthProvider: useEffect
    → localStorage から authToken を取得
    → トークンなし → loading = false, user = null（未ログイン状態）
    → トークンあり → GET /api/auth/me
      → 成功 → user をセット、loading = false（ログイン状態復元）
      → 失敗（401） → localStorage クリア、user = null（トークン無効）
```

### 6-2. 会員登録フロー

```
ユーザー → /auth/register
  → フォーム入力（email, displayName, password）
  → submit
    → AuthContext.register()
      → POST /api/auth/register
        → 成功
          → user, token を state と localStorage に保存
          → navigate('/') でトップページへ
        → 失敗（409: EMAIL_ALREADY_EXISTS）
          → error メッセージを表示
        → 失敗（400: INVALID_REQUEST）
          → error メッセージを表示
```

### 6-3. ログインフロー

```
ユーザー → /auth/login
  → フォーム入力（email, password）
  → submit
    → AuthContext.login()
      → POST /api/auth/login
        → 成功
          → user, token を state と localStorage に保存
          → navigate('/') でトップページへ
        → 失敗（401: INVALID_CREDENTIALS）
          → error メッセージを表示
```

### 6-4. ログアウトフロー

```
ユーザー → Layout の Logout ボタンクリック
  → AuthContext.logout()
    → POST /api/auth/logout
      → 成功・失敗にかかわらず
        → user, token を null にリセット
        → localStorage をクリア
        → トップページへリダイレクト（オプション）
```

### 6-5. API呼び出し時の認証エラーハンドリング

```
ユーザー → 任意のAPI呼び出し（例: GET /api/auth/me）
  → fetchApi()
    → localStorage から authToken を取得
    → Authorization: Bearer <token> ヘッダーを付与
    → fetch()
      → レスポンス: 401 Unauthorized
        → localStorage クリア
        → window.dispatchEvent('auth:unauthorized')
  → AuthContext: useEffect でイベントをリッスン
    → user, token を null にリセット
    → ユーザーに「ログインが必要です」メッセージ表示（オプション）
```

---

## 7. 既存パターンとの整合性

| 観点 | 既存パターン | CHG-006 Task2 |
|------|-------------|---------------|
| Context | ProductProvider, CartProvider | AuthProvider |
| カスタムフック | useCart(), useProduct() | useAuth() |
| API呼び出し | api.ts の関数経由 | 同様（register, login, logout, getCurrentUser 追加） |
| エラーハンドリング | try-catch + setError | 同様 |
| ローディング状態 | loading フラグ | 同様 |
| レスポンス型 | ApiResponse\<T> | 同様 |
| localStorage | sessionId（カート用） | authToken, authUser（認証用） |

---

## 8. セキュリティ考慮事項

### 8-1. トークン管理
- **localStorage**: トークンを`localStorage`に保存（XSS攻撃には注意が必要だが、HTTPSで運用し、CSP設定で緩和）
- **トークン自動付与**: `fetchApi`で`Authorization`ヘッダーを自動付与
- **トークン失効**: ログアウト時にバックエンドでトークンを失効（`isRevoked = true`）

### 8-2. 401エラーハンドリング
- API呼び出しで401エラーが発生した場合、自動的にトークンをクリアして認証状態をリセット
- ユーザーに「ログインが必要です」というメッセージを表示（オプション）

### 8-3. パスワード管理
- パスワードはフォームで入力後、即座にAPIへ送信（state には保存しない）
- バックエンドでBCryptによるハッシュ化を実施（Task1で対応済み）

### 8-4. HTTPS
- 本番環境では必須（トークン盗聴対策） — 別タスクで対応

---

## 9. テスト観点

### 9-1. 認証状態管理
- [ ] 初回アクセス時、localStorageにトークンがない → 未ログイン状態
- [ ] 初回アクセス時、localStorageにトークンがある → `/api/auth/me`でユーザー情報取得 → ログイン状態復元
- [ ] トークンが無効な場合 → localStorageクリア → 未ログイン状態
- [ ] ログイン後、ページリロード → ログイン状態が復元される

### 9-2. 会員登録
- [ ] 正常登録 → トークン発行 → ログイン状態になる → トップページへリダイレクト
- [ ] メール重複 → エラーメッセージ表示
- [ ] パスワード8文字未満 → バリデーションエラー
- [ ] パスワード確認不一致 → クライアント側でエラー

### 9-3. ログイン
- [ ] 正しい認証情報 → トークン発行 → ログイン状態になる → トップページへリダイレクト
- [ ] 間違った認証情報 → エラーメッセージ表示
- [ ] 存在しないメール → エラーメッセージ表示

### 9-4. ログアウト
- [ ] ログアウト → トークンクリア → 未ログイン状態
- [ ] ログアウト後、ページリロード → 未ログイン状態が維持される

### 9-5. UI表示
- [ ] 未ログイン時: ヘッダーに「Login」「Register」リンクが表示される
- [ ] ログイン時: ヘッダーに「ユーザー名」と「Logout」ボタンが表示される
- [ ] カート数が正しく表示される（既存機能の互換性確認）

### 9-6. API連携
- [ ] ログイン後、API呼び出しに`Authorization: Bearer <token>`ヘッダーが自動付与される
- [ ] 401エラー発生時、localStorageがクリアされる
- [ ] 401エラー発生後、未ログイン状態になる

### 9-7. 既存導線維持
- [ ] 未ログイン時でも商品一覧を閲覧できる
- [ ] 未ログイン時でもカートに商品を追加できる
- [ ] 未ログイン時でも注文を完了できる

---

## 10. 今後の拡張（対象外）

Task3以降で対応:
- **カート引き継ぎ**: ゲストカート → 会員カートへの統合（Task3）
- **会員注文履歴**: ログインユーザーの注文一覧表示（Task4）
- **ロール認可**: 管理者ロールの制御（Task5）

本タスク（Task2）では認証基盤とUIのみを構築し、会員専用機能は実装しない。
