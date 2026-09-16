import {
  WorkflowType,
  aiChatNode,
  javaScriptNode,
  databaseSearchNode,
  databaseInsertNode,
  cacheQueryNode,
  cacheWriteNode,
  currentUserNode,
  fileDownloadNode,
  fileUploadNode,
  knowledgeSearchNode,
  jsonResponseNode,
  judgeNode,
  loopNode,
  loopContinueNode,
  loopBreakNode,
  terminalNode,
  variableAssignNode,
  contextPushNode,
  contextManageNode,
  contextQueryNode,
  contextSaveNode,
  factExtractNode,
  approvalNode,
  applyPatchNode,
  readFileNode,
  globNode,
  grepNode,
  listSkillsNode,
  downloadSkillsNode,
  runSkillNode,
  extractNode,
  listDirNode,
  staticFileNode,
  createFileNode
} from '@/workflow/common/data'

/**
 * 一个节点分组：菜单 Accordion 面板 = 一组同类节点。
 * nodes 直接复用 data.ts 里的节点模板对象（含 type / properties.name / 用于建节点的默认数据），
 * 故 type / 名称 / 图标全都单源，菜单渲染与 AI 目录读同一份。
 */
export interface MenuGroup {
  value: string
  label: string
  icon: string
  nodes: any[]
}

/**
 * 对话应用画布（APPLICATION）的节点分组。
 */
const application: MenuGroup[] = [
  {
    value: '0',
    label: 'AI',
    icon: 'pi pi-sparkles',
    nodes: [aiChatNode, contextManageNode, factExtractNode]
  },
  {
    value: '1',
    label: 'AI 工具',
    icon: 'pi pi-bolt',
    nodes: [
      terminalNode,
      readFileNode,
      listDirNode,
      globNode,
      grepNode,
      applyPatchNode,
      fileDownloadNode,
      fileUploadNode,
      createFileNode,
      listSkillsNode,
      downloadSkillsNode,
      runSkillNode
    ]
  },
  {
    value: '2',
    label: '数据',
    icon: 'pi pi-database',
    nodes: [databaseSearchNode, databaseInsertNode, cacheQueryNode, cacheWriteNode, knowledgeSearchNode]
  },
  {
    value: '3',
    label: '控制流',
    icon: 'pi pi-directions',
    nodes: [judgeNode, loopNode]
  },
  {
    value: '4',
    label: '工具',
    icon: 'pi pi-wrench',
    nodes: [
      javaScriptNode,
      extractNode,
      variableAssignNode,
      contextPushNode,
      contextQueryNode,
      contextSaveNode,
      approvalNode,
      jsonResponseNode
    ]
  }
]

/**
 * 处理器画布（PROCESSOR）的节点分组；工具画布（TOOL）复用之。
 * 与对话应用的差异：工具组额外含 currentUserNode / staticFileNode。
 */
const processor: MenuGroup[] = [
  {
    value: '0',
    label: 'AI',
    icon: 'pi pi-sparkles',
    nodes: [aiChatNode, contextManageNode, factExtractNode]
  },
  {
    value: '1',
    label: 'AI 工具',
    icon: 'pi pi-bolt',
    nodes: [
      terminalNode,
      readFileNode,
      listDirNode,
      globNode,
      grepNode,
      applyPatchNode,
      fileDownloadNode,
      fileUploadNode,
      createFileNode,
      listSkillsNode,
      downloadSkillsNode,
      runSkillNode
    ]
  },
  {
    value: '2',
    label: '数据',
    icon: 'pi pi-database',
    nodes: [databaseSearchNode, databaseInsertNode, cacheQueryNode, cacheWriteNode, knowledgeSearchNode]
  },
  {
    value: '3',
    label: '控制流',
    icon: 'pi pi-directions',
    nodes: [judgeNode, loopNode]
  },
  {
    value: '4',
    label: '工具',
    icon: 'pi pi-wrench',
    nodes: [
      javaScriptNode,
      extractNode,
      variableAssignNode,
      contextPushNode,
      contextQueryNode,
      contextSaveNode,
      currentUserNode,
      approvalNode,
      jsonResponseNode,
      staticFileNode
    ]
  }
]

