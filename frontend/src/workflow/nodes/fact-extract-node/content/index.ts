import type { NodeInitContext } from '@/workflow/common/type'

export function init(ctx: NodeInitContext) {
  ctx.model.properties.field_list = [
    { label: '便签', value: 'facts' },
    { label: '本轮抽取数', value: 'extracted' }
  ]
}
