const noteRouter = {
    path: "/application",
    name: "application",
    meta: { title: "应用", icon: 'app-document', activeMenu: "application" },
    component: () => import("@/views/application/index.vue"),
    redirect: '/application/list/all',
    children: [
        {
            path: 'list/:id',
            name: 'applicationList',
            meta: { title: 'common.fileUpload.document', activeMenu: 'application' },
            component: () => import("@/views/application/List.vue"),
        },
        {
            path: 'details/:type/:id',
            name: 'applicationOverview',
            meta: { title: 'common.fileUpload.document', activeMenu: 'application' },
            component: () => import("@/views/application-overview/index.vue"),
        }
    ]
};

export default noteRouter;
