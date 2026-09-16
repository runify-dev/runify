import { cloneDeep } from 'lodash'
import { resolveNode, useAgentConfig } from '@/workflow/ai-generate/common'
import { getDatabasePoolsTool, getNodeFieldOptionsTool, getTemplateVariablesTool } from '@/workflow/ai-generate/tools'
import type { AgentBindings, OutputDef, NodeAgentModule, NodeInputSchema } from '@/workflow/ai-generate/type'
import { currentUserNode } from '@/workflow/common/data'
import { validate as validateNode } from './validator'
import { applyFieldList } from './index'

/**
 * current-user-node（当前用户）的 AI 生成配置（命令式，手写）。仅处理器主画布。
 * nodeData：sessionCacheId + 凭证定位(credentialLocation/Field/Prefix) + keyPrefix + userIdField + roles/permissions 段。
 * field_list 随 roles/permissions.enabled 动态变化，复用 content/index.ts 的 applyFieldList。
 */

const BASE = currentUserNode.properties.nodeData as Record<string, any>

const SEG_DOC = '{enabled,source(inline|cache),field,cacheId,keyPrefix,valueField}；默认关闭不输出'

const outputs: OutputDef[] = [
  { value: 'authenticated', label: '是否已登录', type: 'boolean', description: '凭证缺失或会话缓存未命中为 false' },
  { value: 'user', label: '用户信息', type: 'any', description: '未登录为 null' },
  { value: 'roles', label: '角色', type: 'any', description: '仅角色段开启时输出；未查到为 null' },
  { value: 'permissions', label: '权限', type: 'any', description: '仅权限段开启时输出；未查到为 null' }
]
const input: NodeInputSchema = {
            type: 'object',
            properties: {
              sessionCacheId: {
                type: 'string',
                description: '会话缓存连接资源 id（get_database_pools 获取）'
              },
              credentialLocation: {
                type: 'string',
                enum: ['header', 'cookie', 'query'],
                description: '凭证位置'
              },
              credentialField: {
                type: 'string',
                description: '凭证字段名，如 Authorization / token / sessionId'
              },
              credentialPrefix: {
                type: 'string',
                description: '凭证前缀，取值后剥离，如 "Bearer "'
              },
              keyPrefix: { type: 'string', description: '会话缓存 key 前缀（须与登录侧一致）' },
              userIdField: { type: 'string', description: '用户标识字段（默认 id）' },
              roles: { type: 'object', description: `角色段 ${SEG_DOC}` },
              permissions: { type: 'object', description: `权限段 ${SEG_DOC}` }
            }
          }

function useAgent(bindings: AgentBindings) {
  const node = resolveNode(bindings.lf, bindings.position?.[bindings.position.length - 1] ?? '')
  return useAgentConfig(
    {
      description: '取请求凭证查会话缓存，输出当前用户（可选角色、权限）；仅处理器主画布',
      prompt:
        'current-user-node（当前用户）：用 update 配置。\n' +
        '- sessionCacheId 必填：会话缓存连接资源 id（get_database_pools 获取），缓存内容为 凭证->用户对象。\n' +
        '- credentialLocation(header|cookie|query)+credentialField 必填：从哪取凭证、字段名（如 Authorization）；credentialPrefix 可选剥离前缀（如 "Bearer "）。\n' +
        '- keyPrefix 会话缓存 key 前缀（须与登录侧一致）；userIdField 用户标识字段（默认 id）。\n' +
        '- roles/permissions 段默认关闭；开启才输出对应变量。\n' +
        '- 节点不拦截：未登录/无权限需下游 judge 判断后接响应节点返回 401/403。\n' +
        '- 输出字段：authenticated 是否已登录、user 用户信息、roles/permissions（对应段开启时）。',
      tools: [
        {
          name: 'update',
          description:
            '配置当前用户节点：会话缓存、凭证定位、用户标识、可选角色/权限段。只传要设置的字段。',
          parameters: input,
          apply: (args) => {
            const next = { ...cloneDeep(BASE), ...(node.properties.nodeData ?? {}) }
            for (const key of [
              'sessionCacheId',
              'credentialLocation',
              'credentialField',
              'credentialPrefix',
              'keyPrefix',
              'userIdField'
            ]) {
              if (args[key] !== undefined) next[key] = args[key]
            }
            for (const seg of ['roles', 'permissions']) {
              if (args[seg] !== undefined)
                next[seg] = { ...cloneDeep(BASE[seg]), ...cloneDeep(args[seg]) }
            }
            node.properties.nodeData = next
            // field_list 随 roles/permissions.enabled 动态生成
            applyFieldList(node.properties)
            const _vr = validateNode(node.properties?.nodeData ?? {})
            if (!_vr.valid) return { field_list: node.properties.field_list, errors: _vr.errors }
            return { status: 'stop', data: { field_list: node.properties.field_list } }
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
  type: 'current-user-node',
  useAgent,
  input,
  outputs
} satisfies NodeAgentModule
