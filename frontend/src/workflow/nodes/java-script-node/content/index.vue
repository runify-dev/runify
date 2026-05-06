<template>
  <Form ref="formRef" :resolver="zodResolver(schema)">
    <Fieldset legend="基本信息">
      <FormField name="functionName" class="mt-2">
        <label>函数名称 </label>
        <InputText type="text" class="mt-2" fluid />
      </FormField>

      <FormField v-slot="$field: any" name="code" class="mt-2">
        <label>JavaScript </label>
        <CodeEditor
          class="mt-2"
          v-bind:model-value="$field.value"
          v-on:update:model-value="(v: any) => $field.onChange({ value: v })"
          title="JavaScript"
          lang="JAVASCRIPT"
        ></CodeEditor>
      </FormField>
    </Fieldset>
    <FormField v-slot="$field: any" name="parameters" :initial-value="[]">
      <Parameters
        ref="parametersRef"
        v-bind:parameters="$field.value"
        v-on:update:parameters="(v: any) => $field.onChange({ value: v })"
      ></Parameters>
    </FormField>
  </Form>
</template>
<script setup lang="ts">
import { ref, inject, onMounted } from 'vue'
import Parameters from '@/workflow/nodes/java-script-node/components/parameters/index.vue'
import CodeEditor from '@/components/code-editor/index.vue'
import type { BaseNodeModel } from '@logicflow/core'

import type { FormInstance } from '@primevue/forms'
import { zodResolver } from '@primevue/forms/resolvers/zod'
import { schema, validate as validateNodeData } from './validator'
const getModel = inject('getModel') as () => BaseNodeModel
const formRef = ref<FormInstance>()
const model = getModel()

const validate = () => {
  if (formRef.value) {
    return formRef.value.validate()
  }
  const result = validateNodeData(model.properties.nodeData)
  if (result.valid) {
    return Promise.resolve({ values: model.properties.nodeData, errors: {} })
  }
  return Promise.resolve({ values: {}, errors: result.errors })
}
const submit = () => {
  return validate().then(({ values, errors }) => {
    if (Object.keys(errors).length == 0) {
      model.properties.nodeData = values
      return Promise.resolve(values)
    }
    return Promise.resolve(errors)
  })
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
    model.properties.nodeData = {
      pool: [],
      template: '',
      parameters: []
    }
  }
  setField()
})
</script>
<style lang="scss" scoped></style>
