import type { NodeInitContext } from '@/workflow/common/type'

export function init(ctx: NodeInitContext) {
  ctx.model.properties.field_list = [
    {
      label: '工具执行',
      value: 'tool'
    },
    { label: '目录树', value: 'content' },
    { label: '摘要', value: 'summary' },
    { label: '文件数', value: 'files' },
    { label: '目录数', value: 'dirs' }
  ]
}
