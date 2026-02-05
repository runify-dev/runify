<template>
  <Dialog v-model:visible="visible" :handler="edit ? '修改' : '添加'" :style="{ width: '25rem' }">
    <Form ref="formRef">
      <FormField v-slot="$field" name="field" initial-value="" :resolver="resolvers.field">
        <IftaLabel>
          <InputText type="text" fluid />
          <Message v-if="$field?.invalid" severity="error" size="small" variant="simple">{{
            $field.error?.message
          }}</Message>
          <label>参数</label>
        </IftaLabel>
      </FormField>
      <FormField
        v-slot="$field"
        class="mt-4"
        name="desc"
        initial-value=""
        :resolver="resolvers.desc"
      >
        <IftaLabel>
          <InputText type="text" fluid />
          <Message v-if="$field?.invalid" severity="error" size="small" variant="simple">{{
            $field.error?.message
          }}</Message>
          <label>描述</label>
        </IftaLabel>
      </FormField>
      <FormField
        v-slot="$field"
        class="mt-4"
        name="required"
        :initial-value="false"
        :resolver="resolvers.required"
      >
        <label>必填</label>
        <div class="mt-2">
          <ToggleSwitch />
        </div>

        <Message v-if="$field?.invalid" severity="error" size="small" variant="simple">{{
          $field.error?.message
        }}</Message>
      </FormField>
      <FormField
        v-slot="$field"
        class="mt-4"
        name="location"
        initial-value="reference"
        :resolver="resolvers.location"
      >
        <label>位置</label>
        <SelectButton
          class="mt-2"
          option-label="label"
          option-value="value"
          :options="locationOptions"
          fluid
        />
        <Message v-if="$field?.invalid" severity="error" size="small" variant="simple">{{
          $field.error?.message
        }}</Message>
      </FormField>
    </Form>
    <template #footer>
      <div class="dialog-footer">
        <Button @click="close">取消</Button>
        <Button @click="submit"> 提交 </Button>
      </div>
    </template>
  </Dialog>
</template>
<script setup lang="ts">
import { ref } from 'vue'
import { zodResolver } from '@primevue/forms/resolvers/zod'
import { z } from 'zod'
import type { FormInstance } from '@primevue/forms'
const visible = ref<boolean>(false)
const locationOptions = [
  { label: '引用', value: 'reference' },
  { label: '自定义', value: 'customize' }
]
const resolvers = {
  field: zodResolver(z.string().min(1, { error: '请输入参数' })),
  desc: zodResolver(z.string().min(1, { error: '请输入描述' })),
  required: zodResolver(z.boolean({ error: '请选择是否必填' })),
  location: zodResolver(z.string().min(1, { error: '请选择参数位置' }))
}
// const typenOptions = ['string', 'integer', 'uuid', 'long', 'double']
interface Parameters {
  field: string
  description: string
  required: boolean
  location: 'reference' | 'customize'
  type: 'string' | 'integer' | 'uuid' | 'long' | 'double'
}
const defaultValue: Parameters = {
  field: '',
  description: '',
  required: false,
  location: 'reference',
  type: 'string'
}
const formRef = ref<FormInstance>()
const emit = defineEmits(['submit'])
const edit = ref<boolean>(false)
const form = ref<Parameters>({ ...defaultValue })
const close = () => {
  form.value = { ...defaultValue }
  visible.value = false
  edit.value = false
}
const open = (row?: Parameters) => {
  visible.value = true
  if (row) {
    form.value = row
    edit.value = true
  }
}

const submit = () => {
  formRef.value?.validate().then(({ values, errors }) => {
    if (Object.keys(errors).length == 0) {
      emit('submit', {
        edit: edit.value,
        row: values
      })
    }
  })
}

defineExpose({
  open,
  close
})
</script>
<style lang="scss" scoped></style>
