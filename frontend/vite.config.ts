import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react(), tailwindcss()],
  server: {
    host: '0.0.0.0', // Docker コンテナ内で外部アクセスを許可
    port: 5173,
    watch: {
      usePolling: true, // Windows ホストでのファイル監視に必要
    },
  },
})
