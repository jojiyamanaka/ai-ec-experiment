import { useState } from 'react'
import { useNavigate, Link } from 'react-router'
import { useAuth } from '@features/auth'

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
    } catch {
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
