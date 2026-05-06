import type { NodeInitContext } from '@/workflow/common/type'

export function init(ctx: NodeInitContext) {
  ctx.model.properties.field_list = [
    {
      label:"上下文",
      value: 'messages'
    },
    {
      label: '问题',
      value: 'question'
    },
    {
      label: '图片',
      value: 'images'
    },
    {
      label: '文本',
      value: 'texts'
    },
    {
      label: '视频',
      value: 'videos'
    },
    {
      label: '文件',
      value: 'files'
    }
  ]

  if (!ctx.model.properties.globalFieldList) {
    ctx.model.properties.globalFieldList = []
  }
}
