import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  server: {
    host: '0.0.0.0',
    port: 50002,
    strictPort: true,
    proxy: {
      '/api': {
        target: 'http://localhost:60002',
        changeOrigin: true
      },
      '/uploads': {
        target: 'http://localhost:60002',
        changeOrigin: true
      }
    }
  }
})
