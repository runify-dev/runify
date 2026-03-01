import { type DirectiveBinding } from 'vue';
import { createApp, h, type App } from 'vue';
import ProgressSpinner from 'primevue/progressspinner';

interface LoadingInstance {
  el: HTMLElement;
  spinnerApp: ReturnType<typeof createApp> | null;
}

const loadingMap = new WeakMap<HTMLElement, LoadingInstance>();


function toggleLoading(el: HTMLElement, loading: boolean) {
  let instance = loadingMap.get(el);
  if (!instance) return;

  if (loading) {
    if (!instance.spinnerApp) {
      const overlay = document.createElement('div');
      overlay.className = 'v-loading-overlay';
      overlay.style.cssText = `
                position: absolute;
                top: 0;
                left: 0;
                width: 100%;
                height: 100%;
                display: flex;
                justify-content: center;
                align-items: center;
                background: rgba(255,255,255,0.6);
                z-index: 9999;
            `;
      el.style.position = 'relative';
      el.appendChild(overlay);

      instance.spinnerApp = createApp({
        render: () => h(ProgressSpinner, { style: { width: '50px', height: '50px' } }),
      });
      instance.spinnerApp.mount(overlay);
    }
  } else {
    if (instance.spinnerApp) {
      instance.spinnerApp.unmount();
      const overlay = el.querySelector('.v-loading-overlay');
      overlay && el.removeChild(overlay);
      instance.spinnerApp = null;
    }
  }
}
export default {
  install(app: App) {
    app.directive('loading', {
      mounted(el: HTMLElement, binding: DirectiveBinding) {
        const instance: LoadingInstance = {
          el,
          spinnerApp: null,
        };
        loadingMap.set(el, instance);
        toggleLoading(el, binding.value);
      },
      updated(el: HTMLElement, binding: DirectiveBinding) {
        toggleLoading(el, binding.value);
      },
      unmounted(el: HTMLElement) {
        const instance = loadingMap.get(el);
        if (instance?.spinnerApp) {
          instance.spinnerApp.unmount();
          el.removeChild(el.querySelector('.v-loading-overlay')!);
        }
        loadingMap.delete(el);
      },
    })
  }
}
