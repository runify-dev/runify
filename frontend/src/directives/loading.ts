import { createApp } from 'vue';
import type { App } from 'vue'
import LoadingOverlay from '@/components/loading-overlay/index.vue';

const loadingDirective = {
  mounted(el: any, binding: any) {
    // 创建挂载点
    const overlay = document.createElement('div');
    overlay.id = 'loading-overlay-' + Date.now();
    el.appendChild(overlay);
    el.style.position = 'relative';

    // 创建加载组件实例
    const loadingApp = createApp(LoadingOverlay, {
      visible: binding.value,
      text: binding.arg || '',
      fullscreen: binding.modifiers.fullscreen || false,
      size: binding.modifiers.small ? '30px' : '50px'
    });

    // 挂载
    loadingApp.mount(overlay);

    // 保存实例引用
    el._loadingInstance = loadingApp;
    el._loadingOverlay = overlay;
  },
  updated(el: any, binding: any) {
    if (el._loadingInstance) {
      el._loadingInstance.component.proxy.visible = binding.value;
    }
  },
  unmounted(el: any) {
    if (el._loadingInstance) {
      el._loadingInstance.unmount();
      el.removeChild(el._loadingOverlay);
    }
  }
};

export default {
  install: (app: App) => {
    app.directive('loading', loadingDirective);
  }
}
