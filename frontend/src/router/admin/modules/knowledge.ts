const knowledgeRouter = {
  path: "/knowledge",
  name: "knowledge",
  meta: { title: "知识库", icon: 'app-document', 'activeMenu': 'knowledge' },
  component: () => import("@/views/knowledge/index.vue"),
  redirect: '/knowledge/folders/root',
  children: [
    {
      path: 'folders/:id',
      name: 'knowledgeFolders',
      meta: { title: 'common.fileUpload.document', activeMenu: 'knowledge' },
      component: () => import("@/views/knowledge/List.vue"),
    },

    {
      path: 'resources/:id',
      name: 'knowledgeDetails',
      meta: { title: 'common.fileUpload.document', activeMenu: 'knowledge' },
      component: () => import("@/views/knowledge/Details.vue"),
    },
    {
      path: 'resources/:id/edit',
      name: 'knowledgeEdit',
      meta: { title: 'common.fileUpload.document', activeMenu: 'knowledge' },
      component: () => import("@/views/knowledge/Edit.vue"),
    }
  ]
};

export default knowledgeRouter;
