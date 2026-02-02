<template>
  <Dialog v-model:visible="visible" modal header="重命名" :style="{ width: '25rem' }">
    <Form
      ref="formRef"
      v-slot="$form"
      :initial-values="{ name: '' }"
      :resolver="resolver"
      @submit="submit"
      class="flex flex-col gap-4 w-full"
    >
      <div class="flex flex-col gap-2 w-full">
        <IftaLabel>
          <label>名称</label>
          <InputText name="name" type="text" :placeholder="`请输入名称`" fluid />
          <Message v-if="$form.name?.invalid" severity="error" size="small" variant="simple">{{
            $form.name.error.message
          }}</Message>
        </IftaLabel>
      </div>
    </Form>
    <template #footer>
      <Button type="button" label="取消" severity="secondary" @click="close"></Button>
      <Button type="button" label="确定" @click="formRef?.submit"></Button
    ></template>
  </Dialog>
</template>
<script setup lang="ts">
import { ref } from 'vue'
import { z } from 'zod'
import { type FormInstance } from '@primevue/forms'
import { zodResolver } from '@primevue/forms/resolvers/zod'
import type { WorkflowAPI } from '../common'
import bus from '@/bus'

const visible = ref<boolean>(false)
const formRef = ref<FormInstance>()

const resolver = ref(
  zodResolver(
    z.object({
      name: z.string().min(1, { message: `名称必填` })
    })
  )
)
const api = ref<any>()

const open = (call: any) => {
  visible.value = true
  api.value = call
}
const close = () => {
  visible.value = false
  api.value = undefined
}
const submit = ({ valid, values }: any) => {
  if (valid) {
    if (api.value) {
      api
        .value(values.name)
        .then(() => {
          close()
        })
        .catch((e) => {
          bus.emit('message:error', e.message)
        })
    }
  }
}

defineExpose({
  open,
  close
})
</script>
<style lang="scss"></style>
