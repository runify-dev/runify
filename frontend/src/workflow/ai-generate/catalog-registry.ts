import type { NodeCatalogDef, NodeCatalogEntry } from './node-catalog'
import { AGENT_TOOL_NODE_MAP } from './agent-tools'

/**
 * 节点目录注册中心：自动收集各节点目录下的 catalog.ts（与 validator.ts 的
 * glob 自动注册同一模式），画布 profile 用 collectCatalogEntries(workflowType)
 * 组装自己的目录。新增节点接入 AI 生成 = 在节点目录写一个 catalog.ts，零注册。
 */
const modules: Record<string, any> = import.meta.glob('../nodes/*/catalog.ts', { eager: true })

const allDefs: NodeCatalogDef[] = []
for (const path in modules) {
  const exported = modules[path]?.catalog
  if (!exported) continue
  allDefs.push(...(Array.isArray(exported) ? exported : [exported]))
}

// 给每个有标准函数的工具节点标注其 tool_call 函数名（与 ai-chat 标准 tools 定义绑定）
for (const [functionName, nodeType] of Object.entries(AGENT_TOOL_NODE_MAP)) {
  for (const def of allDefs) {
    if (def.entry.type !== nodeType) continue
    def.entry.notes = [
      ...(def.entry.notes ?? []),
      `tool_call 模式标准函数名：${functionName}。参数由后端按该函数的标准定义解析，在 ai-chat 的 tools 里必须按标准名引用，禁止改名或自编参数`
    ]
  }
}

/** 收集指定画布下可用的目录条目（缺省 workflowTypes 的条目所有画布可用） */
export function collectCatalogEntries(workflowType: string): NodeCatalogEntry[] {
  return allDefs
    .filter((def) => !def.workflowTypes || def.workflowTypes.includes(workflowType))
    .map((def) => def.entry)
}
