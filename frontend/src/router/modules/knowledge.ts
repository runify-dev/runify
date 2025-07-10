const noteRouter = {
  path: "/knowledge",
  name: "knowledge",
  meta: { title: "知识库", icon: 'app-document', 'activeMenu': 'knowledge' },
  component: () => import("@/views/knowledge/index.vue"),
  redirect: '/knowledge/list/all',
  children: [
    {
      path: 'list/:id',
      name: 'knowledgeList',
      meta: { title: 'common.fileUpload.document', activeMenu: 'knowledge' },
      component: () => import("@/views/knowledge/List.vue"),
    },
    {
      path: 'details/:type/:id',
      name: 'knowledgeDetails',
      meta: { title: 'common.fileUpload.document', activeMenu: 'knowledge' },
      component: () => import("@/views/knowledge/Details.vue"),
    }
  ]
};

export default noteRouter;
