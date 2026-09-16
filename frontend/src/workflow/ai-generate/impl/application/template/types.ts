/**
 * 对话应用的「参考范式」——从创建应用时内置的模板（views/application/template）提炼的好流程配方。
 * 规划器（application 父 agent 的 plan 委派子 agent）用它决策执行蓝图：看 when 选型，取 recipe/blueprint 照搭。
 */

/** judge 的一个分支：分流条件（judge 本身的逻辑，尽量写字面，如 'finishReason == "tool_calls"'）+ 这条分支挂的下游子树 */
export interface BlueprintBranch {
  /** 分支命中条件（judge 的分流逻辑，非描述；空/else 表默认分支） */
  cond: string
  /** 这条分支往下的节点子树（可为空＝直接汇合主线/无额外节点） */
  children: BlueprintNode[]
}

/**
 * 结构化蓝图的一个节点（纯骨架，只表结构、不带描述）。
 * 循环节点（loop-node）用 children 装循环体；判断节点（judge-node）用 branches 表多输出，每分支挂一股下游。
 * 只表「有哪些节点、谁在谁的循环体/分支里、大致顺序」；任务意图不进节点——通用约定看 recipe，具体意图由用户需求经 configure_node 落地。
 */
export interface BlueprintNode {
  /** 节点类型（= add_node 的 type，list_nodes 里的值） */
  type: string
  /**
   * 任务语义配置意图（自然语言，单节点局部）：供确定性 builder 建完结构后逐节点 configure_node 配字段。
   * 模板骨架不填；plan 产出时按任务填（如 ai-chat 的身份/目标/产出、内层 loop 遍历哪个数组）。judge 分支条件用 branches.cond，无需 intent。
   */
  intent?: string
  /** 循环体（仅 loop-node）：每轮迭代要执行的子画布节点树 */
  children?: BlueprintNode[]
  /** 多输出分支（仅 judge-node）：每个分支的分流条件与其下游子树 */
  branches?: BlueprintBranch[]
}

export interface WorkflowTemplate {
  /** 范式名（唯一，作为 describeTemplate 的键与 listTemplates 的展示名） */
  name: string
  /** 决策指引：什么样的用户需求该选它（一句话，供 listTemplates 快速选型） */
  when: string
  /** 详细配方：节点形态 / 连接 / 关键配置要点 / 建议步骤（供 describeTemplate，规划器据此产出蓝图） */
  recipe: string
  /**
   * 结构化蓝图（可选）：节点树，镜像真实模板画布的结构——loop 用 children 装循环体、judge 用 branches 表多输出。
   * 有它时规划器优先照它的结构搭（层级/分支天然隔开，避免散文串层）；recipe 补充结构表达不了的语义约定。
   */
  blueprint?: BlueprintNode[]
}
