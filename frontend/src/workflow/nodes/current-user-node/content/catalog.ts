import { currentUserNode, WorkflowType } from '@/workflow/common/data'
import type { NodeCatalogDef } from '@/workflow/ai-generate/node-catalog'
import { applyFieldList } from './index'

/** 后端仅支持 PROCESSOR_HTTP（主画布，不能进循环子画布） */
export const catalog: NodeCatalogDef = {
  workflowTypes: [WorkflowType.PROCESSOR],
  entry: {
    type: 'current-user-node',
    label: '当前用户',
    summary: '取请求凭证查会话缓存，输出当前用户（可选角色、权限）',
    inputs: [
      { key: 'sessionCacheId', label: '会话缓存连接', type: 'string', required: true, description: '缓存连接资源 id，先调用 get_database_pools 获取；缓存内容为 凭证 -> 用户对象' },
      { key: 'credentialLocation', label: '凭证位置', type: 'string', enum: ['header', 'cookie', 'query'], default: 'header' },
      { key: 'credentialField', label: '凭证字段名', type: 'string', required: true, description: '如 Authorization / token / sessionId' },
      { key: 'credentialPrefix', label: '凭证前缀', type: 'string', default: '', description: '取值后剥离，如 "Bearer "' },
      { key: 'keyPrefix', label: '会话缓存 Key 前缀', type: 'string', default: '', description: '需与登录侧写入缓存的规则一致' },
      { key: 'userIdField', label: '用户标识字段', type: 'string', default: 'id', description: '从用户对象取该字段作为角色/权限缓存的 key' },
      { key: 'roles', label: '角色段', type: 'object', default: { enabled: false }, description: '{enabled,source(inline|cache),field,cacheId,keyPrefix,valueField}；默认关闭不输出' },
      { key: 'permissions', label: '权限段', type: 'object', default: { enabled: false }, description: '结构同角色段；source=cache 时按用户标识查独立缓存，支持单独更新权限' }
    ],
    outputs: [
      { value: 'authenticated', label: '是否已登录', type: 'boolean', description: '凭证缺失或会话缓存未命中为 false' },
      { value: 'user', label: '用户信息', type: 'any', description: '未登录为 null' },
      { value: 'roles', label: '角色', type: 'any', description: '仅角色段开启时输出；未查到为 null' },
      { value: 'permissions', label: '权限', type: 'any', description: '仅权限段开启时输出；未查到为 null' }
    ],
    notes: [
      '仅处理器主画布可用，不能放进循环子画布',
      '节点不做拦截：未登录/无权限需在下游用 judge 判断后接响应节点返回 401/403',
      '角色段、权限段默认关闭；只有用户体系的简单系统无需开启',
      '会话缓存由登录侧写入（cache-write），cacheId 与 key 规则必须两侧一致'
    ],
    template: currentUserNode,
    applyFieldList
  }
}
