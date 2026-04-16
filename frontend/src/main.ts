import { createApp } from 'vue'
import App from './App.vue'
import router from '@/router/admin/index'
import '@/styles/tailwind.css'
import '@/styles/index.scss'
import directives from '@/directives'
import { createPinia } from 'pinia'
import i18n from '@/locales'
import components from '@/components'
import 'katex/dist/katex.min.css'
import 'highlight.js/styles/atom-one-dark.css'
import PrimeVue from 'primevue/config'
import ToastService from 'primevue/toastservice'
import Aura from '@primeuix/themes/aura'
import 'primeicons/primeicons.css'
import ConfirmationService from 'primevue/confirmationservice'
const app = createApp(App)

app.use(router)
app.use(i18n)
app.use(createPinia())
app.use(directives)
app.use(components)
app.use(ConfirmationService)
app.use(PrimeVue, {
  theme: {
    preset: Aura
  }
})
app.use(ToastService)
app.mount('#app')
