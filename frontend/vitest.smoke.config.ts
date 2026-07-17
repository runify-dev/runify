import { defineConfig } from 'vitest/config'
import { fileURLToPath, URL } from 'node:url'

// AI 生成模块目录冒烟测试的临时配置（绕过 vitest.config.ts 的既有加载问题）
export default defineConfig({
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
      'vue-clipboard3': fileURLToPath(
        new URL('./src/workflow/ai-generate/__tests__/stubs/vue-clipboard3.ts', import.meta.url)
      )
    }
  },
  test: {
    environment: 'node',
    include: ['src/workflow/ai-generate/__tests__/**/*.spec.ts']
  }
})
