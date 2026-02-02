
import ToastEventBus from 'primevue/toasteventbus';

export default {
  install(bus: any) {
    bus.on('message:success', (params: any) => {
      const [summary, detail] = Array.isArray(params) ? params : [params, undefined]
      ToastEventBus.emit('add', { severity: 'success', summary, detail, life: 3000 });
    })
    bus.on('message:info', (params: any) => {
      const [summary, detail] = Array.isArray(params) ? params : [params, undefined]
      ToastEventBus.emit('add', { severity: 'info', summary, detail, life: 3000 });
    })
    bus.on('message:warn', (params: any) => {
      const [summary, detail] = Array.isArray(params) ? params : [params, undefined]
      ToastEventBus.emit('add', { severity: 'warn', summary, detail, life: 3000 });
    })
    bus.on('message:error', (params: any) => {
      const [summary, detail] = Array.isArray(params) ? params : [params, undefined]
      ToastEventBus.emit('add', { severity: 'error', summary, detail, life: 3000 });
    })

  }
}
