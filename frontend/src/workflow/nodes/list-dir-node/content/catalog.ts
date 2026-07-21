import { listDirNode } from '@/workflow/common/data'
import type { NodeCatalogDef } from '@/workflow/ai-generate/node-catalog'
import { triple, toolModeInputs, HIDDEN_TOOL_OUTPUT } from '@/workflow/ai-generate/catalog-library'

export const catalog: NodeCatalogDef = {
  entry: {
    type: 'list-dir-node',
    label: '目录列表',
    summary: '列出工作目录下的目录树',
    inputs: [
      ...toolModeInputs(),
      triple('path', '目录相对路径', 'string', { required: 'location="customize" 时', default: '', description: '"." 表示根目录' }),
      triple('depth', '遍历深度', 'number')
    ],
    outputs: [
      { value: 'content', label: '目录树', type: 'string' },
      { value: 'summary', label: '摘要', type: 'string' },
      { value: 'files', label: '文件数', type: 'number' },
      { value: 'dirs', label: '目录数', type: 'number' },
      HIDDEN_TOOL_OUTPUT
    ],
    template: listDirNode
  }
}
