<template>
  <Dialog v-model:visible="visible" header="创建处理器" style="width: 35rem">
    <Form ref="formRef">
      <FormField
        v-slot="$field"
        name="name"
        initial-value=""
        :resolver="resolvers.name"
        class="mt-2"
      >
        <IftaLabel>
          <label>处理器名称</label>
          <InputText type="text" fluid />
          <Message v-if="$field?.invalid" severity="error" size="small" variant="simple">{{
            $field.error?.message
          }}</Message>
        </IftaLabel>
      </FormField>
      <FormField
        v-slot="$field"
        name="desc"
        initial-value=""
        :resolver="resolvers.desc"
        class="mt-2"
      >
        <IftaLabel>
          <label>处理器描述</label>
          <InputText type="text" fluid />
          <Message v-if="$field?.invalid" severity="error" size="small" variant="simple">{{
            $field.error?.message
          }}</Message>
        </IftaLabel>
      </FormField>
      <FormField
        v-slot="$field"
        initial-value="HTTP"
        name="protocol"
        :resolver="resolvers.protocol"
        class="mt-2"
      >
        <IftaLabel>
          <Select
            :options="options"
            optionLabel="label"
            optionValue="value"
            placeholder="请选择协议"
            fluid
          />

          <Message v-if="$field?.invalid" severity="error" size="small" variant="simple">{{
            $field.error?.message
          }}</Message>
          <label>处理器协议</label>
        </IftaLabel>
      </FormField>
    </Form>
    <template #footer>
      <div class="dialog-footer">
        <Button @click="close">取消</Button>
        <Button @click="submit"> 添加 </Button>
      </div>
    </template>
  </Dialog>
</template>
<script setup lang="ts">
import { ref } from 'vue'
import bus from '@/bus'
import processorAPI from '@/api/processor'
import { useRouter } from 'vue-router'
import { zodResolver } from '@primevue/forms/resolvers/zod'
import { z } from 'zod'
import type { FormInstance } from '@primevue/forms'

const resolvers = {
  name: zodResolver(z.string().min(1, { error: '请输入处理器名称' })),
  desc: zodResolver(z.string().min(1, { error: '请输入处理器描述' })),
  protocol: zodResolver(z.string().min(1, { error: '请选择协议' }))
}
const router = useRouter()
const visible = ref<boolean>()
const options = [
  {
    label: 'HTTP',
    value: 'HTTP'
  }
]
const currentProjectId = ref<string>()
const formRef = ref<FormInstance>()
const submit = () => {
  formRef.value?.validate().then(({ values, errors }) => {
    if (Object.keys(errors).length == 0) {
      if (currentProjectId.value) {
        processorAPI.createProcessor(currentProjectId.value, values).then((ok) => {
          bus.emit('message:success', '创建成功')
          router.push({ name: 'processorWorkflow', params: { processorId: ok.data.id } })
        })
      }
    }
  })
}
const open = (projectId: string) => {
  visible.value = true
  currentProjectId.value = projectId
}

const close = () => {
  visible.value = false
}

defineExpose({ open, close })
</script>
<style lang="scss" scoped></style>
