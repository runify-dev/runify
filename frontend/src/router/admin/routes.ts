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
    component: () => import("@/layout/AppLayout.vue"),
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
];
