<template>
  <Dialog v-model:visible="visible" modal header="修改名称" :style="{ width: '25rem' }">
    <Form
      ref="formRef"
      v-slot="$form"
      :initial-values="{ name: currentName }"
      :resolver="resolver"
      @submit="submit"
    >
      <div>
        <IftaLabel>
          <label>名称</label>
          <InputText name="name" type="text" fluid />
        </IftaLabel>
        <Message v-if="$form.name?.invalid" severity="error" size="small" variant="simple">{{
          $form.name.error.message
        }}</Message>
      </div>
    </Form>
    <template #footer>
      <Button type="button" label="取消" severity="secondary" @click="close"></Button>
      <Button type="button" label="确定" @click="formRef?.submit"></Button>
    </template>
  </Dialog>
</template>
<script setup lang="ts">
import { ref } from 'vue'
import { z } from 'zod'
import { TreeCommonAPI } from '@/api/tree'
import { type FormInstance } from '@primevue/forms'
import { zodResolver } from '@primevue/forms/resolvers/zod'
import { t } from '@/locales'

const props = defineProps<{ api: TreeCommonAPI }>()
const emit = defineEmits(['rename:success'])

const visible = ref<boolean>(false)
const formRef = ref<FormInstance>()
const currentKey = ref<string>('')
const currentName = ref<string>('')
const currentType = ref<'folder' | 'resource'>('resource')

const resolver = ref(
  zodResolver(
    z.object({
      name: z.string().min(1, { message: t('common.nameRequired') })
    })
  )
)

const open = (key: string, name: string, type: 'folder' | 'resource' = 'resource') => {
  visible.value = true
  currentKey.value = key
  currentName.value = name
  currentType.value = type
}

const close = () => {
  visible.value = false
}

const submit = ({ valid, values }: any) => {
  if (valid) {
    const apiCall =
      currentType.value === 'folder'
        ? props.api.modifyFolderName(currentKey.value, values.name)
        : props.api.modifyResourceName(currentKey.value, values.name)

    apiCall.then((ok) => {
      emit('rename:success', currentKey.value, ok.data)
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
