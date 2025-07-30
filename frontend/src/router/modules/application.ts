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
            redirect: { name: 'applicationOverview' },
            children: [
                {
                    path: "overview",
                    name: 'applicationOverview',
                    meta: { title: '概览', activeMenu: 'application' },
                    component: () => import("@/views/application/Overview.vue"),
                },
                {
                    path: "setting",
                    name: 'applicationSetting',
                    meta: { title: '设置', activeMenu: 'application' },
                    component: () => import("@/views/application/Setting.vue"),
                },
                {
                    path: "conversation-log",
                    name: 'applicationConversationLog',
                    meta: { title: '对话日志', activeMenu: 'application' },
                    component: () => import("@/views/application/ConversationLog.vue"),
                }
            ]
        }
    ]
};

export default noteRouter;
