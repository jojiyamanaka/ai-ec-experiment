import { BrowserRouter, Routes, Route } from 'react-router'
import { CartProvider } from './contexts/CartContext'
import Layout from './components/Layout'
import HomePage from './pages/HomePage'
import ItemListPage from './pages/ItemListPage'
import ItemDetailPage from './pages/ItemDetailPage'
import CartPage from './pages/CartPage'
import OrderConfirmPage from './pages/OrderConfirmPage'
import OrderCompletePage from './pages/OrderCompletePage'

export default function App() {
  return (
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
          </Route>
        </Routes>
      </BrowserRouter>
    </CartProvider>
  )
}
