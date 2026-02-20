import { createContext, useContext, useState, useEffect, useRef } from 'react'
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
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const restoreSequenceRef = useRef(0)

  useEffect(() => {
    const token = localStorage.getItem('bo_token')
    if (!token) {
      setLoading(false)
      return
    }

    let isCancelled = false
    const sequence = restoreSequenceRef.current + 1
    restoreSequenceRef.current = sequence

    setLoading(true)
    getBoUser()
      .then((response) => {
        if (isCancelled || sequence !== restoreSequenceRef.current) {
          return
        }

        if (response.success && response.data) {
          const currentToken = localStorage.getItem('bo_token')
          if (!currentToken || currentToken !== token) {
            return
          }
          setBoUser(response.data)
          window.dispatchEvent(new Event('bo-auth:authenticated'))
          return
        }
        localStorage.removeItem('bo_token')
        setBoUser(null)
      })
      .catch(() => {
        if (isCancelled || sequence !== restoreSequenceRef.current) {
          return
        }
        localStorage.removeItem('bo_token')
        setBoUser(null)
      })
      .finally(() => {
        if (isCancelled || sequence !== restoreSequenceRef.current) {
          return
        }
        setLoading(false)
      })

    return () => {
      isCancelled = true
    }
  }, [])

  useEffect(() => {
    const handleUnauthorized = () => {
      restoreSequenceRef.current += 1
      localStorage.removeItem('bo_token')
      setBoUser(null)
      setLoading(false)
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
        window.dispatchEvent(new Event('bo-auth:authenticated'))
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
      setError(null)
      setLoading(false)
      window.dispatchEvent(new Event('bo-auth:unauthorized'))
    }
  }

  const clearError = () => setError(null)

  return (
    <BoAuthContext.Provider value={{ boUser, loading, error, boLogin, boLogout, clearError }}>
      {children}
    </BoAuthContext.Provider>
  )
}

// eslint-disable-next-line react-refresh/only-export-components
export function useBoAuth() {
  const context = useContext(BoAuthContext)
  if (!context) {
    throw new Error('useBoAuth must be used within BoAuthProvider')
  }
  return context
}
