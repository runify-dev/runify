import type { NodeInitContext } from '@/workflow/common/type'
import { WorkflowType } from '@/workflow/common/data'

export function init(ctx: NodeInitContext) {
  if (ctx.workflowType === WorkflowType.APPLICATION) {
    ctx.model.properties.field_list = [
      {
        label: '响应体',
        value: 'body'
      }
    ]
  } else {
    ctx.model.properties.field_list = [
      {
        label: '结果',
        value: 'result'
      }
    ]
  }
}
