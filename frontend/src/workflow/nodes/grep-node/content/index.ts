import type { NodeInitContext } from '@/workflow/common/type'

export function init(ctx: NodeInitContext) {
  ctx.model.properties.field_list = [
    { label: '搜索结果', value: 'content' },
    { label: '摘要', value: 'summary' },
    { label: '匹配数', value: 'matches' },
    { label: '文件数', value: 'files' }
  ]
}
