const noteRouter = {
  path: "/system-management",
  name: "system-management",
  meta: { title: "系统管理", icon: 'app-model', activeMenu: 'system-management' },
  component: () => import("@/views/system-management/index.vue"),
  redirect: '/system-management/user',
  children: [
    {
      path: 'user',
      name: 'user',
      meta: { title: '用户管理', activeMenu: 'system-management' },
      component: () => import("@/views/user-management/index.vue"),
    }
  ]
};

export default noteRouter;
