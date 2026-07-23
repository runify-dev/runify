import type { WorkflowAgentProfile } from './profiles/types'

/**
 * AI 生成引擎的核心类型（无逻辑）。
 * tools / canvas-ops / profiles 均从此单向引类型，避免模块间循环依赖。
 */

/**
 * agent 工具执行所需的画布上下文，由挂载方（workflow/index.vue）提供。
 * 画布差异（节点目录/提示词/专属工具）全部收敛在 profile 里，
 * 引擎代码不允许出现画布类型分支。
 */
export interface WorkflowAgentContext {
  getLf: () => any
  /** 画布档案（profiles/index.ts 按 WorkflowType 取得） */
  profile: WorkflowAgentProfile
  /** 画布宿主详情（挂载页 provide 的 getDetails：处理器/应用实体），profile 钩子做联动持久化时用 */
  getDetails?: () => any
  validateWorkflow: (options?: {
    silent?: boolean
    skipNodeIds?: string[]
  }) => Promise<{ valid: boolean; nodeId?: string; errors?: Record<string, string> }>
}

export interface ToolExecution {
  /** 是否改变了画布图结构/配置（用于批次后自动布局） */
  mutating: boolean
  /** 图变更后跳过 dagre 自动布局（如模板加载：坐标已人工排好） */
  skipLayout?: boolean
  /** 工具结果回喂模型的截断上限（默认见 useWorkflowAgent 的 TOOL_RESULT_LIMIT） */
  resultLimit?: number
  execute: (args: Record<string, any>, ctx: WorkflowAgentContext) => Promise<any> | any
}