/**
 * 对话应用循环子画布（APPLICATION_LOOP）的节点分组。
 * 与主画布差异：控制流额外含 loopContinueNode / loopBreakNode（循环体内继续/跳出）。
 */
const applicationLoop: MenuGroup[] = [
  {
    value: '0',
    label: 'AI',
    icon: 'pi pi-sparkles',
    nodes: [aiChatNode, contextManageNode, factExtractNode]
  },
  {
    value: '1',
    label: 'AI 工具',
    icon: 'pi pi-bolt',
    nodes: [
      terminalNode,
      readFileNode,
      listDirNode,
      globNode,
      grepNode,
      applyPatchNode,
      fileDownloadNode,
      fileUploadNode,
      createFileNode,
      listSkillsNode,
      downloadSkillsNode,
      runSkillNode
    ]
  },
  {
    value: '2',
    label: '数据',
    icon: 'pi pi-database',
    nodes: [databaseSearchNode, databaseInsertNode, cacheQueryNode, cacheWriteNode, knowledgeSearchNode]
  },
  {
    value: '3',
    label: '控制流',
    icon: 'pi pi-directions',
    nodes: [judgeNode, loopNode, loopContinueNode, loopBreakNode]
  },
  {
    value: '4',
    label: '工具',
    icon: 'pi pi-wrench',
    nodes: [
      javaScriptNode,
      extractNode,
      variableAssignNode,
      contextPushNode,
      contextQueryNode,
      contextSaveNode,
      approvalNode,
      jsonResponseNode
    ]
  }
]

/**
 * 处理器循环子画布（PROCESSOR_LOOP）的节点分组；工具循环子画布（TOOL_LOOP）复用之。
 * 与处理器主画布差异：控制流额外含 loopContinueNode / loopBreakNode，且无 judgeNode（按原菜单为准）。
 */
const processorLoop: MenuGroup[] = [
  {
    value: '0',
    label: 'AI',
    icon: 'pi pi-sparkles',
    nodes: [aiChatNode, contextManageNode, factExtractNode]
  },
  {
    value: '1',
    label: 'AI 工具',
    icon: 'pi pi-bolt',
    nodes: [
      terminalNode,
      readFileNode,
      listDirNode,
      globNode,
      grepNode,
      applyPatchNode,
      fileDownloadNode,
      fileUploadNode,
      createFileNode,
      listSkillsNode,
      downloadSkillsNode,
      runSkillNode
    ]
  },
  {
    value: '2',
    label: '数据',
    icon: 'pi pi-database',
    nodes: [databaseSearchNode, databaseInsertNode, cacheQueryNode, cacheWriteNode, knowledgeSearchNode]
  },
  {
    value: '3',
    label: '控制流',
    icon: 'pi pi-directions',
    nodes: [loopNode, loopContinueNode, loopBreakNode]
  },
  {
    value: '4',
    label: '工具',
    icon: 'pi pi-wrench',
    nodes: [
      javaScriptNode,
      extractNode,
      variableAssignNode,
      contextPushNode,
      contextQueryNode,
      contextSaveNode,
      approvalNode,
      jsonResponseNode,
      staticFileNode
    ]
  }
]

/**
 * 各工作流类型的节点分组：节点菜单（node-menu/impl/*）与 AI 生成的可用节点目录
 * （ai-generate/node-catalog）共用此单一数据源，保证两侧看到的节点集永远一致。
 * 工具画布沿用处理器节点集（与 node-menu/index.vue 的现状一致）。
 */
export const NODE_MENU_GROUPS: Record<string, MenuGroup[]> = {
  [WorkflowType.APPLICATION]: application,
  [WorkflowType.PROCESSOR]: processor,
  [WorkflowType.APPLICATION_LOOP]: applicationLoop,
  [WorkflowType.PROCESSOR_LOOP]: processorLoop,
  [WorkflowType.TOOL]: processor,
  [WorkflowType.TOOL_LOOP]: processorLoop
}
