import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      '/api/gateway': {
        target: 'http://localhost:8080',
        rewrite: (path) => path.replace(/^\/api\/gateway/, ''),
        changeOrigin: true,
      },
      '/api/ingestion': {
        target: 'http://localhost:8081',
        rewrite: (path) => path.replace(/^\/api\/ingestion/, ''),
        changeOrigin: true,
      },
      '/api/analytics': {
        target: 'http://localhost:8082',
        rewrite: (path) => path.replace(/^\/api\/analytics/, ''),
        changeOrigin: true,
      },
      '/api/fraud': {
        target: 'http://localhost:8083',
        rewrite: (path) => path.replace(/^\/api\/fraud/, ''),
        changeOrigin: true,
      },
      '/api/insight': {
        target: 'http://localhost:8084',
        rewrite: (path) => path.replace(/^\/api\/insight/, ''),
        changeOrigin: true,
      },
    }
  }
})
