import { loopNode } from '@/workflow/common/data'
import type { NodeCatalogDef } from '@/workflow/ai-generate/node-catalog'
import { createLoopStartNode, inferDataType } from '@/workflow/ai-generate/catalog-library'

export const catalog: NodeCatalogDef = {
  entry: {
    type: 'loop-node',
    label: '循环',
    summary: '循环执行子画布（数组遍历/无限/指定次数），子画布内节点用 parentLoopId 添加',
    inputs: [
      { key: 'loopType', label: '循环类型', type: 'string', enum: ['foreach', 'infinite', 'count'], required: true, default: 'foreach' },
      { key: 'loopVariable', label: '遍历数组', type: 'array', reference: true, required: 'loopType="foreach" 时', description: '要遍历的数组变量，如 [aiChatID,"toolCalls"]' },
      { key: 'loopCount', label: '循环次数', type: 'number', required: 'loopType="count" 时' },
      {
        key: 'loopVariables',
        label: '循环作用域变量',
        type: 'array',
        default: [],
        description:
          '元素 { name, label, dataType, defaultValue }。dataType 必填且必须与值实际类型一致："string"|"number"|"boolean"|"array"|"dict"；' +
          'defaultValue 为字符串形式（数组 "[]"、字典 "{}"）。示例：{"name":"context","label":"上下文","dataType":"array","defaultValue":"[]"}。' +
          '定义后成为本循环输出字段，循环体内用 variable-assign 对 ["循环ID",name] 赋值实现跨轮传递状态'
      },
      { key: 'children', label: '子画布', type: 'object', description: '系统管理：添加本节点时自动初始化（含入口 loop-start-node），不要手动传' }
    ],
    outputs: [
      { value: 'item', label: '当前项', type: 'any', description: 'foreach 时为数组当前元素' },
      { value: 'index', label: '当前索引', type: 'number' }
    ],
    notes: [
      '子画布：add_node/add_edge/update_node/delete_node 传 parentLoopId=本节点 id；第一条边必须从 "loop-start-node" 引出',
      '循环体内引用当前项 [本循环ID,"item"]；用 loop-break-node 跳出、loop-continue-node 跳过本轮',
      'loopVariables 定义的变量也是本循环的输出字段'
    ],
    template: loopNode,
    defaults: () => ({ children: { nodes: [createLoopStartNode()], edges: [] } }),
    normalizeNodeData: (nodeData) => {
      if (!nodeData.children || !Array.isArray(nodeData.children.nodes)) {
        nodeData.children = { nodes: [createLoopStartNode()], edges: [] }
      } else if (!nodeData.children.nodes.some((n: any) => n.type === 'loop-start-node')) {
        nodeData.children.nodes.unshift(createLoopStartNode())
      }
      if (!Array.isArray(nodeData.children.edges)) nodeData.children.edges = []
      if (!Array.isArray(nodeData.loopVariables)) nodeData.loopVariables = []
      for (const variable of nodeData.loopVariables) {
        if (!variable || typeof variable !== 'object') continue
        if (!variable.label) variable.label = variable.name
        if (variable.defaultValue != null && typeof variable.defaultValue !== 'string') {
          variable.defaultValue = JSON.stringify(variable.defaultValue)
        }
        if (!['string', 'number', 'boolean', 'array', 'dict'].includes(variable.dataType)) {
          variable.dataType = inferDataType(variable.defaultValue)
        }
      }
      return nodeData
    },
    applyFieldList: (properties) => {
      const loopVariables = properties.nodeData?.loopVariables ?? []
      const customFields = loopVariables
        .filter((v: any) => v.name && String(v.name).trim())
        .map((v: any) => ({ label: v.label || v.name, value: v.name }))
      properties.field_list = [
        { label: '当前项', value: 'item' },
        { label: '当前索引', value: 'index' },
        ...customFields
      ]
      properties.loopFieldList = customFields
    }
  }
}
