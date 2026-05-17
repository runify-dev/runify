import type { NodeInitContext } from '@/workflow/common/type'

export function init(ctx: NodeInitContext) {
  ctx.model.properties.field_list = [
    { label: '访问地址', value: 'url' },
    { label: '部署ID', value: 'deployId' }
  ]
}
