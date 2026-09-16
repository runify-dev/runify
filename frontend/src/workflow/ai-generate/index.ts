import { WorkflowType } from '@/workflow/common/data'
import { useAgent as applicationUseAgent } from './impl/application'
import { useAgent as processorUseAgent } from './impl/processor'
import { useAgent as toolUseAgent } from './impl/tool'
import type { AgentBindings, RunnableAgent } from './type'

/**
 * ai-generate 入口：按 workflowType 分派到对应画布类型的父 agent（各自定义在 impl/application、impl/processor、impl/tool）。
 * 本文件只负责分派——不含任何 prompt / 建图逻辑；循环子画布沿用同类主画布父 agent。
 */
export interface WorkflowAgentModule {
  /** 按 bindings 装配该画布父 agent 的可运行 agent */
  useAgent: (bindings: AgentBindings) => RunnableAgent
}

const registry: Record<string, WorkflowAgentModule> = {
  [WorkflowType.APPLICATION]: { useAgent: applicationUseAgent },
  [WorkflowType.APPLICATION_LOOP]: { useAgent: applicationUseAgent },
  [WorkflowType.PROCESSOR]: { useAgent: processorUseAgent },
  [WorkflowType.PROCESSOR_LOOP]: { useAgent: processorUseAgent },
  [WorkflowType.TOOL]: { useAgent: toolUseAgent },
  [WorkflowType.TOOL_LOOP]: { useAgent: toolUseAgent }
}

/** 按 workflowType 取画布父 agent 模块（默认对话应用） */
export function profileFor(workflowType: string): WorkflowAgentModule {
  return registry[workflowType] ?? registry[WorkflowType.APPLICATION]
}
