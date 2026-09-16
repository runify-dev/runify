import { cloneDeep } from 'lodash'
import { resolveNode } from '@/workflow/ai-generate/common'
import { getNodeFieldOptionsTool, getTemplateVariablesTool } from '@/workflow/ai-generate/tools'
import type { AgentConfig, AgentBindings, NodeAgent } from '@/workflow/ai-generate/type'
import { validate as validateNode } from './validator'

/**
 * response-node 工具画布版（数据响应）的 AI 生成配置（命令式，手写，自包含）。
 * 对应 content/tool/index.vue：可切「对话/处理器」两种模式——模式存 properties.toolResponseMode（不在 nodeData）。
 * 处理器模式才用 status/headers；对话模式只出响应体。
 */

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
      '工具向调用方输出响应内容；被处理器调用可出 HTTP 响应，被对话调用可出结构化/文本内容',
    prompt:
      'response-node（数据响应，工具画布）：用 update 配置。\n' +
      '- mode 响应模式："chat"（对话调用，只出响应体）|"processor"（处理器调用，可出 HTTP 响应）。\n' +
      '- status/headers 仅 processor 模式使用，非必填。\n' +
      '- contentType："jsonFields"（字段列表）|"jsonObject"（整个 JSON）|"plainText"（纯文本）。\n' +
      '- jsonFields：元素 { field 字段名, location:"reference"|"customize", reference:[节点ID,字段], value 固定值, required }。\n' +
      '- jsonObject/plainText：{ location:"reference"|"customize", value(JSON字符串/文本), reference:[节点ID,字段] }。',
    tools: [
      {
        name: 'update',
        description:
          '配置当前工具数据响应节点：响应模式、响应体、可选状态码/响应头。只传要设置的字段。',
        parameters: {
          type: 'object',
          properties: {
            mode: {
              type: 'string',
              enum: ['chat', 'processor'],
              description: '响应模式（存于节点，不在 nodeData）'
            },
            status: { type: 'number', description: '仅 processor 模式：HTTP 状态码' },
            headers: {
              type: 'array',
              description: '仅 processor 模式：响应头，元素 { field, location, value, reference }'
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
          // 模式单独存 properties.toolResponseMode，避免被子编辑器写 nodeData 覆盖
          if (args.mode !== undefined) node.properties.toolResponseMode = args.mode
          else if (!node.properties.toolResponseMode) node.properties.toolResponseMode = 'processor'
          const next = { ...(node.properties.nodeData ?? {}) }
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
          const mode = node.properties.toolResponseMode
          // 落库后自动校验：通过即 stop 收尾（无需 AI 再调 validate），不通过返回 errors 让其续修
          const result = validateNode(node.properties?.nodeData ?? {})
          if (!result.valid) return { ok: true, mode, errors: result.errors }
          return { status: 'stop', data: { ok: true, mode } }
        }
      },
      getNodeFieldOptionsTool(bindings),
      getTemplateVariablesTool(bindings)
    ]
  }
}

export default { type: 'response-node', useAgent } satisfies NodeAgent
