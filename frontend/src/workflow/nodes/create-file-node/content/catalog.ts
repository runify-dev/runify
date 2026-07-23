import { createFileNode } from '@/workflow/common/data'
import type { NodeCatalogDef } from '@/workflow/ai-generate/node-catalog'
import { triple, toolModeInputs, HIDDEN_TOOL_OUTPUT } from '@/workflow/ai-generate/catalog-library'

export const catalog: NodeCatalogDef = {
  entry: {
    type: 'create-file-node',
    label: '创建文件',
    summary: '在工作目录创建/写入文件',
    inputs: [
      ...toolModeInputs(),
      triple('path', '文件相对路径', 'string', { required: 'location="customize" 时', default: '' }),
      triple('content', '文件内容', 'string', { default: '' })
    ],
    outputs: [{ value: 'result', label: '执行结果', type: 'string' }, HIDDEN_TOOL_OUTPUT],
    template: createFileNode
  }
}
