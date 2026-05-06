import type { NodeInitContext } from '@/workflow/common/type'

export function init(ctx: NodeInitContext) {
  ctx.model.properties.field_list = [
    {
      label: '回答结果',
      value: 'content'
    },
    {
      label: '思考过程',
      value: 'reasoningContent'
    },
    {
      label: '拒绝原因文本',
      value: 'refusal'
    },
    { label: '是否拒绝回答', value: 'isRefusal' },
    {
      label: '工具调用',
      value: 'toolCalls'
    },
    {
      label: '结束原因',
      value: 'finishReason'
    }
  ]
}
