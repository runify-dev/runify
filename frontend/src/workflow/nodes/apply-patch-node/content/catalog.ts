import { applyPatchNode } from '@/workflow/common/data'
import type { NodeCatalogDef } from '@/workflow/ai-generate/node-catalog'
import { triple, toolModeInputs, HIDDEN_TOOL_OUTPUT } from '@/workflow/ai-generate/catalog-library'

export const catalog: NodeCatalogDef = {
  entry: {
    type: 'apply-patch-node',
    label: '数据修补',
    summary: '对工作目录文件应用 unified diff 补丁',
    inputs: [
      ...toolModeInputs(),
      triple('path', '目标文件相对路径', 'string', { default: '' }),
      triple('patch', '补丁内容', 'string', { required: 'location="customize" 时', default: '' })
    ],
    outputs: [
      { value: 'result', label: '执行结果', type: 'string' },
      { value: 'stdout', label: '标准输出', type: 'string' },
      { value: 'stderr', label: '错误输出', type: 'string' },
      HIDDEN_TOOL_OUTPUT
    ],
    template: applyPatchNode
  }
}
