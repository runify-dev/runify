import { WorkflowType } from '@/workflow/common/data'
import type { WorkflowAgentProfile } from '../types'
import { applicationCatalog } from './catalog'
import { buildApplicationPrompt } from './prompt'
import { applicationToolSchemas, applicationToolExecutors } from './tools'

/** 应用（对话）画布档案 */
export const applicationProfile: WorkflowAgentProfile = {
  workflowType: WorkflowType.APPLICATION,
  catalog: applicationCatalog,
  buildSystemPrompt: buildApplicationPrompt,
  tools: {
    schemas: applicationToolSchemas,
    executors: applicationToolExecutors
  }
}
