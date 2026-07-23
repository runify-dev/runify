import type { NodeCatalog } from '../node-catalog'
import type { ToolExecution, WorkflowAgentContext } from '../types'

/**
 * 画布档案（profile）：AI 生成对一种画布的全部适配收敛于此。
 * 引擎（useWorkflowAgent/tools）只面向本接口编程，不出现任何画布类型分支；
 * 新画布接入 = 新建 profiles/<画布>/ 目录组装一个 profile 并在 profiles/index.ts 注册。
 */
export interface WorkflowAgentProfile {
  /** 画布类型标识（WorkflowType 枚举值），同时用于节点校验等需要画布类型的场合 */
  workflowType: string
  /** 本画布可用节点目录（type → entry，画布内 type 唯一；如 response 双版本由各画布放各自变体） */
  catalog: NodeCatalog
  /** 完整系统提示词（prompt-base 公共骨架 + 本画布差异段拼装） */
  buildSystemPrompt(): string
  /** 画布专属工具（如处理器的数据库资源查询），引擎与通用工具合并下发；没有则不填 */
  tools?: {
    schemas: any[]
    executors: Record<string, ToolExecution>
  }
  /**
   * 开始节点修改钩子：提供即代表本画布的 start-node 允许 AI 经 update_node 修改，
   * 由钩子完成画布外的联动（如处理器把 HTTP 入参持久化到处理器配置）并返回工具结果；
   * 不提供则 update_node("start-node") 直接报错（如应用画布）。
   */
  updateStartNode?: (ctx: WorkflowAgentContext, nodeDataPatch: Record<string, any>) => Promise<any> | any
}
