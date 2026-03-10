<template>
  <Dialog @click.stop v-model:visible="visible" header="表达式" width="500" :before-close="close">
    <Form ref="formRef">
      <FormField v-slot="$field" name="mathematics" :resolver="resolver.mathematics">
        <IftaLabel>
          <label>表达式</label>
          <Textarea rows="3" cols="30"
        /></IftaLabel>
        <Message v-if="$field?.invalid" severity="error" size="small" variant="simple">{{
          $field.error?.message
        }}</Message>
      </FormField>
    </Form>
    <template #footer>
      <Button @click="close">取消</Button>
      <Button @click.stop="submit"> 确定 </Button>
    </template>
  </Dialog>
</template>
<script setup lang="ts">
import { ref, computed, nextTick } from 'vue'
import { type Editor } from '@tiptap/vue-3'

import { zodResolver } from '@primevue/forms/resolvers/zod'
import { z } from 'zod'
import type { FormInstance } from '@primevue/forms'
const visible = ref<boolean>()
const current = ref<Editor>()
const close = () => {
  current.value = undefined
  visible.value = false
}
const resolver = computed(() => {
  return {
    mathematics: zodResolver(z.string().min(1, `表达式必填`))
  }
})
const formRef = ref<FormInstance>()
const isEdit = ref<boolean>(false)
const open = (editor: Editor, mathematics?: string) => {
  visible.value = true
  current.value = editor
  if (mathematics) {
    isEdit.value = true
  }
  nextTick(() => {
    formRef.value?.setFieldValue('mathematics', mathematics)
  })
}

const submit = () => {
  formRef.value?.validate().then(({ values, errors }) => {
    console.log(errors, values)
    if (Object.keys(errors).length == 0) {
      if (isEdit.value) {
        current.value?.chain().updateBlockMath({ latex: values.mathematics }).focus().run()
      } else {
        current.value?.chain().insertBlockMath({ latex: values.mathematics }).focus().run()
      }

      close()
    }
  })
}

defineExpose({ open, close })
</script>
<style lang="scss" scoped></style>
