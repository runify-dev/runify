import { useAgentConfig } from '@/workflow/ai-generate/common'
import type { AgentBindings, NodeAgent, OutputDef, NodeAgentModule, NodeInputSchema } from '@/workflow/ai-generate/type'
import { WorkflowType } from '@/workflow/common/data'
import applicationAgent from './application/agent'
import processorAgent from './processor/agent'
import toolAgent from './tool/agent'

/**
 * response-node 是画布差异节点：与 content/{application,processor,tool} 的表单/校验器分裂一一对应。
 * useAgent 按 bindings.workflowType 选对应变体（与 content/validator.ts 的分派表一致），取变体的
 * AgentConfig 再装配成可运行 agent；configure_node 委派时把当前画布 workflowType 传进来。
 */

const agentMap: Record<string, NodeAgent> = {
  [WorkflowType.APPLICATION]: applicationAgent,
  [WorkflowType.APPLICATION_LOOP]: applicationAgent,
  [WorkflowType.PROCESSOR]: processorAgent,
  [WorkflowType.PROCESSOR_LOOP]: processorAgent,
  [WorkflowType.TOOL]: toolAgent,
  [WorkflowType.TOOL_LOOP]: toolAgent
}

/**
 * 入参/出参签名：response 是画布差异节点，入参随变体（application/processor/tool）而异、无单一 schema；
 * 它是终端输出节点，可引用出参以画布上实际 field_list 为准，这里不固定断言。
 */
const input: NodeInputSchema = { type: 'object', properties: {} }
const outputs: OutputDef[] = []

/** 本节点的 useAgent：按 workflowType 选变体（默认处理器），取其 config 再装配成可运行 agent */
function useAgent(bindings: AgentBindings) {
  const variant = agentMap[bindings.workflowType] ?? processorAgent
  return useAgentConfig(variant.useAgent(bindings), bindings)
}

export default {
  type: 'response-node',
  useAgent,
  input,
  outputs
} satisfies NodeAgentModule
