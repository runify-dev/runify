/**
 * 生成面板（对话框）的展示类型：活动树。
 * 把 createAgent 下发的扁平 AgentEvent 流（每条带 position）拼成一棵可展开的树的结构定义。
 * 业务/框架类型在 ../type，活动树的拼装逻辑在 ./index.ts。
 */

export interface TextEntry {
  type: 'text'
  kind: 'narration' | 'reasoning'
  text: string
}

export interface ToolEntry {
  type: 'tool'
  id: string
  name: string
  /** 入参 JSON 增量拼接（流式展示） */
  args: string
  /** 结果摘要（tool_end） */
  result: string
  ok: boolean
  running: boolean
  /** 若本工具是委派型（起了子 agent），其子作用域挂这里，成为可展开子树 */
  childScope?: Scope
  /** UI 展开态（默认折叠成一行，用户点开看输入/输出） */
  expanded: boolean
}

/** 匹配不到宿主工具卡时的兜底分组（一个子作用域直接作为一项展示） */
export interface GroupEntry {
  type: 'group'
  label: string
  childScope: Scope
  expanded: boolean
}

export type Entry = TextEntry | ToolEntry | GroupEntry

export interface Scope {
  /** position.join('/')，根作用域为 '' */
  key: string
  items: Entry[]
}
