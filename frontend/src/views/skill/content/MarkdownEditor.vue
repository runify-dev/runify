<template>
  <div class="flex flex-col h-full min-h-0">
    <div class="flex items-center gap-2 px-4 py-2 border-b shrink-0" style="border-color: var(--p-content-border-color);">
      <i class="pi pi-file-edit text-primary-500"/>
      <span class="text-sm font-semibold">{{ file.name }}</span>
      <span v-if="saving" class="text-xs text-surface-400">{{ t('skill.details.saving') }}</span>
      <span v-else-if="saved" class="text-xs text-green-500">{{ t('skill.details.saved') }}</span>
    </div>
    <div class="flex-1 min-h-0 overflow-hidden">
      <Editor ref="editorRef" @change="onChange"/>
    </div>
  </div>
</template>
<script setup lang="ts">
import {onBeforeUnmount, onMounted, ref} from 'vue'
import {t} from '@/locales'
import Editor from '@/editor/index.vue'
import skillApi from '@/api/skill'
import type {SkillFile} from '@/api/skill'

const props = defineProps<{ file: SkillFile; skillId: string }>()

const editorRef = ref<InstanceType<typeof Editor>>()
let saveTimer: any = null
const saving = ref(false)
const saved = ref(false)

onMounted(() => {
  setTimeout(() => { editorRef.value?.setContent(props.file.content || '') }, 50)
})

const onChange = (editor: any) => {
  if (saveTimer) clearTimeout(saveTimer)
  saved.value = false
  saveTimer = setTimeout(() => {
    saving.value = true
    skillApi.updateContent(props.skillId, props.file.id, editor.editor.getMarkdown()).then(() => {
      saving.value = false
      saved.value = true
      setTimeout(() => { saved.value = false }, 2000)
    }).catch(() => { saving.value = false })
  }, 3000)
}

onBeforeUnmount(() => { if (saveTimer) clearTimeout(saveTimer) })
</script>
