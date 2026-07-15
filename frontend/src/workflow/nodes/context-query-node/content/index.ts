import type { NodeInitContext } from '@/workflow/common/type'

export function init(ctx: NodeInitContext) {
  ctx.model.properties.field_list = [
    { label: '历史上下文', value: 'history' },
    { label: '完整历史（含当前）', value: 'full' },
    { label: '摘要', value: 'summary' },
    { label: '便签', value: 'facts' },
    { label: '载入边界', value: 'historyUpto' }
  ]
}
