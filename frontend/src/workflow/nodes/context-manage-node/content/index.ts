import type { NodeInitContext } from '@/workflow/common/type'

export function init(ctx: NodeInitContext) {
  ctx.model.properties.field_list = [
    { label: '消息', value: 'messages' },
    { label: '摘要', value: 'summary' },
    { label: '统计', value: 'stats' }
  ]
}
