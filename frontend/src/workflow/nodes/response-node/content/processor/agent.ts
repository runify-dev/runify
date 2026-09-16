import { cloneDeep } from 'lodash'
import { resolveNode } from '@/workflow/ai-generate/common'
import { getNodeFieldOptionsTool, getTemplateVariablesTool } from '@/workflow/ai-generate/tools'
import type { AgentBindings, AgentConfig, NodeAgent } from '@/workflow/ai-generate/type'
import { validate as validateNode } from './validator'

/**
 * response-node 处理器画布版（HTTP 响应）的 AI 生成配置（命令式，手写，自包含）。
 * 对应 content/processor/index.vue：status（必填）+ headers + 响应体。处理器工作流的收尾节点。
 */

const DEFAULT_HEADERS = [
  { field: 'content-type', value: 'application/json; charset=utf-8', location: 'customize' }
]

/** 响应体归一：补齐 jsonFields/headers 元素与 jsonObject/plainText 的 location/value 缺省，否则校验永远不通过 */
function normalizeBody(nodeData: Record<string, any>): Record<string, any> {
  for (const listKey of ['jsonFields', 'headers']) {
    if (!Array.isArray(nodeData[listKey])) continue
    for (const item of nodeData[listKey]) {
      if (!item || typeof item !== 'object') continue
      if (item.value === undefined) item.value = ''
      if (!item.location) item.location = 'customize'
      if (item.reference === undefined) item.reference = []
    }
  }
  for (const key of ['jsonObject', 'plainText']) {
    const obj = nodeData[key]
    if (obj && typeof obj === 'object') {
      if (!obj.location) obj.location = 'customize'
      if (obj.value === undefined) obj.value = ''
    }
  }
  return nodeData
}

const useAgent = (bindings: AgentBindings): AgentConfig => {
  const node = resolveNode(bindings.lf, bindings.position?.[bindings.position.length - 1] ?? '')
  return {
    description:
      '把处理结果作为 HTTP 响应返回给调用方（状态码/响应头/响应体），处理器工作流的收尾节点',
    prompt:
      'response-node（HTTP 响应，处理器画布）：用 update 配置。处理器工作流必须以本节点结尾，否则调用方收不到响应。\n' +
      '- status 必填：HTTP 状态码（默认 200）。\n' +
      '- headers 响应头：元素 { field 头名, location:"customize"|"reference", value 固定值, reference:[节点ID,字段], required }；默认已含 content-type: application/json，一般无需改。\n' +
      '- contentType："jsonFields"（字段列表）|"jsonObject"（整个 JSON）|"plainText"（纯文本）。\n' +
      '- jsonFields：元素 { field 字段名, location:"reference"|"customize", reference:[节点ID,字段], value 固定值, required }。\n' +
      '- jsonObject/plainText：{ location:"reference"|"customize", value(JSON字符串/文本), reference:[节点ID,字段] }。',
    tools: [
      {
        name: 'update',
        description: '配置当前 HTTP 响应节点：状态码、响应头、响应体。只传要设置的字段。',
        parameters: {
          type: 'object',
          properties: {
            status: { type: 'number', description: 'HTTP 状态码（默认 200）' },
            headers: {
              type: 'array',
              description:
                '响应头，元素 { field, location:"customize"|"reference", value, reference:[节点ID,字段], required }'
            },
            contentType: {
              type: 'string',
              enum: ['jsonFields', 'jsonObject', 'plainText'],
              description: '响应体类型'
            },
            jsonFields: {
              type: 'array',
              description:
                'contentType=jsonFields 时：元素 { field, location, reference:[节点ID,字段], value, required }'
            },
            jsonObject: {
              type: 'object',
              description: 'contentType=jsonObject 时：{ location, value(JSON字符串), reference }'
            },
            plainText: {
              type: 'object',
              description: 'contentType=plainText 时：{ location, value 文本, reference }'
            }
          }
        },
        apply: (args) => {
          const next = {
            status: 200,
            headers: cloneDeep(DEFAULT_HEADERS),
            ...(node.properties.nodeData ?? {})
          }
          for (const key of [
            'status',
            'headers',
            'contentType',
            'jsonFields',
            'jsonObject',
            'plainText'
          ]) {
            if (args[key] !== undefined) next[key] = cloneDeep(args[key])
          }
          node.properties.nodeData = normalizeBody(next)
          // 落库后自动校验：通过即 stop 收尾（无需 AI 再调 validate），不通过返回 errors 让其续修
          const result = validateNode(node.properties?.nodeData ?? {})
          if (!result.valid) return { ok: true, errors: result.errors }
          return { status: 'stop', data: { ok: true } }
        }
      },
      getNodeFieldOptionsTool(bindings),
      getTemplateVariablesTool(bindings)
    ]
  }
}

export default { type: 'response-node', useAgent } satisfies NodeAgent
