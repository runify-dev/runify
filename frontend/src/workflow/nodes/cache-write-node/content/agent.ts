import { cloneDeep } from 'lodash'
import { resolveNode, useAgentConfig, deriveFieldList } from '@/workflow/ai-generate/common'
import { getDatabasePoolsTool, getNodeFieldOptionsTool, getTemplateVariablesTool } from '@/workflow/ai-generate/tools'
import type { AgentBindings, OutputDef, NodeAgentModule, NodeInputSchema } from '@/workflow/ai-generate/type'
import { cacheWriteNode } from '@/workflow/common/data'
import { validate as validateNode } from './validator'

/**
 * cache-write-node（缓存写入）的 AI 生成配置（命令式，手写）。仅处理器主画布。
 * nodeData：cacheId + 三元组 key / value + ttl(过期秒数,可选)。
 */

const BASE = cacheWriteNode.properties.nodeData
const outputs: OutputDef[] = [
  { value: 'success', label: '成功', type: 'boolean' }
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
              key: { description: `缓存键。${INPUT_HINT}` },
              value: { description: `缓存值。${INPUT_HINT}` },
              ttl: { type: 'number', description: '过期秒数（可选，留空不过期）' }
            }
          }

function useAgent(bindings: AgentBindings) {
  const node = resolveNode(bindings.lf, bindings.position?.[bindings.position.length - 1] ?? '')
  return useAgentConfig(
    {
      description: '按键写入缓存值，可设过期时间（仅处理器主画布）',
      prompt:
        'cache-write-node（缓存写入）：用 update 配置。\n' +
        '- cacheId 必填：缓存连接资源 id，先 get_database_pools 获取。\n' +
        `- key 必填：缓存键。value 必填：缓存值。${INPUT_HINT}。ttl 可选：过期秒数（留空不过期）。\n` +
        '- 输出字段：success 是否成功。',
      tools: [
        {
          name: 'update',
          description: '配置当前缓存写入节点：缓存连接、键、值、可选过期秒数。只传要设置的字段。',
          parameters: input,
          apply: (args) => {
            const next = { ...cloneDeep(BASE), ...(node.properties.nodeData ?? {}) }
            if (args.cacheId !== undefined) next.cacheId = args.cacheId
            if (args.ttl !== undefined) next.ttl = args.ttl
            for (const key of ['key', 'value']) {
              if (args[key] !== undefined) setInput(next, key, args[key])
            }
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
  type: 'cache-write-node',
  useAgent,
  input,
  outputs
} satisfies NodeAgentModule
