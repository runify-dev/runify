<template>
  <Dialog v-model:visible="visible" modal :header="t('skill.file.createTextHeader')" :style="{ width: '25rem' }">
    <div class="flex flex-col gap-4">
      <div class="flex flex-col gap-1.5">
        <label class="text-sm font-medium text-surface-700">{{ t('skill.file.fileNameLabel') }}</label>
        <InputText v-model="name" type="text" :placeholder="t('skill.file.fileNamePlaceholder')" fluid class="!text-sm" autofocus @keyup.enter="submit"/>
      </div>
    </div>
    <template #footer>
      <Button :label="t('common.cancel')" severity="secondary" variant="outlined" @click="close"/>
      <Button :label="t('common.create')" :disabled="!name.trim()" @click="submit"/>
    </template>
  </Dialog>
</template>
<script setup lang="ts">
import {ref} from 'vue'
import {t} from '@/locales'

const emit = defineEmits(['create:success'])

const visible = ref(false)
const name = ref('')
const parentId = ref('')
const skillId = ref('')

const open = (options: { skillId: string; parentId?: string }) => {
  skillId.value = options.skillId
  parentId.value = options.parentId ?? ''
  name.value = t('skill.file.newFileName')
  visible.value = true
}

const close = () => {
  visible.value = false
}

const submit = () => {
  if (!name.value.trim()) return
  emit('create:success', {skillId: skillId.value, parentId: parentId.value, name: name.value})
  close()
}

defineExpose({open, close})
</script>
