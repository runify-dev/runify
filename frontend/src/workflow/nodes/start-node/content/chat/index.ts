import type {NodeInitContext} from '@/workflow/common/type'

export function init(ctx: NodeInitContext) {
  ctx.model.properties.field_list = [
    {
      label: "上下文",
      value: 'messages'
    },
    {
      label: '用户问题',
      value: 'question',
      children: [
        {
          label: '问题',
          value: 'content'
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
        }]
    },
    {
      label: '用户',
      value: 'user',
      children: [
        {
          label: 'ID',
          value: 'id'
        },
        {
          label: '类型',
          value: 'type'
        },
        {
          label: '资料',
          value: 'profile',
          children: [
            {
              label: '昵称',
              value: 'nickname'
            },
            {
              label: '用户名',
              value: 'username'
            },
            {
              label: '邮箱',
              value: 'email'
            },
            {
              label: '手机',
              value: 'phone'
            },
            {
              label: '头像',
              value: 'icon'
            }
          ]
        }
      ]
    },

  ]

  if (!ctx.model.properties.globalFieldList) {
    ctx.model.properties.globalFieldList = []
  }
}
