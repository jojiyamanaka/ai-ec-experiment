import { Outlet } from 'react-router'
import AdminSidebar from './AdminSidebar'

export default function AdminLayout() {
  return (
    <div className="flex h-screen overflow-hidden">
      <AdminSidebar />
      <main className="flex-1 overflow-y-auto bg-gray-50">
        <Outlet />
      </main>
    </div>
  )
}
