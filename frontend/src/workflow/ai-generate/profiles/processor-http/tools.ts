import { cloneDeep } from 'lodash'
import { reactive } from 'vue'
import { TreeCommonAPI } from '@/api/tree'
import databaseConnectionPoolAPI from '@/api/database-connection-pool'
import processorAPI from '@/api/processor'
import { ROOT_FOLDER_ID } from '@/constants/common'
import { catalog as startNodeCatalog } from '@/workflow/nodes/start-node/catalog'
import { validate as validateStartNode } from '@/workflow/nodes/start-node/content/processor/validator'
import { sanitizeNodeDataPatch, type NodeCatalogEntry } from '../../node-catalog'
import type { ToolExecution, WorkflowAgentContext } from '../../types'

/**
 * 处理器画布专属工具集：
 * - 数据库/缓存连接资源与库结构查询（供 SQL 类节点配置用）
 * - updateStartNode：开始节点 HTTP 入参修改钩子（经 profile.updateStartNode 挂接）
 */

export const processorToolExecutors: Record<string, ToolExecution> = {
  get_database_pools: {
    mutating: false,
    execute: async () => {
      const res = await new TreeCommonAPI('datasource').listResource(ROOT_FOLDER_ID)
      // provider 区分 SQL 库（mysql/postgresql，供 database-search/insert 的 poolId）与
      // 缓存（redis/local_cache，供 cache-query/write 的 cacheId）；kind 归一供模型判断
      return (res.data ?? []).map((item: any) => {
        const provider = item.provider ?? ''
        return {
          id: item.id,
          name: item.name,
          provider,
          kind: provider === 'redis' || provider === 'local_cache' ? 'cache' : 'sql'
        }
      })
    }
  },

  get_database_tables: {
    mutating: false,
    execute: async ({ poolId }) => {
      if (!poolId) throw new Error('poolId 不能为空')
      const res = await databaseConnectionPoolAPI.getTables(poolId)
      return res.data ?? []
    }
  },

  get_database_columns: {
    mutating: false,
    resultLimit: 20000,
    execute: async ({ poolId, table }) => {
      if (!poolId) throw new Error('poolId 不能为空')
      if (!table) throw new Error('table 不能为空')
      const res = await databaseConnectionPoolAPI.getColumns(poolId, table)
      return res.data ?? []
    }
  }
}

export const processorToolSchemas = [
  {
    type: 'function',
    function: {
      name: 'get_database_pools',
      description:
        '获取可用的数据库/缓存连接资源列表（仅当方案用到 database-search / database-insert / cache-query / cache-write 节点、需要填 poolId 或 cacheId 时调用）。' +
        '返回元素 { id, name, provider(mysql/postgresql/redis/local_cache), kind("sql"|"cache") }。' +
        'kind="sql" 的连接才用于 database-search/insert 的 poolId、才支持 get_database_tables/columns；' +
        'kind="cache" 的连接用于 cache-query/write 的 cacheId，没有表结构，不要对它查表。',
      parameters: { type: 'object', properties: {} }
    }
  },
  {
    type: 'function',
    function: {
      name: 'get_database_tables',
      description:
        '获取指定 SQL 数据库连接池中的数据表列表。编写 SQL 前必须先查表，禁止凭猜测写表名。' +
        '返回元素 { name 表名, engine 存储引擎/表类型, comment 表注释 }。仅对 kind="sql" 的连接有效。',
      parameters: {
        type: 'object',
        properties: {
          poolId: { type: 'string', description: 'SQL 数据库连接池 id（get_database_pools 中 kind="sql" 的项）' }
        },
        required: ['poolId']
      }
    }
  },
  {
    type: 'function',
    function: {
      name: 'get_database_columns',
      description:
        '获取指定数据表的字段（列）结构。编写涉及具体字段的 SQL 前先查列，禁止凭猜测写列名。' +
        '返回元素 { name 列名, type 数据类型, nullable 是否可空(boolean), primaryKey 是否主键(boolean), comment 列注释 }。',
      parameters: {
        type: 'object',
        properties: {
          poolId: { type: 'string', description: 'SQL 数据库连接池 id（kind="sql"）' },
          table: { type: 'string', description: '表名（get_database_tables 获取）' }
        },
        required: ['poolId', 'table']
      }
    }
  }
]

/**
 * 开始节点 HTTP 入参修改钩子（画布固有节点，不可添加/删除；目录条目见 nodes/start-node）。
 * 修改必须同步持久化到处理器配置（editProcessor({meta})，与开始节点设置面板保存一致）——
 * 部署路由读的是处理器实体的 meta，只改画布 nodeData 会导致部署路由与画布不一致。
 */
const startNodeEntry: NodeCatalogEntry = startNodeCatalog.entry

export async function updateProcessorStartNode(
  ctx: WorkflowAgentContext,
  nodeDataPatch: Record<string, any>
): Promise<any> {
  const details = ctx.getDetails?.()
  if (!details?.projectId || !details?.id) {
    throw new Error('缺少处理器信息，无法保存 HTTP 入参')
  }
  const lf = ctx.getLf()
  const model = lf?.graphModel?.getNodeModelById('start-node')
  if (!model) throw new Error('开始节点不存在')
  // reactive 包装：teleport 里节点组件读的是 reactive(model)，直接改 raw model 不触发视图更新
  const node = reactive(model)

  // 兼容 AI 把 get_node_detail 的 { protocol, meta } 结构原样回传：展开 meta 到顶层再走白名单
  let flatPatch = nodeDataPatch ?? {}
  if (flatPatch.meta && typeof flatPatch.meta === 'object') {
    const { meta: metaPatch, protocol: _protocol, ...rest } = flatPatch
    flatPatch = { ...metaPatch, ...rest }
  }
  const { patch, warnings } = sanitizeNodeDataPatch(
    { [startNodeEntry.type]: startNodeEntry },
    startNodeEntry.type,
    flatPatch
  )
  const currentMeta = cloneDeep(node.properties.nodeData?.meta ?? {})
  const meta = { contentType: 'application/json', parameters: [], requestBody: [], ...currentMeta, ...patch }

  // 立即用开始节点的真实校验器把关（method/path 格式、参数完整性、路径参数与 path 对应），
  // 不合法直接报错回喂，不落库
  const result = validateStartNode({ protocol: 'HTTP', meta })
  if (!result.valid) {
    throw new Error(
      'HTTP 入参校验未通过：' +
        Object.entries(result.errors)
          .map(([key, message]) => `${key}: ${message}`)
          .join('；')
    )
  }

  await processorAPI.editProcessor(details.projectId, details.id, { meta })
  node.properties.nodeData = { protocol: details.protocol ?? 'HTTP', meta }
  startNodeEntry.applyFieldList!(node.properties)

  return {
    nodeId: 'start-node',
    field_list: node.properties.field_list ?? [],
    ...(warnings.length ? { warnings } : {})
  }
}
