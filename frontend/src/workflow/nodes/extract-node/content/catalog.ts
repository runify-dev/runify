import { extractNode } from '@/workflow/common/data'
import type { NodeCatalogDef } from '@/workflow/ai-generate/node-catalog'

export const catalog: NodeCatalogDef = {
  entry: {
    type: 'extract-node',
    label: '参数提取',
    summary: '用 JSONPath 从上游变量（JSON/对象）中提取字段，产出可被下游引用的新变量',
    inputs: [
      { key: 'sourceReference', label: '源变量', type: 'array', reference: true, required: true },
      {
        key: 'rules',
        label: '提取规则',
        type: 'array',
        required: true,
        description: '元素 { name 字段名(成为输出字段), description 说明, path JSONPath 如 "$.functionName" 或 "$[0].id" }'
      }
    ],
    outputs: [],
    notes: ['输出字段由 rules 动态生成：每条规则一个输出字段（value=rule.name，label=rule.description）'],
    template: extractNode,
    applyFieldList: (properties) => {
      const rules = properties.nodeData?.rules ?? []
      properties.field_list = rules
        .filter((r: any) => r.name && String(r.name).trim())
        .map((r: any) => ({ label: r.description || r.name, value: r.name }))
    }
  }
}
