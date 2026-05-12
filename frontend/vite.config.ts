import { fileURLToPath, URL } from 'node:url'
import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'path'
import fs from 'fs'
import { createHtmlPlugin } from 'vite-plugin-html'
import type { ProxyOptions } from 'vite'
import DefineOptions from 'unplugin-vue-define-options/vite'
import Components from 'unplugin-vue-components/vite';
import { PrimeVueResolver } from '@primevue/auto-import-resolver';
import tailwindcss from '@tailwindcss/vite';
const envDir = './env'
// 自定义插件：重命名入口文件
const renameHtmlPlugin = (outDir: string, entry: string) => {
  return {
    name: 'rename-html',
    closeBundle: () => {
      const buildDir = path.resolve(__dirname, outDir)
      const oldFile = path.join(buildDir, entry)
      const newFile = path.join(buildDir, 'index.html')

      // 检查文件是否存在
      if (fs.existsSync(oldFile)) {
        // 删除已存在的 index.html
        if (fs.existsSync(newFile)) {
          fs.unlinkSync(newFile)
        }
        // 重命名文件
        fs.renameSync(oldFile, newFile)
      }
    },
  }
}
export default defineConfig(({ mode }) => {
  const ENV = loadEnv(mode, envDir)
  const proxyConf: Record<string, string | ProxyOptions> = {}

  proxyConf['/admin/api'] = {
    target: 'http://127.0.0.1:8080',
    changeOrigin: true,
    configure: (proxy, options) => {
      proxy.on("proxyRes", (proxyRes, request, response) => {
        delete proxyRes.headers['content-length']
      });
    },
  }

  proxyConf['/conversation/api'] = {
    target: 'http://127.0.0.1:8080',
    changeOrigin: true,
    configure: (proxy, options) => {
      proxy.on("proxyRes", (proxyRes, request, response) => {
        delete proxyRes.headers['content-length']
      });
    },
  }

  proxyConf[`^${ENV.VITE_BASE_PATH}.+/storage/file/.*$`] = {
    target: `http://127.0.0.1:8080`,
    changeOrigin: true,
  }

  proxyConf[`^${ENV.VITE_BASE_PATH}storage/file/.*$`] = {
    target: `http://127.0.0.1:8080`,
    changeOrigin: true,
  }
  proxyConf['^/huhu/.*'] = {
    target: `http://127.0.0.1:8080`,
    changeOrigin: true,
  }
  // 前端静态资源转发到本身
  proxyConf[ENV.VITE_BASE_PATH] = {
    target: `http://127.0.0.1:${ENV.VITE_APP_PORT}`,
    changeOrigin: true,
    rewrite: (path: string) => path.replace(ENV.VITE_BASE_PATH, '/'),
  }
  return {
    preflight: false,
    lintOnSave: false,
    base: './',
    envDir: envDir,
    plugins: [vue(), DefineOptions(),
    createHtmlPlugin({ template: ENV.VITE_ENTRY }),
    tailwindcss(),
    Components({
      resolvers: [
        PrimeVueResolver()
      ]
    }),
    renameHtmlPlugin(`dist${ENV.VITE_BASE_PATH}`, ENV.VITE_ENTRY)],
    server: {
      cors: true,
      host: '0.0.0.0',
      port: Number(ENV.VITE_APP_PORT),
      strictPort: true,
      proxy: proxyConf
    },
    build: {
      outDir: `dist${ENV.VITE_BASE_PATH}`,
      rollupOptions: {
        input: ENV.VITE_ENTRY,
      },
    },
    resolve: {
      alias: {
        '@': fileURLToPath(new URL('./src', import.meta.url))
      }
    }
  }
})
