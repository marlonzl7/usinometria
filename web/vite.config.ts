import react from '@vitejs/plugin-react'
import { VitePWA } from 'vite-plugin-pwa'
import { defineConfig } from 'vitest/config'

// Imita o Traefik de produção (topologia A): o front chama /api/... e o
// prefixo é removido antes de chegar à API, que expõe /jobs, /jobs/rate etc.
const apiProxy = {
  '/api': {
    target: 'http://localhost:8080',
    changeOrigin: true,
    rewrite: (path: string) => path.replace(/^\/api/, ''),
  },
}

export default defineConfig({
  plugins: [
    react(),
    VitePWA({
      registerType: 'autoUpdate',
      includeAssets: ['favicon.svg'],
      manifest: {
        name: 'Usinometria — Estimador de Tempo de Usinagem',
        short_name: 'Usinometria',
        lang: 'pt-BR',
        display: 'standalone',
        start_url: '/',
        background_color: '#f4f6f9',
        theme_color: '#2f6fed',
        icons: [
          { src: 'pwa-192.png', sizes: '192x192', type: 'image/png' },
          { src: 'pwa-512.png', sizes: '512x512', type: 'image/png' },
          { src: 'pwa-512.png', sizes: '512x512', type: 'image/png', purpose: 'maskable' },
        ],
      },
      workbox: {
        // Só o shell do app entra no cache. Respostas da API nunca são
        // cacheadas (sem runtimeCaching) e /api não cai no fallback do SPA.
        globPatterns: ['**/*.{js,css,html,svg,png,ico}'],
        navigateFallbackDenylist: [/^\/api/],
      },
    }),
  ],
  server: { proxy: apiProxy },
  preview: { proxy: apiProxy },
  test: {
    environment: 'node',
    include: ['src/**/*.test.ts'],
  },
})
