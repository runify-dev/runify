import { globNode } from '@/workflow/common/data'
import type { NodeCatalogDef } from '@/workflow/ai-generate/node-catalog'
import { triple, toolModeInputs, HIDDEN_TOOL_OUTPUT } from '@/workflow/ai-generate/catalog-library'

export const catalog: NodeCatalogDef = {
  entry: {
    type: 'glob-node',
    label: '文件搜索',
    summary: '按 glob 模式搜索工作目录中的文件名',
    inputs: [
      ...toolModeInputs(),
      triple('pattern', 'glob 模式', 'string', { required: 'location="customize" 时', default: '', description: '如 "**/*.md"' }),
      triple('path', '搜索起始目录', 'string', { default: '' }),
      triple('maxResults', '结果上限', 'number')
    ],
    outputs: [
      { value: 'content', label: '文件列表', type: 'string' },
      { value: 'summary', label: '摘要', type: 'string' },
      { value: 'files', label: '文件数', type: 'number' },
      HIDDEN_TOOL_OUTPUT
    ],
    template: globNode
  }
}
