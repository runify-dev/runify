import { contextPushNode } from '@/workflow/common/data'
import type { NodeCatalogDef } from '@/workflow/ai-generate/node-catalog'

export const catalog: NodeCatalogDef = {
  entry: {
    type: 'context-push-node',
    label: '推送上下文',
    summary: '把消息/工具结果追加进循环上下文变量（agent 循环中回写工具执行结果）',
    inputs: [
      {
        key: 'items',
        label: '推送项',
        type: 'array',
        default: [],
        description:
          '元素 { variable: 目标上下文变量 [外层循环ID,"context"], mode:"reference"|"custom", ' +
          'reference:[节点ID,字段](mode=reference 时；工具结果引用工具节点的隐藏字段 tool，如 [工具节点ID,"tool"]), ' +
          'content: JSON 字符串(mode=custom 时), role:"system"|"user"|"assistant"|"tool"(工具结果一般用 "user") }'
      }
    ],
    outputs: [],
    template: contextPushNode
  }
}
