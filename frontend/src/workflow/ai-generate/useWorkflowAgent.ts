import { useAgentLoop, type AgentLoopOptions } from './useAgentLoop'
import { resolveToolExecutor, buildActiveToolSchemas } from './tools'
import { flushLoopRefresh, buildWorkflowSnapshot, hasCanvasContent } from './canvas-ops'
import type { WorkflowAgentContext } from './types'

export type { AgentStatus, LogItem, PlanItem, PendingAsk } from './useAgentLoop'

/**
 * 用户消息自动附带画布结构快照：修改场景不能依赖模型自觉调 get_workflow，
 * 每次用户发话都强制给它最新拓扑（节点/连线/分支/循环），完整配置由它按需 get_node_detail
 */
function withCanvasSnapshot(ctx: WorkflowAgentContext, content: string): string {
  const lf = ctx.getLf()
  if (!hasCanvasContent(lf)) return content
  const snapshot = buildWorkflowSnapshot(lf)
  if (!snapshot) return content
  return (
    `${content}\n\n---\n` +
    `[系统附注] 当前画布结构快照（自动附带，仅含拓扑与输出字段；节点完整配置用 get_node_detail 查询）：\n` +
    snapshot
  )
}

/**
 * 工作流生成 agent = 通用 agent 循环（useAgentLoop）+ 画布运行时。
 * 画布相关的四个注入点：每轮按画布状态下发工具、用户消息附带画布快照、
 * 工具批次后合并刷新循环子画布、画布变更后自动布局。
 * options.autonomous 供子 agent 场景（被程序驱动、无人值守）使用。
 */
export function useWorkflowAgent(
  ctx: WorkflowAgentContext,
  relayout: () => void,
  options?: AgentLoopOptions
) {
  return useAgentLoop(
    {
      buildSystemPrompt: () => ctx.profile.buildSystemPrompt(),
      buildTools: () => buildActiveToolSchemas(ctx.getLf(), ctx.profile),
      resolveTool: (name) => {
        const executor = resolveToolExecutor(name, ctx.profile)
        if (!executor) return undefined
        return {
          mutating: executor.mutating,
          skipLayout: executor.skipLayout,
          resultLimit: executor.resultLimit,
          execute: (args) => executor.execute(args, ctx)
        }
      },
      decorateUserMessage: (content) => withCanvasSnapshot(ctx, content),
      afterToolBatch: () => flushLoopRefresh(ctx.getLf()),
      onMutatedRound: relayout
    },
    options
  )
}
