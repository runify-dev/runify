import type { NodeInitContext } from '@/workflow/common/type'
import { init as initChat } from './chat'
import { init as initProcessor } from './processor'
import { init as initTool } from './tool'
import { WorkflowType } from '@/workflow/common/data'

const initMap: Record<string, (ctx: NodeInitContext) => void> = {
  [WorkflowType.APPLICATION]: initChat,
  [WorkflowType.PROCESSOR]: initProcessor,
  [WorkflowType.TOOL]: initTool
}

export function init(ctx: NodeInitContext) {
  if (initMap[ctx.workflowType]) {
    initMap[ctx.workflowType](ctx)
  }
}
