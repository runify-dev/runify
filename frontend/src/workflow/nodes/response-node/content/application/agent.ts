import { cloneDeep } from 'lodash'
import { resolveNode } from '@/workflow/ai-generate/common'
import { getNodeFieldOptionsTool, getTemplateVariablesTool } from '@/workflow/ai-generate/tools'
import type { AgentBindings, AgentConfig, NodeAgent } from '@/workflow/ai-generate/type'
import { validate as validateNode } from './validator'

/**
 * response-node 应用画布版（数据响应）的 AI 生成配置（命令式，手写，自包含）。
 * 对应 content/application/index.vue：无状态码/响应头，仅响应体 + chunk 分块开关。
 */

/** 响应体归一：补齐 jsonFields 元素与 jsonObject/plainText 的 location/value 缺省，否则校验永远不通过 */
function normalizeBody(nodeData: Record<string, any>): Record<string, any> {
  if (Array.isArray(nodeData.jsonFields)) {
    for (const item of nodeData.jsonFields) {
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
    description: '向用户输出结构化/文本响应内容（引用上游变量或固定值）',
    prompt:
      'response-node（数据响应，应用画布）：用 update 配置。把 ai-chat 回答之外的结构化数据/文本响应给前端。\n' +
      '- contentType："jsonFields"（字段列表）|"jsonObject"（整个 JSON）|"plainText"（纯文本）。\n' +
      '- jsonFields：元素 { field 字段名, location:"reference"|"customize", reference:[节点ID,字段], value 固定值, required }。\n' +
      '- jsonObject/plainText：{ location:"reference"|"customize", value(JSON字符串/文本), reference:[节点ID,字段] }。\n' +
      '- chunk 分块输出：保持默认 false。',
    tools: [
      {
        name: 'update',
        description:
          '配置当前数据响应节点：响应体（结构化/文本）、可选分块开关。只传要设置的字段。',
        parameters: {
          type: 'object',
          properties: {
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
            },
            chunk: { type: 'boolean', description: '分块输出（保持默认 false）' }
          }
        },
        apply: (args) => {
          const next = { chunk: false, ...(node.properties.nodeData ?? {}) }
          for (const key of ['contentType', 'jsonFields', 'jsonObject', 'plainText', 'chunk']) {
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
