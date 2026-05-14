import type { NodeInitContext } from '@/workflow/common/type'

export function init(ctx: NodeInitContext) {
  // field_list 由用户添加的变量动态生成
  if (!ctx.model.properties.field_list) {
    ctx.model.properties.field_list = []
  }
}
