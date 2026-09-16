import { cloneDeep } from 'lodash'
import { resolveNode, useAgentConfig, deriveFieldList } from '@/workflow/ai-generate/common'
import { getDatabasePoolsTool, getNodeFieldOptionsTool, getTemplateVariablesTool } from '@/workflow/ai-generate/tools'
import type { AgentBindings, OutputDef, NodeAgentModule, NodeInputSchema } from '@/workflow/ai-generate/type'
import { cacheQueryNode } from '@/workflow/common/data'
import { validate as validateNode } from './validator'

/**
 * cache-query-node（缓存查询）的 AI 生成配置（命令式，手写）。仅处理器主画布。
 * nodeData：cacheId + 三元组 key。
 */

const BASE = cacheQueryNode.properties.nodeData
const outputs: OutputDef[] = [
  { value: 'result', label: '缓存值', type: 'any', description: '未命中为 null（judge 用 is_null 判断）' }
]
const FIELD_LIST = deriveFieldList(outputs)

function setInput(data: Record<string, any>, key: string, v: any): void {
  if (Array.isArray(v)) {
    data[`${key}Location`] = 'reference'
    data[`${key}Reference`] = cloneDeep(v)
  } else {
    data[`${key}Location`] = 'customize'
    data[key] = v
  }
}

const INPUT_HINT = '传字符串=字面值；传 [节点ID,字段] 数组=引用上游变量'

const input: NodeInputSchema = {
            type: 'object',
            properties: {
              cacheId: {
                type: 'string',
                description: '缓存连接资源 id（get_database_pools 获取）'
              },
              key: { description: `缓存键。${INPUT_HINT}` }
            }
          }

function useAgent(bindings: AgentBindings) {
  const node = resolveNode(bindings.lf, bindings.position?.[bindings.position.length - 1] ?? '')
  return useAgentConfig(
    {
      description: '按键读取缓存值（未命中为 null，仅处理器主画布）',
      prompt:
        'cache-query-node（缓存查询）：用 update 配置。\n' +
        '- cacheId 必填：缓存连接资源 id，先 get_database_pools 获取。\n' +
        `- key 必填：缓存键。${INPUT_HINT}。\n` +
        '- 输出字段：result 缓存值（未命中 null，可用 judge is_null 判断）。',
      tools: [
        {
          name: 'update',
          description: '配置当前缓存查询节点：缓存连接、缓存键。只传要设置的字段。',
          parameters: input,
          apply: (args) => {
            const next = { ...cloneDeep(BASE), ...(node.properties.nodeData ?? {}) }
            if (args.cacheId !== undefined) next.cacheId = args.cacheId
            if (args.key !== undefined) setInput(next, 'key', args.key)
            node.properties.nodeData = next
            node.properties.field_list = cloneDeep(FIELD_LIST)
            const _vr = validateNode(node.properties?.nodeData ?? {})
            if (!_vr.valid) return { field_list: FIELD_LIST, errors: _vr.errors }
            return { status: 'stop', data: { field_list: FIELD_LIST } }
          }
        },
        getDatabasePoolsTool,
        getNodeFieldOptionsTool(bindings),
        getTemplateVariablesTool(bindings)
      ]
    },
    bindings
  )
}

export default {
  type: 'cache-query-node',
  useAgent,
  input,
  outputs
} satisfies NodeAgentModule
