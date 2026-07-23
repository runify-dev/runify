import { cacheQueryNode, WorkflowType } from '@/workflow/common/data'
import type { NodeCatalogDef } from '@/workflow/ai-generate/node-catalog'
import { triple } from '@/workflow/ai-generate/catalog-library'

/** 后端仅支持 PROCESSOR_HTTP（主画布，不能进循环子画布） */
export const catalog: NodeCatalogDef = {
  workflowTypes: [WorkflowType.PROCESSOR],
  entry: {
    type: 'cache-query-node',
    label: '缓存查询',
    summary: '按键读取缓存值',
    inputs: [
      { key: 'cacheId', label: '缓存连接', type: 'string', required: true, description: '缓存连接资源 id，先调用 get_database_pools 获取' },
      triple('key', '缓存键', 'string', { required: 'keyLocation="customize" 时', default: '' })
    ],
    outputs: [{ value: 'result', label: '缓存值', type: 'any', description: '未命中为 null（judge 用 is_null 判断）' }],
    notes: ['仅处理器主画布可用，不能放进循环子画布'],
    template: cacheQueryNode
  }
}
