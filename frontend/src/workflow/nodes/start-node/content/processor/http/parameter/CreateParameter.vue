<template>
  <Dialog v-model:visible="visible" :handler="edit ? '修改' : '添加'" :style="{ width: '35rem' }">
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
        name="description"
        initial-value=""
        :resolver="resolvers.description"
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
        initial-value="query"
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
      <FormField
        v-slot="$field"
        class="mt-4"
        name="type"
        initial-value="string"
        :resolver="resolvers.location"
      >
        <label>类型</label>
        <SelectButton
          class="mt-2"
          option-label="label"
          option-value="value"
          :options="typenOptions"
          fluid
        />
        <Message v-if="$field?.invalid" severity="error" size="small" variant="simple">{{
          $field.error?.message
        }}</Message>
      </FormField>
      <FormField
        v-slot="$field"
        class="mt-4"
        name="many"
        :initial-value="false"
        :resolver="resolvers.required"
      >
        <label>多参数</label>
        <div class="mt-2">
          <ToggleSwitch />
        </div>

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
import { nextTick, ref } from 'vue'
import { zodResolver } from '@primevue/forms/resolvers/zod'
import { z } from 'zod'
import type { FormInstance } from '@primevue/forms'
const visible = ref<boolean>(false)
const locationOptions = [
  { label: 'Query', value: 'query' },
  { label: 'Path', value: 'path' }
]
const typenOptions = [
  { label: 'String', value: 'string' },
  { label: 'Integer', value: 'integer' },
  { label: 'Uuid', value: 'uuid' },
  { label: 'Long', value: 'long' },
  { label: 'Double', value: 'double' }
]

const resolvers = {
  field: zodResolver(z.string().min(1, { error: '请输入参数' })),
  description: zodResolver(z.string().min(1, { error: '请输入描述' })),
  required: zodResolver(z.boolean({ error: '请选择是否必填' })),
  location: zodResolver(z.string().min(1, { error: '请选择参数位置' })),
  type: zodResolver(z.string().min(1, { error: '请选择参数类型' })),
  many: zodResolver(z.boolean({ error: '请选择是否为多参数' }))
}
const formRef = ref<FormInstance>()
const emit = defineEmits(['submit'])
const edit = ref<boolean>(false)
const curentIndex = ref<number>()
const close = () => {
  visible.value = false
  edit.value = false
  curentIndex.value = undefined
}
const open = (row?: any, index?: number) => {
  visible.value = true
  if (row && index !== undefined) {
    edit.value = true
    curentIndex.value = index
    nextTick(() => {
      formRef.value?.setValues(row)
    })
  }
}

const submit = () => {
  formRef.value?.validate().then(({ values, errors }) => {
    if (Object.keys(errors).length == 0) {
      emit('submit', {
        edit: edit.value,
        index: curentIndex.value,
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
