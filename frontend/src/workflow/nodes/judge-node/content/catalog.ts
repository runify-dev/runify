import { judgeNode } from '@/workflow/common/data'
import { randomId } from '@/utils/common'
import type { NodeCatalogDef } from '@/workflow/ai-generate/node-catalog'

export const catalog: NodeCatalogDef = {
  entry: {
    type: 'judge-node',
    label: '条件判断',
    summary: '按条件分支执行，每个分支有独立出口锚点（连线时用 sourceBranchId 指定分支）',
    inputs: [
      {
        key: 'branches',
        label: '分支',
        type: 'array',
        required: true,
        description:
          '必须至少一个 if 分支和一个 else 分支（else 必须保留且在最后）。元素 { type:"if"|"elseif"|"else", logic:"and"|"or", ' +
          'conditions:[{ variable:[节点ID,字段], compare, value }] }（else 不需要 logic/conditions；id 可省略自动生成）。' +
          'compare 可选值：eq/not_eq/ge/gt/le/lt/contain/not_contain/is_null/is_not_null/len_eq/len_ge/len_gt/len_le/len_lt/' +
          'is_true/is_not_true/start_with/end_with/regex/wildcard（is_null/is_not_null/is_true/is_not_true 不需要 value）'
      }
    ],
    outputs: [],
    notes: ['连线：add_edge 时用 sourceBranchId 指定从哪个分支引出（分支 id 通过 get_workflow 或 add_node 返回的 nodeData 查看）'],
    template: judgeNode,
    defaults: () => ({
      branches: [
        {
          id: randomId(),
          type: 'if',
          logic: 'and',
          conditions: [{ id: randomId(), variable: [], compare: 'eq', value: '' }]
        },
        { id: randomId(), type: 'else', logic: 'and', conditions: [] }
      ]
    }),
    normalizeNodeData: (nodeData) => {
      const branches = Array.isArray(nodeData.branches) ? nodeData.branches : []
      for (const branch of branches) {
        if (!branch.id) branch.id = randomId()
        if (branch.type !== 'else') {
          if (!branch.logic) branch.logic = 'and'
          if (!Array.isArray(branch.conditions)) branch.conditions = []
          for (const condition of branch.conditions) {
            if (!condition.id) condition.id = randomId()
            if (!Array.isArray(condition.variable)) condition.variable = []
            if (condition.value === undefined) condition.value = ''
          }
        }
      }
      if (branches.length > 0 && !branches.some((b: any) => b.type === 'else')) {
        branches.push({ id: randomId(), type: 'else', logic: 'and', conditions: [] })
      }
      return { ...nodeData, branches }
    }
  }
}
