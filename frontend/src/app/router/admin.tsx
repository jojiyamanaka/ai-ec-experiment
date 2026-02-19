import { BrowserRouter, Routes, Route } from 'react-router'
import { BoAuthProvider } from '@features/bo-auth'
import { ProductProvider } from '@entities/product'
import { AdminLayout } from '@widgets/AdminLayout'
import AdminItemPage from '@pages/admin/AdminItemPage'
import AdminOrderPage from '@pages/admin/AdminOrderPage'
import AdminInventoryPage from '@pages/admin/AdminInventoryPage'
import AdminMembersPage from '@pages/admin/AdminMembersPage'
import BoLoginPage from '@pages/admin/BoLoginPage'

export function AdminRouter() {
  return (
    <ProductProvider>
      <BoAuthProvider>
        <BrowserRouter>
          <Routes>
            <Route path="/" element={<BoLoginPage />} />
            <Route path="/bo/login" element={<BoLoginPage />} />
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
