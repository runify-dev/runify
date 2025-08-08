import type { App } from 'vue'
import type { Dict } from '@/api/type/common'
import DynamicsForm from '@/components/dynamics-form/index.vue'
let components: Dict<any> = {
  ...import.meta.glob('@/components/dynamics-form/form-item/**/**.vue', {
    eager: true
  }),
  ...import.meta.glob('@/components/dynamics-form/constructor/**/**.vue', {
    eager: true
  })
};


components = {
  ...components,
  ...import.meta.glob('@/components/dynamics-form/form-item/**/**/**.vue', {
    eager: true
  })
}

const install = (app: App) => {
  Object.keys(components).forEach((key: string) => {
    const commentName: string = key
      .substring(key.lastIndexOf('/') + 1, key.length)
      .replace('.vue', '')
    app.component(commentName, components[key].default)
  })
  app.component('DynamicsForm', DynamicsForm)
}
export default { install }
