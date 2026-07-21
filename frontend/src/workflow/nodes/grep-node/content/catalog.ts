import { grepNode } from '@/workflow/common/data'
import type { NodeCatalogDef } from '@/workflow/ai-generate/node-catalog'
import { triple, toolModeInputs, HIDDEN_TOOL_OUTPUT } from '@/workflow/ai-generate/catalog-library'

export const catalog: NodeCatalogDef = {
  entry: {
    type: 'grep-node',
    label: '内容搜索',
    summary: '按正则在工作目录文件内容中搜索',
    inputs: [
      ...toolModeInputs(),
      triple('pattern', '搜索正则', 'string', { required: 'location="customize" 时', default: '' }),
      triple('path', '搜索路径', 'string', { required: 'location="customize" 时', default: '' }),
      triple('filePattern', '文件名过滤', 'string', { default: '' }),
      triple('contextLines', '上下文行数', 'number'),
      triple('maxResults', '结果上限', 'number')
    ],
    outputs: [
      { value: 'content', label: '搜索结果', type: 'string', description: '格式 "文件:行号: 内容"' },
      { value: 'summary', label: '摘要', type: 'string' },
      { value: 'matches', label: '匹配数', type: 'number' },
      { value: 'files', label: '文件数', type: 'number' },
      HIDDEN_TOOL_OUTPUT
    ],
    template: grepNode
  }
}
