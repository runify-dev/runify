import type { App } from 'vue'
export default {
    install: (app: App) => {
        app.directive('focus', {
            // 当绑定元素插入到 DOM 中时
            mounted: function (el: any) {
                setTimeout(() => {
                    el.querySelector('input').focus();
                }, 1)

            }
        });
    }
}
