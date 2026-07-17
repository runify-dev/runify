// vitest 桩：vue-clipboard3 的 CJS 产物在 node 测试环境下无法加载，目录测试不涉及剪贴板
export default function useClipboard() {
  return { toClipboard: async () => undefined }
}
