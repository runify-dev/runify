import { javaScriptNode } from '@/workflow/common/data'
import type { NodeCatalogDef } from '@/workflow/ai-generate/node-catalog'
import { triple } from '@/workflow/ai-generate/catalog-library'

export const catalog: NodeCatalogDef = {
  entry: {
    type: 'java-script-node',
    label: 'JavaScript 执行',
    summary: '执行一段 JavaScript 代码做数据加工/计算，返回结果供下游引用',
    inputs: [
      { key: 'mode', label: '模式', type: 'string', enum: ['script', 'function'], default: 'script', description: '推荐 script：代码直接执行，最后一个表达式的值即结果' },
      triple('code', 'JavaScript 代码', 'string', { required: 'codeLocation="customize" 时', default: '' }),
      { key: 'functionName', label: '函数名', type: 'string', default: '', required: 'mode="function" 时' },
      { key: 'allowIO', label: '允许 IO', type: 'boolean', default: false, description: '是否允许 IO/进程访问' },
      {
        key: 'parameters',
        label: '注入参数',
        type: 'array',
        default: [],
        description:
          'script 模式注入为顶层变量。元素 { field 变量名, location:"reference"|"customize", value, type/description 可选 }。' +
          '★ 无独立 reference 字段：location="reference" 时 value 填引用路径数组 [节点ID,字段]；' +
          'location="customize" 时 value 填常量值。不要写 reference 字段。'
      }
    ],
    outputs: [{ value: 'result', label: '结果', type: 'any', description: '代码最后一个表达式的值' }],
    notes: [
      '示例：parameters=[{field:"hits",location:"reference",value:["知识检索节点ID","hits"]}]，code="hits.map(h => h.content).join(\'\\n\')"'
    ],
    template: javaScriptNode
  }
}
