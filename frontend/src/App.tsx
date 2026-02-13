import { BrowserRouter, Routes, Route } from 'react-router'
import { AuthProvider } from './contexts/AuthContext'
import { BoAuthProvider } from './contexts/BoAuthContext'
import { CartProvider } from './contexts/CartContext'
import { ProductProvider } from './contexts/ProductContext'
import AdminLayout from './components/AdminLayout'
import Layout from './components/Layout'
import HomePage from './pages/HomePage'
import ItemListPage from './pages/ItemListPage'
import ItemDetailPage from './pages/ItemDetailPage'
import CartPage from './pages/CartPage'
import OrderConfirmPage from './pages/OrderConfirmPage'
import OrderCompletePage from './pages/OrderCompletePage'
import OrderDetailPage from './pages/OrderDetailPage'
import OrderHistoryPage from './pages/OrderHistoryPage'
import AdminItemPage from './pages/AdminItemPage'
import AdminInventoryPage from './pages/AdminInventoryPage'
import AdminMembersPage from './pages/AdminMembersPage'
import AdminOrderPage from './pages/AdminOrderPage'
import BoLoginPage from './pages/BoLoginPage'
import LoginPage from './pages/LoginPage'
import RegisterPage from './pages/RegisterPage'

export default function App() {
  const appMode = import.meta.env.VITE_APP_MODE === 'admin' ? 'admin' : 'customer'

  if (appMode === 'admin') {
    return (
      <ProductProvider>
        <BoAuthProvider>
          <BrowserRouter>
            <Routes>
              <Route path="/" element={<BoLoginPage />} />
              <Route path="/bo/login" element={<BoLoginPage />} />

              {/* 管理画面（別レイアウト） */}
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

  return (
    <AuthProvider>
      <ProductProvider>
        <CartProvider>
          <BrowserRouter>
            <Routes>
              {/* 一般ユーザー画面 */}
              <Route element={<Layout />}>
                <Route path="/" element={<HomePage />} />
                <Route path="/item" element={<ItemListPage />} />
                <Route path="/item/:id" element={<ItemDetailPage />} />
                <Route path="/order/cart" element={<CartPage />} />
                <Route path="/order/reg" element={<OrderConfirmPage />} />
                <Route path="/order/complete" element={<OrderCompletePage />} />
                <Route path="/order/:id" element={<OrderDetailPage />} />
                <Route path="/order/history" element={<OrderHistoryPage />} />

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
