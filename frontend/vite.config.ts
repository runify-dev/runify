import { fileURLToPath, URL } from 'node:url'

import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
const envDir = './env'
import type { ProxyOptions } from 'vite'
import DefineOptions from 'unplugin-vue-define-options/vite'
export default defineConfig(({ mode }) => {
  const ENV = loadEnv(mode, envDir)
  const proxyConf: Record<string, string | ProxyOptions> = {}
  proxyConf['/api'] = {
    target: 'http://127.0.0.1:8080',
    changeOrigin: true,
    rewrite: (path) => path.replace(ENV.VITE_BASE_PATH, '/'),
    configure: (proxy, options) => {
      // 移除响应头中的 Content-Length
      proxy.on("proxyRes", (proxyRes, request, response) => {
        delete proxyRes.headers['content-length']
      });
    },
  }
  return {
    preflight: false,
    lintOnSave: false,
    base: ENV.VITE_BASE_PATH,
    envDir: envDir,
    plugins: [vue(), DefineOptions()],
    server: {
      cors: true,
      host: '0.0.0.0',
      port: Number(ENV.VITE_APP_PORT),
      strictPort: true,
      proxy: proxyConf
    },
    build: {
      outDir: 'dist/ui'
    },
    resolve: {
      alias: {
        '@': fileURLToPath(new URL('./src', import.meta.url))
      }
    }
  }
})
