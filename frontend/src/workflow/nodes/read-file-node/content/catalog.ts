import { readFileNode } from '@/workflow/common/data'
import type { NodeCatalogDef } from '@/workflow/ai-generate/node-catalog'
import { triple, toolModeInputs, HIDDEN_TOOL_OUTPUT } from '@/workflow/ai-generate/catalog-library'

export const catalog: NodeCatalogDef = {
  entry: {
    type: 'read-file-node',
    label: '读取文件',
    summary: '读取工作目录中的文件内容',
    inputs: [
      ...toolModeInputs(),
      triple('path', '文件相对路径', 'string', { required: 'location="customize" 时', default: '' }),
      triple('offset', '起始行', 'number'),
      triple('limit', '读取行数', 'number')
    ],
    outputs: [
      { value: 'content', label: '带行号内容', type: 'string' },
      { value: 'rawContent', label: '原始内容', type: 'string' },
      { value: 'totalLines', label: '总行数', type: 'number' },
      { value: 'lines', label: '读取行数', type: 'number' },
      HIDDEN_TOOL_OUTPUT
    ],
    template: readFileNode
  }
}
