import { CustomerRouter } from './router/customer'
import { AdminRouter } from './router/admin'

export default function App() {
  const appMode = import.meta.env.VITE_APP_MODE === 'admin' ? 'admin' : 'customer'
  return appMode === 'admin' ? <AdminRouter /> : <CustomerRouter />
}
