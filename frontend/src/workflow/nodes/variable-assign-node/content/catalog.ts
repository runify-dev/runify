import { variableAssignNode } from '@/workflow/common/data'
import type { NodeCatalogDef } from '@/workflow/ai-generate/node-catalog'

export const catalog: NodeCatalogDef = {
  entry: {
    type: 'variable-assign-node',
    label: '变量赋值',
    summary: '给开始节点定义的全局变量或循环变量赋值（引用上游变量或常量）',
    inputs: [
      {
        key: 'variables',
        label: '赋值项',
        type: 'array',
        default: [],
        description:
          '元素 { variable: 目标变量路径(全局变量 ["global",名] 或循环变量 ["循环ID",名]), type:"reference"|"constant", ' +
          'reference:[节点ID,字段](type=reference 时), dataType:"string"|"array"|"dict"|"number"|"boolean"(type=constant 时), ' +
          'value: 常量值(array/dict 用 JSON 字符串) }'
      }
    ],
    outputs: [],
    template: variableAssignNode
  }
}
