import type { NodeInitContext } from '@/workflow/common/type'

export function init(ctx: NodeInitContext) {
  if (!ctx.model.properties.field_list || ctx.model.properties.field_list.length === 0) {
    ctx.model.properties.field_list = [{ label: '执行结果', value: 'result' }]
  }
}
