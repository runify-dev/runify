import type {NodeInitContext} from '@/workflow/common/type'

export function init(ctx: NodeInitContext) {
  ctx.model.properties.field_list = [
    {
      label: '工具执行',
      value: 'tool'
    },
    {
      label: '提问内容(用户下次ai交互push上下文) -> 二进制文件才会存在',
      value: 'question'
    },
    {label: '带行号内容', value: 'content'},
    {label: '原始内容', value: 'rawContent'},
    {label: '总行数', value: 'totalLines'},
    {label: '读取行数', value: 'lines'},
  ]
}
