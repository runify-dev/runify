const knowledgeRouter = {
  path: "/note",
  name: "note",
  meta: { title: "笔记", icon: 'app-document', 'activeMenu': 'note' },
  component: () => import("@/views/note/index.vue"),
  redirect: '/note/folders/root',
  children: [
    {
      path: 'folders/:id',
      name: 'noteFolders',
      meta: { title: 'common.fileUpload.document', activeMenu: 'note' },
      component: () => import("@/views/note/List.vue"),
    },

    {
      path: 'resources/:id',
      name: 'noteDetails',
      meta: { title: 'common.fileUpload.document', activeMenu: 'note' },
      component: () => import("@/views/note/Details.vue"),
    },
    {
      path: 'resources/:id/edit',
      name: 'noteEdit',
      meta: { title: 'common.fileUpload.document', activeMenu: 'note' },
      component: () => import("@/views/note/Edit.vue"),
    }
  ]
};

export default knowledgeRouter;
