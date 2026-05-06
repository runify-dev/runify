import type { NodeInitContext } from '@/workflow/common/type'

export function init(ctx: NodeInitContext) {
  ctx.model.properties.field_list = [
    {
      label: '请求体',
      value: 'body'
    },
    {
      label: '请求头',
      value: 'headers'
    },
    {
      label: '查询参数',
      value: 'query'
    },
    {
      label: '路径参数',
      value: 'params'
    }
  ]
}
