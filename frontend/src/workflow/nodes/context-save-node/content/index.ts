import type { NodeInitContext } from '@/workflow/common/type'

export function init(ctx: NodeInitContext) {
  ctx.model.properties.field_list = [
    { label: '摘要已写入', value: 'savedSummary' },
    { label: '便签写入数', value: 'savedFacts' }
  ]
}
