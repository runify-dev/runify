const noteRouter = {
  path: "/knowledge",
  name: "knowledge",
  meta: { title: "知识库", icon: 'app-document', 'activeMenu': 'knowledge' },
  component: () => import("@/views/knowledge/index.vue"),
  redirect: '/knowledge/folder/root/resource/root',
  children: [
    {
      path: 'star',
      name: 'knowledgeListResourceStar',
      meta: { title: 'common.fileUpload.document', activeMenu: 'knowledge' },
      component: () => import("@/views/knowledge/List.vue"),
    },
    {
      path: 'share',
      name: 'knowledgeListResourceShare',
      meta: { title: 'common.fileUpload.document', activeMenu: 'knowledge' },
      component: () => import("@/views/knowledge/List.vue"),
    },
    {
      path: 'folder/:folderId/resource/:id',
      name: 'knowledgeListResource',
      meta: { title: 'common.fileUpload.document', activeMenu: 'knowledge' },
      component: () => import("@/views/knowledge/List.vue"),
    },
    {
      path: 'folder/:folderId/resource/:id/details',
      name: 'knowledgeDetails',
      meta: { title: 'common.fileUpload.document', activeMenu: 'knowledge' },
      component: () => import("@/views/knowledge/Details.vue"),
    },
    {
      path: 'folder/:folderId/resource/:id/edit',
      name: 'knowledgeEdit',
      meta: { title: 'common.fileUpload.document', activeMenu: 'knowledge' },
      component: () => import("@/views/knowledge/Edit.vue"),
    }
  ]
};

export default noteRouter;
