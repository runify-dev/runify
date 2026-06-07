import type { NodeInitContext } from '@/workflow/common/type'

export function init(ctx: NodeInitContext) {
  ctx.model.properties.field_list = [
    { label: '技能列表', value: 'skills' },
    { label: '摘要', value: 'summary' },
    { label: '技能数', value: 'skills_count' }
  ]
}
