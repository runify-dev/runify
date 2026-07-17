import agentTemplate from '@/views/application/template/agent/template.json'

/**
 * 标准工具函数名 → 对应的工具节点类型
 * 后端各工具节点在 tool_call 模式下按固定函数名/参数名解析 functionArguments，
 * 因此函数定义必须与内置 agent 模板完全一致，禁止改名或改参数结构
 */
export const AGENT_TOOL_NODE_MAP: Record<string, string> = {
  run_command: 'terminal-node',
  read_file: 'read-file-node',
  list_dir: 'list-dir-node',
  glob: 'glob-node',
  grep: 'grep-node',
  create_file: 'create-file-node',
  apply_patch: 'apply-patch-node',
  file_upload: 'file-upload-node',
  file_download: 'file-download-node',
  list_skills: 'list-skills-node',
  download_skills: 'download-skills-node',
  run_skill: 'run-skill-node'
}

/** 递归收集图（含循环子画布）中的所有节点 */
function collectNodes(nodes: any[], result: any[] = []): any[] {
  for (const node of nodes ?? []) {
    result.push(node)
    const children = node?.properties?.nodeData?.children
    if (children?.nodes?.length) {
      collectNodes(children.nodes, result)
    }
  }
  return result
}

/** 从内置 agent 模板提取标准工具函数定义（name → 完整 tool 定义） */
function extractCanonicalTools(): Record<string, any> {
  const result: Record<string, any> = {}
  const aiChat = collectNodes((agentTemplate as any).nodes).find(
    (node) => node.type === 'ai-chat-node' && node.properties?.nodeData?.tools?.tools?.length
  )
  for (const tool of aiChat?.properties?.nodeData?.tools?.tools ?? []) {
    const name = tool?.function?.name
    if (name) result[name] = tool
  }
  return result
}

/** 标准工具函数完整定义（name → {type,function:{name,description,parameters}}），提取自内置 agent 模板 */
export const AGENT_TOOL_DEFINITIONS: Record<string, any> = extractCanonicalTools()

export const AGENT_TOOL_NAMES = Object.keys(AGENT_TOOL_DEFINITIONS)
