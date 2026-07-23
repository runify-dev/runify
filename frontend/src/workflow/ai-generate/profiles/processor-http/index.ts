import { WorkflowType } from '@/workflow/common/data'
import type { WorkflowAgentProfile } from '../types'
import { processorCatalog } from './catalog'
import { buildProcessorPrompt } from './prompt'
import { processorToolSchemas, processorToolExecutors, updateProcessorStartNode } from './tools'

/** 处理器 HTTP 画布档案（将来 ws 版新增 profiles/processor-ws/） */
export const processorHttpProfile: WorkflowAgentProfile = {
  workflowType: WorkflowType.PROCESSOR,
  catalog: processorCatalog,
  buildSystemPrompt: buildProcessorPrompt,
  tools: {
    schemas: processorToolSchemas,
    executors: processorToolExecutors
  },
  // 开始节点的 HTTP 入参允许 AI 配置（联动持久化到处理器配置）
  updateStartNode: updateProcessorStartNode
}
