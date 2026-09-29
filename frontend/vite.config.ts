import vue from '@vitejs/plugin-vue'
import { defineConfig } from 'vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [vue()],
  server: {
    // Todo lo que empiece con /api se reenvía al backend Spring
    proxy: {
      '/api': 'http://localhost:8081',
    },
  },
})
