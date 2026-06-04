<template>
  <Dialog v-model:visible="visible" modal header="重命名" :style="{ width: '24rem' }">
    <div class="flex flex-col gap-1.5">
      <label class="text-sm font-medium text-surface-700">名称</label>
      <InputText v-model="name" type="text" placeholder="请输入名称" fluid class="!text-sm"/>
    </div>
    <template #footer>
      <div class="flex justify-end gap-2">
        <Button label="取消" severity="secondary" variant="outlined" @click="close"/>
        <Button label="确定" @click="submit"/>
      </div>
    </template>
  </Dialog>
</template>
<script setup lang="ts">
import {ref} from 'vue'

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
