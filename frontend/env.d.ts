/// <reference types="vite/client" />
declare module "swagger-ui-dist"
declare module "katex"
declare module "pinyin-pro"
interface Window {
  RUNIFY_APP: {
    baseURL: string
    admin: {
      baseURL: string
    },
    chat: {
      baseURL: string
    }
  }
}
