import type { NodeInitContext } from '@/workflow/common/type'

export function init(ctx: NodeInitContext) {
  // 条件判断节点没有输出字段
  if (!ctx.model.properties.field_list) {
    ctx.model.properties.field_list = []
  }
}
