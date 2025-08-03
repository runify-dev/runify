const noteRouter = {
    path: "/model",
    name: "model",
    meta: { title: "模型", icon: 'app-document', activeMenu: "model" },
    component: () => import("@/views/model/index.vue"),
    redirect: '/model/folder/root/resource/root',
    children: [
        {
            path: 'folder/:folderId/resource/:id',
            name: 'modelListResource',
            meta: { title: 'common.fileUpload.document', activeMenu: 'model' },
            component: () => import("@/views/model/List.vue"),
        },
        {
            path: 'folder/:folderId/resource/:id/details',
            name: 'modelDetails',
            meta: { title: 'common.fileUpload.document', activeMenu: 'model' },
            component: () => import("@/views/model/Details.vue"),
        }
    ]
};

export default noteRouter;
