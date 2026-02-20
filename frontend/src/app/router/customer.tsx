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
import MyPagePage from '@pages/customer/MyPagePage'
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
                <Route path="/mypage" element={<MyPagePage />} />
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
