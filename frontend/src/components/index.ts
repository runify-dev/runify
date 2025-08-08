import { type App } from 'vue'
import dynamicsForm from './dynamics-form'
export default {
  install(app: App) {
    app.use(dynamicsForm)
  }
}
