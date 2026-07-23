import { startNode, WorkflowType } from '@/workflow/common/data'
import type { NodeCatalogDef } from '@/workflow/ai-generate/node-catalog'
import { computeHttpFieldList } from './http'

/**
 * 处理器开始节点条目：HTTP 入参可由 AI 经 update_node 配置
 * （联动持久化见 profiles/processor/start-node.ts 的 updateStartNode 钩子）
 */
export const catalog: NodeCatalogDef = {
  workflowTypes: [WorkflowType.PROCESSOR],
  entry: {
    type: 'start-node',
    label: '开始节点（HTTP 入参）',
    summary: 'HTTP 接口入口：请求方式/请求路径/入参配置（画布固有节点，仅可 update_node 修改）',
    addable: false,
    inputs: [
      { key: 'method', label: '请求方式', type: 'string', enum: ['GET', 'POST', 'PUT', 'DELETE'], required: true },
      {
        key: 'path',
        label: '请求路径',
        type: 'string',
        required: true,
        description: '以 / 开头；路径参数用 :参数名 占位，如 /users/:id'
      },
      {
        key: 'contentType',
        label: '请求体类型',
        type: 'string',
        enum: ['application/json', 'multipart/form-data'],
        default: 'application/json'
      },
      {
        key: 'parameters',
        label: '入参列表',
        type: 'array',
        default: [],
        description:
          '查询/路径参数。元素 { field 参数名, description 描述, location:"query"|"path", ' +
          'type:"string"|"integer"|"uuid"|"long"|"double", required: boolean, many: boolean(同名多值查询参数) }；' +
          'location=path 的参数名必须与 path 中的 :参数名 一一对应'
      },
      {
        key: 'requestBody',
        label: '请求体结构',
        type: 'array',
        default: [],
        description:
          'contentType=multipart/form-data 时的表单字段。元素 { field 字段名, description 描述, type:"file"|"string", required: boolean }；' +
          'application/json 时无需配置（请求体运行时整体输出为 body 字段）'
      }
    ],
    outputs: [],
    notes: [
      '本节点画布固有：不可 add_node / delete_node，仅可 update_node(nodeId="start-node") 修改',
      '修改会同步保存到处理器的 HTTP 入参配置（等同在开始节点设置面板保存），并立即按新配置刷新输出字段',
      '字段传顶层（method/path/contentType/parameters/requestBody），系统合并进现有配置；修改前先 get_node_detail 查看当前配置',
      '输出字段由配置决定：每个参数一个同名字段；JSON 请求体输出 body；form-data 输出各文件字段 + formAttributes'
    ],
    template: startNode,
    applyFieldList: (properties) => {
      properties.field_list = computeHttpFieldList(properties.nodeData?.meta)
    }
  }
}
