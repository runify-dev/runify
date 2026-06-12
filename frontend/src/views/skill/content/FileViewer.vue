<template>
  <div class="flex-1 flex items-center justify-center">
    <div class="text-center max-w-sm">
      <i class="pi pi-file text-5xl text-primary-500 mb-4"/>
      <h3 class="text-lg font-semibold mb-2">{{ file.fileName || file.name }}</h3>
      <p class="text-sm text-surface-500 mb-1">{{ t('skill.details.size') }}: {{ formatSize(file.fileSize) }}</p>
      <p class="text-sm text-surface-500 mb-4" v-if="file.desc">{{ t('skill.details.desc') }}: {{ file.desc }}</p>
      <Button icon="pi pi-download" :label="t('skill.details.download')" @click="download"/>
    </div>
  </div>
</template>
<script setup lang="ts">
import type {SkillFile} from '@/api/skill'
import {t} from '@/locales'

const props = defineProps<{ file: SkillFile; skillId: string }>()

const download = () => {
  if (!props.file.fileId) return
  const a = document.createElement('a')
  a.href = `${window.RUNIFY_APP.admin.baseURL}/api/storage/file/${props.file.fileId}`
  a.download = props.file.fileName || props.file.name
  a.click()
}

const formatSize = (bytes?: number) => {
  if (!bytes) return t('skill.details.unknown')
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1048576) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / 1048576).toFixed(1) + ' MB'
}
</script>
