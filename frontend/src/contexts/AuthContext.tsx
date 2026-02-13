import { createContext, useContext, useState, useEffect } from 'react'
import type { ReactNode } from 'react'
import type { User } from '../types/api'
import * as api from '../lib/api'

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
  const isAuthenticated = !!user && !!token
  const isAdmin = false

  return (
    <AuthContext.Provider
      value={{
        user,
        token,
        isAuthenticated,
        isAdmin,
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
