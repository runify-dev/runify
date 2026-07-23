import { useAgentLoop } from '@/workflow/ai-generate/useAgentLoop'
import { buildProjectPrompt } from './prompt'
import { projectToolSchemas, projectToolExecutors, type ProjectAgentContext } from './tools'

/**
 * 项目级生成 agent = 通用 agent 循环（useAgentLoop）+ 项目运行时。
 * 不操作画布：工具全是纯 API 调用，工作流搭建经 generate_workflow 交给子代理。
 * 用户消息自动附带处理器清单（对标画布 agent 的画布快照），多轮续聊不依赖模型自觉查现状。
 */
export function useProjectAgent(ctx: ProjectAgentContext) {
  async function withProcessorSnapshot(content: string): Promise<string> {
    const processors = await projectToolExecutors.list_processors.execute({}, ctx)
    if (!Array.isArray(processors) || !processors.length) return content
    return (
      `${content}\n\n---\n` +
      `[系统附注] 当前项目处理器清单（自动附带）：\n` +
      JSON.stringify(processors)
    )
  }

  return useAgentLoop(
    {
      buildSystemPrompt: buildProjectPrompt,
      buildTools: () => projectToolSchemas,
      resolveTool: (name) => {
        const executor = projectToolExecutors[name]
        if (!executor) return undefined
        return {
          resultLimit: executor.resultLimit,
          execute: (args, meta) => executor.execute(args, ctx, meta)
        }
      },
      // 装饰失败由循环回落原文（如清单接口报错不阻塞发话）
      decorateUserMessage: withProcessorSnapshot
    },
    // 工具全是 API 调用与子代理调度，同一批次并发执行；
    // 并行 generate_workflow 的画布互不相同（页面按 processorId 分卡片），并发上限由页面信号量把关
    { parallelTools: true }
  )
}
