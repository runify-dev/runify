import { cloneDeep } from 'lodash'
import { resolveNode, useAgentConfig } from '@/workflow/ai-generate/common'
import { PROCESSOR_DB_TOOLS, getNodeFieldOptionsTool, getTemplateVariablesTool } from '@/workflow/ai-generate/tools'
import type { AgentBindings, OutputDef, NodeAgentModule, NodeInputSchema } from '@/workflow/ai-generate/type'
import { databaseSearchNode } from '@/workflow/common/data'
import { validate as validateNode } from './validator'

/**
 * database-search-node（数据库检索）的 AI 生成配置（命令式，手写）。仅处理器主画布。
 * nodeData：poolId + location(customize|reference) + reference(数组) + template(SQL) + parameters(数组)。
 */

const BASE = databaseSearchNode.properties.nodeData
const FIELD_LIST = [{ label: '查询结果', value: 'result' }]

const PARAM_DOC =
  'SQL 参数数组。元素 { field 参数名(对应 #{field} 占位符), location:"reference"|"customize", value, desc 可选 }。' +
  '★ 无独立 reference 字段：location="reference" 时 value 填引用路径数组 [节点ID,字段]、customize 时填常量值。'

const outputs: OutputDef[] = [
  { value: 'result', label: '查询结果', type: 'array', description: '行对象数组，元素 { 列名: 值 }' }
]
const input: NodeInputSchema = {
            type: 'object',
            properties: {
              poolId: { type: 'string', description: '连接池资源 id（get_database_pools 获取）' },
              location: {
                type: 'string',
                enum: ['customize', 'reference'],
                description: 'SQL 来源'
              },
              reference: {
                type: 'array',
                items: { type: 'string' },
                description: 'location="reference" 时：SQL 文本引用 [节点ID,字段]'
              },
              template: {
                type: 'string',
                description: 'location="customize" 时：SQL 模板，占位符 #{名}'
              },
              parameters: { type: 'array', description: PARAM_DOC }
            }
          }

function useAgent(bindings: AgentBindings) {
  const node = resolveNode(bindings.lf, bindings.position?.[bindings.position.length - 1] ?? '')
  return useAgentConfig(
    {
      description: '在数据库连接池上执行查询 SQL，返回行列表（仅处理器主画布，不能进循环子画布）',
      prompt:
        'database-search-node（数据库检索）：用 update 配置。\n' +
        '- poolId 必填：连接池资源 id，先 get_database_pools 获取；SQL 方言须匹配其 provider（mysql/postgresql）。\n' +
        '- location="customize" 用 template 里的 SQL 模板（占位符 #{名}，禁止拼接变量防注入）；="reference" 用 reference 引用完整 SQL 文本。\n' +
        '- parameters 为 #{名} 参数值数组。输出字段：result 行对象数组。',
      tools: [
        {
          name: 'update',
          description:
            '配置当前数据库检索节点：连接池、SQL 来源、SQL 模板、参数。只传要设置的字段。',
          parameters: input,
          apply: (args) => {
            const next = { ...cloneDeep(BASE), ...(node.properties.nodeData ?? {}) }
            for (const key of ['poolId', 'location', 'reference', 'template', 'parameters']) {
              if (args[key] !== undefined) next[key] = cloneDeep(args[key])
            }
            node.properties.nodeData = next
            node.properties.field_list = cloneDeep(FIELD_LIST)
            const _vr = validateNode(node.properties?.nodeData ?? {})
            if (!_vr.valid) return { field_list: FIELD_LIST, errors: _vr.errors }
            return { status: 'stop', data: { field_list: FIELD_LIST } }
          }
        },
        ...PROCESSOR_DB_TOOLS,
        getNodeFieldOptionsTool(bindings),
        getTemplateVariablesTool(bindings)
      ]
    },
    bindings
  )
}

export default {
  type: 'database-search-node',
  useAgent,
  input,
  outputs
} satisfies NodeAgentModule
