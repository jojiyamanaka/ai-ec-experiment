import { BrowserRouter, Routes, Route } from 'react-router'
import { AuthProvider } from './contexts/AuthContext'
import { CartProvider } from './contexts/CartContext'
import { ProductProvider } from './contexts/ProductContext'
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
import AdminOrderPage from './pages/AdminOrderPage'
import LoginPage from './pages/LoginPage'
import RegisterPage from './pages/RegisterPage'

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
                <Route path="/order/history" element={<OrderHistoryPage />} />
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
