import type { NodeInitContext } from '@/workflow/common/type'

export function init(ctx: NodeInitContext) {
  ctx.model.properties.field_list = [
   {
      label: '工具执行',
      value: 'tool'
    },
    { label: '执行结果', value: 'result' },
    { label: '标准输出', value: 'stdout' },
    { label: '错误输出', value: 'stderr' },
    { label: '退出码', value: 'exitCode' }
  ]
}
