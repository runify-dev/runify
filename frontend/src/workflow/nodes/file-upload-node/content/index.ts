import type { NodeInitContext } from '@/workflow/common/type'

export function init(ctx: NodeInitContext) {
  ctx.model.properties.field_list = [
    { label: '文件ID', value: 'fileId' },
    { label: '文件名', value: 'fileName' },
    { label: '文件大小', value: 'fileSize' }
  ]
}
