const projectRouter = {
  path: "/project",
  name: "project",
  meta: { title: "项目", icon: 'app-document', 'activeMenu': 'project' },
  component: () => import("@/views/project/index.vue"),
  redirect: '/project/folders/root',
  children: [
    {
      path: 'folders/:id',
      name: 'projectFolders',
      meta: { title: 'common.fileUpload.document', activeMenu: 'project' },
      component: () => import("@/views/project/List.vue"),
    },
    {
      path: 'resources/:id',
      name: 'projectDetails',
      meta: { title: 'common.fileUpload.document', activeMenu: 'project' },
      component: () => import("@/views/project/Details.vue"),
      redirect: { name: 'projectPublic' },
      children: [
        {
          path: "public",
          name: 'projectPublic',
          meta: { title: '公共资源', activeMenu: 'project' },
          component: () => import("@/views/project/public/index.vue"),
        },
        {
          path: "processor",
          name: 'processor',
          meta: { title: '处理器', activeMenu: 'project' },
          component: () => import("@/views/project/processor/index.vue"),
          redirect: { name: 'processorTable' },
          children: [
            {
              path: "workflow/:processorId",
              name: 'processorWorkflow',
              meta: { title: '处理器', activeMenu: 'project', activeSubMenu: 'processor' },
              component: () => import("@/views/project/processor/workflow/index.vue"),
            },
            {
              path: "index",
              name: 'processorTable',
              meta: { title: '处理器', activeMenu: 'project', activeSubMenu: 'processor' },
              component: () => import("@/views/project/processor/table/index.vue"),
            }
          ]
        },
      ]
    }
  ]
};

export default projectRouter;
