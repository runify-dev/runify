import type { NodeInitContext } from '@/workflow/common/type'

export function init(ctx: NodeInitContext) {
  ctx.model.properties.field_list = [
    {
      label: '工具执行',
      value: 'tool'
    },
    { label: '文件列表', value: 'content' },
    { label: '摘要', value: 'summary' },
    { label: '文件数', value: 'files' }
  ]
}
