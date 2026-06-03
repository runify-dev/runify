<template>
  <Dialog v-model:visible="visible" modal header="重命名" :style="{ width: '25rem' }">
    <div class="flex flex-col gap-4">
      <div class="flex flex-col gap-1.5">
        <label class="text-sm font-medium text-surface-700">名称</label>
        <InputText v-model="name" type="text" fluid class="!text-sm" autofocus @keyup.enter="submit"/>
      </div>
    </div>
    <template #footer>
      <Button label="取消" severity="secondary" variant="outlined" @click="close"/>
      <Button label="确定" :disabled="!name.trim()" @click="submit"/>
    </template>
  </Dialog>
</template>
<script setup lang="ts">
import {ref} from 'vue'

const emit = defineEmits(['rename:success'])

const visible = ref(false)
const name = ref('')
const fileId = ref('')
const skillId = ref('')

const open = (options: { skillId: string; fileId: string; currentName: string }) => {
  skillId.value = options.skillId
  fileId.value = options.fileId
  name.value = options.currentName
  visible.value = true
}

const close = () => {
  visible.value = false
}

const submit = () => {
  if (!name.value.trim()) return
  emit('rename:success', {skillId: skillId.value, fileId: fileId.value, name: name.value})
  close()
}

defineExpose({open, close})
</script>
