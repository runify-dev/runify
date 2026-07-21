import { cacheWriteNode, WorkflowType } from '@/workflow/common/data'
import type { NodeCatalogDef } from '@/workflow/ai-generate/node-catalog'
import { triple } from '@/workflow/ai-generate/catalog-library'

/** 目前仅处理器目录提供（与 cache-query 配套；后端本身也支持对话工作流） */
export const catalog: NodeCatalogDef = {
  workflowTypes: [WorkflowType.PROCESSOR],
  entry: {
    type: 'cache-write-node',
    label: '缓存写入',
    summary: '按键写入缓存值，可设置过期时间',
    inputs: [
      { key: 'cacheId', label: '缓存连接', type: 'string', required: true, description: '缓存连接资源 id，先调用 get_database_pools 获取' },
      triple('key', '缓存键', 'string', { required: 'keyLocation="customize" 时', default: '' }),
      triple('value', '缓存值', 'string', { required: 'valueLocation="customize" 时', default: '' }),
      { key: 'ttl', label: '过期秒数', type: 'number', description: '可选，留空不过期' }
    ],
    outputs: [{ value: 'success', label: '成功', type: 'boolean' }],
    template: cacheWriteNode
  }
}
