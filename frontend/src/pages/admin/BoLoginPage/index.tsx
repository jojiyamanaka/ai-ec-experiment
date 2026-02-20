import { useState } from 'react'
import { useLocation, useNavigate } from 'react-router'
import { useBoAuth } from '@features/bo-auth'

export default function BoLoginPage() {
  const navigate = useNavigate()
  const location = useLocation()
  const { boLogin, loading, error, clearError } = useBoAuth()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    clearError()

    try {
      await boLogin(email, password)
      const from = (location.state as { from?: string } | null)?.from
      navigate(from ?? '/bo/item', { replace: true }) // ログイン成功後に管理画面へ
    } catch {
      // エラーは BoAuthContext で管理されている
    }
  }

  return (
    <div className="mx-auto max-w-md px-6 py-12">
      <h1 className="mb-8 text-center font-serif text-3xl tracking-wider">
        Admin Login
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
    </div>
  )
}
