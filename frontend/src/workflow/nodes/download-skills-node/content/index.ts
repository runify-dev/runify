import type { NodeInitContext } from '@/workflow/common/type'

export function init(ctx: NodeInitContext) {
  ctx.model.properties.field_list = [
    {
      label: '工具执行',
      value: 'tool'
    },
    { label: '技能ID', value: 'skillId' },
    { label: '技能名称', value: 'skillName' },
    { label: '文件数', value: 'files' },
    { label: '安装状态', value: 'status' },
    { label: '本地路径', value: 'localPath' }
  ]
}
