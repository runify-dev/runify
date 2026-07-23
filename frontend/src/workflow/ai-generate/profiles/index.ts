import type { WorkflowAgentProfile } from './types'
import { applicationProfile } from './application'
import { processorHttpProfile } from './processor-http'

export type { WorkflowAgentProfile } from './types'

/**
 * 画布档案注册表。
 * 新画布接入 AI 生成：新建 profiles/<画布>/ 目录（四件套 index/catalog/prompt/tools），
 * 组装一个 WorkflowAgentProfile 后在这里注册一行即可，引擎与其他画布零改动。
 */
const profiles: Record<string, WorkflowAgentProfile> = {
  [applicationProfile.workflowType]: applicationProfile,
  [processorHttpProfile.workflowType]: processorHttpProfile
}

export function getProfile(workflowType: string): WorkflowAgentProfile {
  const profile = profiles[workflowType]
  if (!profile) {
    throw new Error(`该画布类型未接入 AI 生成: ${workflowType}`)
  }
  return profile
}

/** 已注册的全部画布档案（测试遍历用） */
export function getAllProfiles(): WorkflowAgentProfile[] {
  return Object.values(profiles)
}
