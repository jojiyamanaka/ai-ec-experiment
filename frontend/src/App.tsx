import { BrowserRouter, Routes, Route } from 'react-router'
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
import AdminItemPage from './pages/AdminItemPage'
import AdminOrderPage from './pages/AdminOrderPage'

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
