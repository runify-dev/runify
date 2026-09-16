import { cloneDeep } from 'lodash'
import { resolveNode } from '@/workflow/ai-generate/common'
import { getNodeFieldOptionsTool, getTemplateVariablesTool } from '@/workflow/ai-generate/tools'
import type { AgentBindings, AgentConfig, NodeAgent } from '@/workflow/ai-generate/type'
import { validate as validateProcessor } from './validator'
import { computeHttpFieldList } from './http'

/**
 * start-node 处理器画布版（HTTP 入参）的 AI 生成配置（命令式，手写，自包含）。
 * 画布固有节点（不可 add_node/delete_node），用 configure_node("start-node", 需求) 配置。
 * nodeData = { protocol:'HTTP', meta:{ method, path, contentType, parameters, requestBody } }；
 * 字段传顶层，update 合并进 meta，输出字段由 meta 计算（computeHttpFieldList）。
 * ★ 持久化：改动落到画布 nodeData.meta，由消费方（generateProcessorWorkflow）收尾时
 *   editProcessor({ meta }) 同步到处理器实体（部署路由读的是实体 meta）。
 */

const useAgent = (bindings: AgentBindings): AgentConfig => {
  const node = resolveNode(bindings.lf, bindings.position?.[bindings.position.length - 1] ?? '')
  return {
    description:
      'HTTP 接口入口：请求方式/请求路径/入参（固有节点，不可 add_node，用 configure_node 配置）',
    prompt:
      'start-node（HTTP 入参，处理器画布）：画布固有节点，用 configure_node("start-node", 需求) 配置，不可增删。\n' +
      '- method 必填：GET/POST/PUT/DELETE。path 必填：以 / 开头，路径参数用 :名 占位（如 /users/:id）。\n' +
      '- contentType："application/json"（默认，请求体整体输出为 body 字段）|"multipart/form-data"（表单，用 requestBody 定义文件/字段）。\n' +
      '- parameters 查询/路径参数：元素 { field 参数名, description 描述, location:"query"|"path", type:"string"|"integer"|"uuid"|"long"|"double", required, many }；location=path 的参数名必须与 path 的 :名 一一对应。\n' +
      '- requestBody 仅 multipart/form-data：元素 { field, description, type:"file"|"string", required }。\n' +
      '- 修改前先 get_node_detail 查看当前 { protocol, meta }。输出字段由配置决定：每个参数一个同名字段；JSON 请求体输出 body；form-data 输出各文件字段 + formAttributes。',
    tools: [
      {
        name: 'update',
        description:
          '配置开始节点的 HTTP 入参：请求方式/路径/请求体类型/参数/表单字段。字段传顶层，合并进现有配置。',
        parameters: {
          type: 'object',
          properties: {
            method: {
              type: 'string',
              enum: ['GET', 'POST', 'PUT', 'DELETE'],
              description: '请求方式'
            },
            path: { type: 'string', description: '请求路径，以 / 开头；路径参数用 :名 占位' },
            contentType: {
              type: 'string',
              enum: ['application/json', 'multipart/form-data'],
              description: '请求体类型'
            },
            parameters: {
              type: 'array',
              description:
                '查询/路径参数，元素 { field, description, location:"query"|"path", type, required, many }'
            },
            requestBody: {
              type: 'array',
              description:
                '仅 multipart/form-data：表单字段，元素 { field, description, type:"file"|"string", required }'
            }
          }
        },
        apply: (args) => {
          const currentMeta = cloneDeep(node.properties.nodeData?.meta ?? {})
          const meta: Record<string, any> = {
            contentType: 'application/json',
            parameters: [],
            requestBody: [],
            ...currentMeta
          }
          for (const key of ['method', 'path', 'contentType', 'parameters', 'requestBody']) {
            if (args[key] !== undefined) meta[key] = cloneDeep(args[key])
          }
          node.properties.nodeData = { protocol: 'HTTP', meta }
          const field_list = computeHttpFieldList(meta)
          node.properties.field_list = field_list
          // 落库后自动校验：通过即 stop 收尾（无需 AI 再调 validate），不通过返回 errors 让其续修
          const result = validateProcessor(node.properties.nodeData)
          if (!result.valid) return { field_list, errors: result.errors }
          return { status: 'stop', data: { field_list } }
        }
      },
      getNodeFieldOptionsTool(bindings),
      getTemplateVariablesTool(bindings)
    ]
  }
}

export default { type: 'start-node', useAgent } satisfies NodeAgent
