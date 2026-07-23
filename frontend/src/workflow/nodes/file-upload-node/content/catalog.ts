import { fileUploadNode } from '@/workflow/common/data'
import type { NodeCatalogDef } from '@/workflow/ai-generate/node-catalog'
import { triple, toolModeInputs, HIDDEN_TOOL_OUTPUT } from '@/workflow/ai-generate/catalog-library'

export const catalog: NodeCatalogDef = {
  entry: {
    type: 'file-upload-node',
    label: '文件上传',
    summary: '把工作目录文件上传到文件库，返回下载 URL（产出文件交付给用户的必经通道）',
    inputs: [...toolModeInputs(), triple('path', '文件相对路径', 'string', { required: 'location="customize" 时', default: '' })],
    outputs: [
      { value: 'url', label: '下载地址', type: 'string', description: './api/storage/file/{id}，必须原样使用' },
      { value: 'fileId', label: '文件 id', type: 'string' },
      { value: 'fileName', label: '文件名', type: 'string' },
      { value: 'fileSize', label: '文件大小', type: 'number' },
      HIDDEN_TOOL_OUTPUT
    ],
    notes: ['工作流产出的文件必须经本节点（或 file_upload 工具调用）上传后，用返回的 url 交付给用户下载'],
    template: fileUploadNode
  }
}
