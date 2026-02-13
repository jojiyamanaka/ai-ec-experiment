import { Link, Outlet } from 'react-router'
import { useCart } from '../contexts/CartContext'
import { useAuth } from '../contexts/AuthContext'

export default function Layout() {
  const { totalQuantity } = useCart()
  const { user, isAuthenticated, logout } = useAuth()

  return (
    <div className="flex min-h-screen flex-col bg-stone-50">
      <header className="fixed top-0 left-0 right-0 z-50 bg-white/80 backdrop-blur-md border-b border-stone-200">
        <div className="mx-auto flex max-w-7xl items-center justify-between px-6 h-20">
          {/* 左側ナビゲーション */}
          <nav className="hidden md:flex space-x-8 text-xs tracking-widest uppercase">
            <Link to="/item" className="hover:text-zinc-600 transition-colors">
              Collection
            </Link>
          </nav>

          {/* 中央ロゴ */}
          <Link to="/" className="font-serif text-2xl tracking-[0.2em] text-zinc-900">
            AI EC Shop
          </Link>

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
                <Link to="/order/history" className="hover:text-zinc-600 transition-colors">
                  Orders
                </Link>
                <span className="text-xs text-zinc-700">
                  {user?.displayName}
                </span>
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
