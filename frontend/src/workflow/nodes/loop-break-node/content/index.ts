import type { NodeInitContext } from '@/workflow/common/type'

export function init(ctx: NodeInitContext) {
  // 循环跳出节点没有输出字段
  if (!ctx.model.properties.field_list) {
    ctx.model.properties.field_list = []
  }
}
