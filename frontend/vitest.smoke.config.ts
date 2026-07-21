import { defineConfig } from 'vitest/config'
import { fileURLToPath, URL } from 'node:url'

// AI 生成模块目录冒烟测试的临时配置（绕过 vitest.config.ts 的既有加载问题）
export default defineConfig({
  resolve: {
    // 数组按序匹配：request 层 stub 必须在 '@' 前缀别名之前
    alias: [
      {
        find: /^@\/request\/admin(\/index)?$/,
        replacement: fileURLToPath(
          new URL('./src/workflow/ai-generate/__tests__/stubs/request-admin.ts', import.meta.url)
        )
      },
      {
        find: 'vue-clipboard3',
        replacement: fileURLToPath(
          new URL('./src/workflow/ai-generate/__tests__/stubs/vue-clipboard3.ts', import.meta.url)
        )
      },
      { find: '@', replacement: fileURLToPath(new URL('./src', import.meta.url)) }
    ]
  },
  test: {
    environment: 'node',
    include: ['src/workflow/ai-generate/__tests__/**/*.spec.ts']
  }
})
