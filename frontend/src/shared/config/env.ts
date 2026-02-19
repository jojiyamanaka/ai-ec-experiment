export const APP_MODE = import.meta.env.VITE_APP_MODE === 'admin' ? 'admin' : 'customer'
export const API_BASE_URL =
  import.meta.env.VITE_API_URL ||
  (APP_MODE === 'admin' ? 'http://localhost:3002' : 'http://localhost:3001')
