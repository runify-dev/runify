import { useAgentConfig } from '@/workflow/ai-generate/common'
import type { AgentBindings, AgentConfig, NodeAgent, OutputDef, NodeAgentModule, NodeInputSchema } from '@/workflow/ai-generate/type'
import { WorkflowType } from '@/workflow/common/data'
import processorAgent from './processor/agent'

/**
 * start-node 是画布固有的入口节点（不可 add_node/delete_node），且是画布变体：
 * 仅处理器画布有可配置的 HTTP 入参；应用/工具画布的开始节点不可由 AI 配置。
 * useAgent 按 bindings.workflowType 选变体（与 content/validator.ts 的分派表一致），取变体的
 * AgentConfig 再装配成可运行 agent；configure_node 委派时把当前画布 workflowType 传进来。
 */

export const addable = false

/** 非处理器画布：开始节点不可配置——惰性变体（空 description 不进选型索引，update 空操作，validate 恒 valid） */
const inertAgent: NodeAgent = {
  type: 'start-node',
  useAgent: (): AgentConfig => ({
    description: '',
    prompt: '开始节点在本画布为固定入口，不可由 AI 配置。',
    tools: [
      {
        name: 'update',
        description: '开始节点在本画布不可配置（空操作，调用即完成）。',
        parameters: { type: 'object', properties: {} },
        // 不可配置：一调 update 即通过收尾（无需再校验）
        apply: () => ({ status: 'stop', data: { ok: true, note: '开始节点在本画布不可配置' } })
      }
    ]
  })
}

const agentMap: Record<string, NodeAgent> = {
  [WorkflowType.PROCESSOR]: processorAgent,
  [WorkflowType.PROCESSOR_LOOP]: processorAgent
}

/**
 * 入参/出参签名：start 是画布固有入口且画布差异节点，不可 add_node；入参随变体而异、无单一 schema；
 * 出参（question/user/messages 等）以画布上实际 field_list 为准，这里不固定断言。
 */
const input: NodeInputSchema = { type: 'object', properties: {} }
const outputs: OutputDef[] = []

/** 本节点的 useAgent：按 workflowType 选变体（默认惰性），取其 config 再装配成可运行 agent */
function useAgent(bindings: AgentBindings) {
  const variant = agentMap[bindings.workflowType] ?? inertAgent
  return useAgentConfig(variant.useAgent(bindings), bindings)
}

export default {
  type: 'start-node',
  useAgent,
  input,
  outputs
} satisfies NodeAgentModule
