import { terminalNode } from '@/workflow/common/data'
import type { NodeCatalogDef } from '@/workflow/ai-generate/node-catalog'
import { triple, toolModeInputs, HIDDEN_TOOL_OUTPUT } from '@/workflow/ai-generate/catalog-library'

export const catalog: NodeCatalogDef = {
  entry: {
    type: 'terminal-node',
    label: '终端执行',
    summary: '在会话工作目录执行 shell 命令，输出 stdout/stderr/退出码',
    inputs: [
      { key: 'runtime', label: '运行时', type: 'string', default: 'local', description: '保持 "local"' },
      ...toolModeInputs(),
      triple('code', 'shell 命令', 'string', { required: 'location="customize" 时', default: '' }),
      triple('timeout', '超时秒数', 'number', { description: '1-3600，可选' })
    ],
    outputs: [
      { value: 'result', label: '执行结果', type: 'string' },
      { value: 'stdout', label: '标准输出', type: 'string' },
      { value: 'stderr', label: '错误输出', type: 'string' },
      { value: 'exitCode', label: '退出码', type: 'number' },
      HIDDEN_TOOL_OUTPUT
    ],
    notes: ['文件路径均为会话工作目录内的相对路径'],
    template: terminalNode
  }
}
