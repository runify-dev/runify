<template>
  <Dialog v-model:visible="visible" modal :header="t('knowledge.document.renameHeader')" :style="{ width: '24rem' }">
    <div class="flex flex-col gap-1.5">
      <label class="text-sm font-medium text-surface-700">{{ t('knowledge.document.newNameLabel') }}</label>
      <InputText v-model="name" type="text" :placeholder="t('knowledge.document.newNamePlaceholder')" fluid class="!text-sm"/>
    </div>
    <template #footer>
      <div class="flex justify-end gap-2">
        <Button :label="t('common.cancel')" severity="secondary" variant="outlined" @click="close"/>
        <Button :label="t('common.confirm')" @click="submit"/>
      </div>
    </template>
  </Dialog>
</template>
<script setup lang="ts">
import {ref} from 'vue'
import {t} from '@/locales'

const emit = defineEmits(['rename:success'])
const visible = ref(false)
const name = ref('')
const currentData = ref<any>(null)

const open = (data: {knowledgeId: string; documentId: string; currentName: string}) => {
  currentData.value = data
  name.value = data.currentName
  visible.value = true
}

const close = () => {
  visible.value = false
  currentData.value = null
}

const submit = () => {
  if (!name.value.trim() || !currentData.value) return
  emit('rename:success', {
    knowledgeId: currentData.value.knowledgeId,
    documentId: currentData.value.documentId,
    name: name.value
  })
  close()
}

defineExpose({open, close})
</script>
