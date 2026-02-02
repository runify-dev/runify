<template>
  <Form ref="formRef">
    <Fieldset legend="基本设置">
      <FormField v-slot="$field" name="chunk" :initial-value="false">
        <div><label>chunk响应</label></div>
        <ToggleSwitch class="mt-2" />
        <Message v-if="$field?.invalid" severity="error" size="small" variant="simple">{{
          $field.error?.message
        }}</Message>
      </FormField></Fieldset
    >

    <FormField v-slot="$field: any" name="parameters" :initial-value="[]">
      <Parameters
        ref="parametersRef"
        v-bind:parameters="$field.value"
        v-on:update:parameters="(v: any) => $field.onChange({ value: v })"
      ></Parameters>
      <Message v-if="$field?.invalid" severity="error" size="small" variant="simple">{{
        $field.error?.message
      }}</Message>
    </FormField>
  </Form>
</template>
<script setup lang="ts">
import { ref, inject, onMounted } from 'vue'
import Parameters from '@/workflow/nodes/json-response-node/components/parameters/index.vue'
import type { BaseNodeModel } from '@logicflow/core'
import type { FormInstance } from '@primevue/forms'

const getModel = inject('getModel') as () => BaseNodeModel
const formRef = ref<FormInstance>()
const model = getModel()

const validate = () => {
  return formRef.value ? formRef.value?.validate() : Promise.resolve(false)
}
const submit = () => {
  formRef.value?.validate().then(({ values, errors }) => {
    if (Object.keys(errors).length == 0) {
      model.properties.nodeData = values
    }
  })
  return Promise.resolve(true)
}
const setField = () => {
  model.properties.field_list = [
    {
      label: '结果',
      value: 'result'
    }
  ]
}
defineExpose({ validate, submit, setField })
onMounted(() => {
  if (model.properties.nodeData) {
    formRef.value?.setValues(JSON.parse(JSON.stringify(model.properties.nodeData)))
  } else {
    model.properties.nodeData = { parameters: [], chunk: false }
  }
  setField()
})
</script>
<style lang="scss" scoped></style>
