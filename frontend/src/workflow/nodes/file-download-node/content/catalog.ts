import { fileDownloadNode } from '@/workflow/common/data'
import type { NodeCatalogDef } from '@/workflow/ai-generate/node-catalog'
import { triple, toolModeInputs, HIDDEN_TOOL_OUTPUT } from '@/workflow/ai-generate/catalog-library'

export const catalog: NodeCatalogDef = {
  entry: {
    type: 'file-download-node',
    label: '文件下载',
    summary: '按文件 id 下载文件到工作目录',
    inputs: [
      ...toolModeInputs(),
      triple('fileId', '文件 id', 'string', { required: 'location="customize" 时', default: '' }),
      triple('path', '保存路径', 'string', { default: '' })
    ],
    outputs: [
      { value: 'filePath', label: '本地路径', type: 'string' },
      { value: 'fileName', label: '文件名', type: 'string' },
      { value: 'fileSize', label: '文件大小', type: 'number' },
      HIDDEN_TOOL_OUTPUT
    ],
    template: fileDownloadNode
  }
}
