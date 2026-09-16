import type { WorkflowTemplate } from './types'
import { simpleChat } from './simple-chat'
import { rag } from './rag'
import { routing } from './routing'
import { pipeline } from './pipeline'
import { agent } from './agent'

export type { WorkflowTemplate, BlueprintNode, BlueprintBranch } from './types'

/** 对话应用可参考的范式配方（规划器决策执行蓝图时查阅） */
export const APPLICATION_TEMPLATES: WorkflowTemplate[] = [simpleChat, rag, routing, pipeline, agent]

/** 简易列表：名称 + 何时适用（供选型） */
export function listTemplates(): Array<{ name: string; when: string }> {
  return APPLICATION_TEMPLATES.map(({ name, when }) => ({ name, when }))
}

/** 按名取完整配方；未命中回可选项名提示 */
export function describeTemplate(name: string): WorkflowTemplate | { error: string; available: string[] } {
  const found = APPLICATION_TEMPLATES.find((t) => t.name === name)
  if (found) return found
  return { error: `未知范式: ${name}`, available: APPLICATION_TEMPLATES.map((t) => t.name) }
}
