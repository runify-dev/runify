import { knowledgeSearchNode } from '@/workflow/common/data'
import type { NodeCatalogDef } from '@/workflow/ai-generate/node-catalog'
import { triple, toolModeInputs, HIDDEN_TOOL_OUTPUT } from '@/workflow/ai-generate/catalog-library'

export const catalog: NodeCatalogDef = {
  entry: {
    type: 'knowledge-search-node',
    label: '知识检索',
    summary: '在指定知识库中做语义检索，返回命中片段列表',
    inputs: [
      { key: 'knowledgeIds', label: '知识库', type: 'array', required: true, description: '知识库 id 列表，先调用 get_knowledge_bases 获取' },
      ...toolModeInputs(),
      triple('keyword', '检索关键词', 'string', { required: 'location="customize" 时', default: '', description: '通常引用用户问题/入参变量' }),
      triple('pageNo', '页码', 'number', { default: 1 }),
      triple('pageSize', '每页条数', 'number', { default: 10 })
    ],
    outputs: [
      {
        value: 'hits',
        label: '结果列表',
        type: 'array',
        description: '元素 { id, knowledgeId, score 相关度分, content 片段内容, title 标题 }'
      },
      { value: 'total', label: '总数', type: 'number' },
      { value: 'topScore', label: '最高分', type: 'number' },
      HIDDEN_TOOL_OUTPUT
    ],
    template: knowledgeSearchNode
  }
}
