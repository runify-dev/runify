const modelRouter = {
  path: "/model",
  name: "model",
  meta: { title: "模型", icon: 'app-model', activeMenu: "model" },
  component: () => import("@/views/model/index.vue"),
  redirect: '/model/folders/root',
  children: [
    {
      path: 'folders/:id',
      name: 'modelFolders',
      meta: { title: 'common.fileUpload.document', activeMenu: 'model' },
      component: () => import("@/views/model/List.vue"),
    },

    {
      path: 'resources/:id',
      name: 'modelDetails',
      meta: { title: 'common.fileUpload.document', activeMenu: 'model' },
      component: () => import("@/views/model/Details.vue"),
    },
    {
      path: 'resources/:id/edit',
      name: 'modelEdit',
      meta: { title: 'common.fileUpload.document', activeMenu: 'model' },
      component: () => import("@/views/model/Edit.vue"),
    }
  ]
};

export default modelRouter;
