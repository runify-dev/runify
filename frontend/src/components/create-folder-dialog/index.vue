<template>
  <Dialog v-model:visible="visible" modal :header="`新建文件夹`" :style="{ width: '25rem' }">
    <Form
      ref="formRef"
      v-slot="$form"
      :initial-values="{ name: '' }"
      :resolver="resolver"
      @submit="submit"
    >
      <div>
        <IftaLabel>
          <label>文件夹名称</label>
          <InputText name="name" type="text" fluid />
        </IftaLabel>
        <Message v-if="$form.name?.invalid" severity="error" size="small" variant="simple">{{
          $form.name.error.message
        }}</Message>
      </div>
    </Form>
    <template #footer>
      <Button type="button" label="取消" severity="secondary" @click="close"></Button>
      <Button type="button" label="创建" @click="formRef?.submit"></Button
    ></template>
  </Dialog>
</template>
<script setup lang="ts">
import { ref } from 'vue'
import { z } from 'zod'
import type { TreeNode } from 'primevue/treenode'
import { TreeCommonAPI } from '@/api/tree'
import { type FormInstance } from '@primevue/forms'
import { zodResolver } from '@primevue/forms/resolvers/zod'
const props = defineProps<{ api: TreeCommonAPI }>()
const emit = defineEmits(['create:folder:success'])

const visible = ref<boolean>(false)
const formRef = ref<FormInstance>()
const current = ref<TreeNode>()
const resolver = ref(
  zodResolver(
    z.object({
      name: z.string().min(1, { message: `文件夹名称必填` })
    })
  )
)
const open = (node?: TreeNode) => {
  visible.value = true
  current.value = node
}
const close = () => {
  visible.value = false
}
const submit = ({ valid, values }: any) => {
  if (valid) {
    props.api
      .createFolder(current.value ? current.value.key : 'root', { name: values.name })
      .then((ok) => {
        emit('create:folder:success', current.value ? current.value.key : undefined, ok.data)
        close()
      })
  }
}

defineExpose({
  open,
  close
})
</script>
<style lang="scss"></style>
