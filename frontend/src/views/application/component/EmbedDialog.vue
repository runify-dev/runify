<template>
  <Dialog
    v-model:visible="visible"
    header="嵌入第三方"
    :style="{ width: '700px' }"
    :modal="true"
    class="text-sm"
  >
    <div class="grid grid-cols-2 gap-4">
      <div class="border border-surface-200 dark:border-surface-700 rounded-xl p-4 hover:border-primary transition-colors cursor-pointer group" @click="copyEmbedCode('iframe')">
        <div class="flex items-center gap-3 mb-3">
          <div class="w-10 h-10 rounded-lg bg-primary-100 flex items-center justify-center">
            <i class="pi pi-window-maximize text-primary-600"></i>
          </div>
          <div>
            <h4 class="text-sm font-semibold text-surface-900 dark:text-surface-0">iframe 嵌入</h4>
            <p class="text-xs text-surface-500">适合网页嵌入</p>
          </div>
        </div>
        <div class="bg-surface-50 dark:bg-surface-800 rounded-lg p-3 text-[11px] text-surface-600 dark:text-surface-300 font-mono break-all leading-relaxed">
          {{ embedCode }}
        </div>
        <div class="mt-3 flex justify-end">
          <span class="text-xs text-primary-500 opacity-0 group-hover:opacity-100 transition-opacity">
            <i class="pi pi-copy mr-1"></i>点击复制
          </span>
        </div>
      </div>

      <div class="border border-surface-200 dark:border-surface-700 rounded-xl p-4 hover:border-primary transition-colors cursor-pointer group" @click="copyEmbedCode('js')">
        <div class="flex items-center gap-3 mb-3">
          <div class="w-10 h-10 rounded-lg bg-orange-50 flex items-center justify-center">
            <i class="pi pi-code text-orange-500"></i>
          </div>
          <div>
            <h4 class="text-sm font-semibold text-surface-900 dark:text-surface-0">JavaScript 嵌入</h4>
            <p class="text-xs text-surface-500">悬浮对话气泡</p>
          </div>
        </div>
        <div class="bg-surface-50 dark:bg-surface-800 rounded-lg p-3 text-[11px] text-surface-600 dark:text-surface-300 font-mono break-all leading-relaxed">
          {{ embedJsCode }}
        </div>
        <div class="mt-3 flex justify-end">
          <span class="text-xs text-primary-500 opacity-0 group-hover:opacity-100 transition-opacity">
            <i class="pi pi-copy mr-1"></i>点击复制
          </span>
        </div>
      </div>
    </div>
  </Dialog>
</template>

<script setup lang="ts">
import {ref, computed} from 'vue'
import Dialog from 'primevue/dialog'
import {copyContent} from "@/utils/common"
import bus from "@/bus"

const visible = ref(false)
const applicationId = ref('')

const getEmbedHost = () => {
  return window.location.origin === 'http://localhost:3000' ? 'http://localhost:3001' : window.location.origin
}

const embedCode = computed(() => {
  const host = getEmbedHost()
  return `<iframe src="${host}/conversation/a/${applicationId.value}" style="width:100vw;height:100vh;border:none;"></iframe>`
})

const embedJsCode = computed(() => {
  const host = getEmbedHost()
  return `<script src="${host}/conversation/api/application/${applicationId.value}/embed"><\/script>`
})

const copyEmbedCode = (type: 'iframe' | 'js') => {
  const code = type === 'iframe' ? embedCode.value : embedJsCode.value
  copyContent(code).then(() => {
    bus.emit("message:success", "复制成功")
  }).catch(() => {
    bus.emit("message:success", code)
  })
}

const open = (id: string) => {
  applicationId.value = id
  visible.value = true
}

const close = () => {
  visible.value = false
}

defineExpose({open, close})
</script>
