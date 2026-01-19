const noteRouter = {
  path: '/application-details/resources/:id',
  name: 'application-details',
  meta: { title: 'common.fileUpload.document', activeMenu: 'application' },
  component: () => import("@/views/application/Details.vue"),
  redirect: { name: 'applicationOverview' },
  children: [
    {
      path: "overview",
      name: 'applicationOverview',
      meta: { title: '概览', activeMenu: 'application' },
      component: () => import("@/views/application/Overview.vue"),
    },
    {
      path: "setting",
      name: 'applicationSetting',
      meta: { title: '设置', activeMenu: 'application' },
      component: () => import("@/views/application/Setting.vue"),
    },
    {
      path: "conversation-log",
      name: 'applicationConversationLog',
      meta: { title: '对话日志', activeMenu: 'application' },
      component: () => import("@/views/application/ConversationLog.vue"),
    }
  ]
}

export default noteRouter;
