<template>
  <Drawer
    v-model:visible="drawer"
    :header="t('application.publish.history')"
    position="right"
    :pt="{ root: { style: { '--drawer-width-desktop': '480px' }, class: 'responsive-drawer' } }"
  >
    <div v-if="loading" class="flex items-center justify-center py-16 text-surface-400">
      <i class="pi pi-spin pi-spinner text-2xl" />
    </div>

    <div
      v-else-if="versions.length === 0"
      class="flex flex-col items-center justify-center py-16 text-surface-400"
    >
      <i class="pi pi-inbox text-4xl mb-3 opacity-40" />
      <p class="text-sm">{{ t('application.publish.empty') }}</p>
    </div>

    <div v-else class="flex flex-col gap-3">
      <div
        v-for="(item, index) in versions"
        :key="item.id"
        class="rounded-lg p-3 transition-colors"
        style="border: 1px solid var(--p-content-border-color); background: var(--p-content-background)"
      >
        <div class="flex items-center justify-between mb-1.5">
          <div class="flex items-center gap-2">
            <span class="text-sm font-semibold" style="color: var(--p-text-color)">
              {{ t('application.publish.versionLabel', { version: item.version }) }}
            </span>
            <span
              v-if="index === 0"
              class="text-[11px] px-1.5 py-0.5 rounded-full bg-primary-50 text-primary-600 dark:bg-primary-900/30 dark:text-primary-300"
            >
              {{ t('application.publish.current') }}
            </span>
          </div>
          <Button
            :label="t('application.publish.rollback')"
            icon="pi pi-replay"
            size="small"
            variant="text"
            :loading="rollbackingId === item.id"
            @click="rollback(item)"
          />
        </div>
        <p
          v-if="item.remark"
          class="text-xs mb-2 leading-relaxed"
          style="color: var(--p-text-color)"
        >
          {{ item.remark }}
        </p>
        <div class="flex items-center gap-3 text-[11px]" style="color: var(--p-text-muted-color)">
          <span class="flex items-center gap-1">
            <i class="pi pi-user text-[10px]" />
            {{ item.createUserName || t('application.publish.unknownUser') }}
          </span>
          <span class="flex items-center gap-1">
            <i class="pi pi-clock text-[10px]" />
            {{ item.createTime }}
          </span>
        </div>
      </div>
    </div>
  </Drawer>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { t } from '@/locales'
import bus from '@/bus'
import applicationApi from '@/api/application'

const emit = defineEmits<{ rollback: [workflow: any] }>()

const drawer = ref(false)
const loading = ref(false)
const rollbackingId = ref<string>('')
const applicationId = ref<string>('')
const versions = ref<Array<any>>([])

const open = (id: string) => {
  applicationId.value = id
  drawer.value = true
  fetchVersions()
}

const close = () => {
  drawer.value = false
}

const fetchVersions = () => {
  applicationApi.listVersions(applicationId.value, loading).then((ok) => {
    versions.value = ok.data || []
  })
}

// 回滚：拉取该版本 snapshot 的工作流,回填画布(不落库),由父组件渲染,用户再保存/发布
const rollback = (item: any) => {
  rollbackingId.value = item.id
  applicationApi
    .getVersion(applicationId.value, item.id)
    .then((ok) => {
      const workflow = ok.data?.snapshot?.workflow
      emit('rollback', workflow)
      drawer.value = false
      bus.emit('message:success', t('application.publish.rollbackLoaded'))
    })
    .finally(() => {
      rollbackingId.value = ''
    })
}

defineExpose({ open, close })
</script>

<style lang="scss">
.responsive-drawer {
  width: 90% !important;
}
@media (min-width: 1024px) {
  .responsive-drawer {
    width: var(--drawer-width-desktop) !important;
  }
}
</style>
