import type { RouteRecordRaw } from "vue-router";
const modules: any = import.meta.glob("./modules/*.ts", { eager: true });
const routeList: any = [
  ...Object.keys(modules).map((key) => modules[key].default),
];
export const routes: Array<RouteRecordRaw> = [
  {
    path: "/",
    name: "home",
    redirect: "application",
    component: () => import("@/layout-plus/index.vue"),
    children: routeList,
  },
  {
    path: "/doc",
    name: "doc",
    component: () => import("@/views/openapi/index.vue"),
  },
  {
    path: "/login",
    name: "login",
    component: () => import("@/views/login/index.vue"),
  },
  {
    path: "/test",
    name: "test",
    component: () => import("@/test.vue"),
  },
  {
    path: "/test1",
    name: "test1",
    component: () => import("@/test1.vue"),
  },
];
