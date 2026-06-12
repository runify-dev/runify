<template>
  <div class="flex flex-col h-full min-h-0">
    <!-- 未选中文件 -->
    <div v-if="!selectedFile" class="flex-1 flex items-center justify-center text-surface-400">
      <div class="text-center">
        <i class="pi pi-folder-open text-4xl mb-3 opacity-40"/>
        <p class="text-sm">{{ t('skill.details.selectFile') }}</p>
      </div>
    </div>

    <!-- 文件夹：子文件列表 -->
    <div v-else-if="selectedFile.type === 'folder'" class="flex-1 overflow-auto p-4">
      <h3 class="text-base font-semibold mb-4">{{ selectedFile.name }}</h3>
      <div class="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-3">
        <div
          v-for="child in folderChildren"
          :key="child.id"
          class="flex items-center gap-3 p-3 rounded-lg border cursor-pointer hover:bg-surface-50 dark:hover:bg-surface-800 transition-colors"
          style="border-color: var(--p-content-border-color);"
          @click="selectFile(child)"
        >
          <i :class="child.type === 'folder' ? 'pi pi-folder' : child.type === 'text' ? 'pi pi-file-edit' : 'pi pi-file'" class="text-lg text-primary-500"/>
          <div class="min-w-0 flex-1">
            <p class="text-sm font-medium truncate">{{ child.name }}</p>
            <p class="text-xs text-surface-400">{{ child.type === 'file' ? formatSize(child.fileSize) : child.type }}</p>
          </div>
        </div>
        <div v-if="!folderChildren.length" class="col-span-full text-center py-8 text-surface-400 text-sm">{{ t('skill.details.emptyFolder') }}</div>
      </div>
    </div>

    <!-- 文件内容：根据后缀自动选择编辑器 -->
    <ContentViewer
      v-else
      :file="selectedFile"
      :skillId="currentSkillId"
      class="flex-1 min-h-0"
    />
  </div>
</template>

<script setup lang="ts">
import {inject, ref, watch, type Ref} from 'vue'
import {t} from '@/locales'
import ContentViewer from './content/index.vue'
import skillApi from '@/api/skill'
import type {SkillFile} from '@/api/skill'

const currentSkillId = inject<Ref<string>>('currentSkillId', ref(''))
const selectedSkillFile = inject<Ref<SkillFile | null>>('selectedSkillFile', ref(null))

const selectedFile = ref<SkillFile | null>(null)
const folderChildren = ref<SkillFile[]>([])

watch(selectedSkillFile, (file) => {
  selectedFile.value = file
  if (file?.type === 'folder') {
    skillApi.listChildren(currentSkillId.value, file.id).then(res => { folderChildren.value = res.data })
  }
}, {immediate: true})

const selectFile = (file: SkillFile) => {
  selectedFile.value = file
  if (file.type === 'folder') {
    skillApi.listChildren(currentSkillId.value, file.id).then(res => { folderChildren.value = res.data })
  }
}

const formatSize = (bytes?: number) => {
  if (!bytes) return t('skill.details.unknown')
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1048576) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / 1048576).toFixed(1) + ' MB'
}
</script>
