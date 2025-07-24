const noteRouter = {
    path: "/application",
    name: "application",
    meta: { title: "应用", icon: 'app-document', activeMenu: "application" },
    component: () => import("@/views/application/index.vue"),
    redirect: '/application/folder/root/resource/root',
    children: [
        {
            path: 'star',
            name: 'applicationListResourceStar',
            meta: { title: 'common.fileUpload.document', activeMenu: 'application' },
            component: () => import("@/views/application/List.vue"),
        },
        {
            path: 'share',
            name: 'applicationListResourceShare',
            meta: { title: 'common.fileUpload.document', activeMenu: 'application' },
            component: () => import("@/views/application/List.vue"),
        },
        {
            path: 'folder/:folderId/resource/:id',
            name: 'applicationListResource',
            meta: { title: 'common.fileUpload.document', activeMenu: 'application' },
            component: () => import("@/views/application/List.vue"),
        },
        {
            path: 'folder/:folderId/resource/:id/details',
            name: 'applicationDetails',
            meta: { title: 'common.fileUpload.document', activeMenu: 'application' },
            component: () => import("@/views/application/Details.vue"),
        },
        {
            path: 'folder/:folderId/resource/:id/edit',
            name: 'applicationEdit',
            meta: { title: 'common.fileUpload.document', activeMenu: 'application' },
            component: () => import("@/views/application/Edit.vue"),
        }
    ]
};

export default noteRouter;
