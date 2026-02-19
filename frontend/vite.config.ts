import path from 'path'
import { defineConfig, loadEnv } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'

// https://vite.dev/config/
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  const appMode = env.VITE_APP_MODE || (mode === 'admin' ? 'admin' : 'customer')

  return {
    plugins: [react(), tailwindcss()],
    resolve: {
      alias: {
        '@app': path.resolve(__dirname, 'src/app'),
        '@pages': path.resolve(__dirname, 'src/pages'),
        '@widgets': path.resolve(__dirname, 'src/widgets'),
        '@features': path.resolve(__dirname, 'src/features'),
        '@entities': path.resolve(__dirname, 'src/entities'),
        '@shared': path.resolve(__dirname, 'src/shared'),
      },
    },
    server: {
      host: '0.0.0.0', // Docker コンテナ内で外部アクセスを許可
      port: appMode === 'admin' ? 5174 : 5173,
      watch: {
        usePolling: true, // Docker コンテナ内でのファイル監視に必要
      },
    },
  }
})
