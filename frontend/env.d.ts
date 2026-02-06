/// <reference types="vite/client" />
declare module "swagger-ui-dist"
declare module "katex"
declare module "pinyin-pro"
declare module '@primevue/forms/resolvers/zod' {
  import { Resolver } from 'vee-validate'
  import { ZodSchema } from 'zod'

  export function zodResolver(schema: ZodSchema): Resolver
  export default zodResolver
}

declare module 'primevue/toasteventbus' {

  export default any
}
declare module '@primeuix/themes/aura' {

  export default any
}
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
