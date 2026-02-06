<template>
  <Form ref="formRef">
    <Fieldset legend="基本信息">
      <FormField v-slot="$field: any" :initial-value="[]" name="pool">
        <label>数据库连接池 </label>
        <Cascader
          placeholder="请选择数据库连接池"
          :config="{ labelKey: 'label', valueKey: 'value' }"
          :options="options"
          v-bind:model-value="$field.value"
          v-on:update:model-value="(v) => $field.onChange({ value: v })"
          optionLabel="label"
          optionGroupChildren="children"
          class="w-full mt-2"
        />
      </FormField>
      <FormField v-slot="$field: any" name="template" class="mt-2">
        <label>Sql </label>
        <CodeEditor
          class="mt-2"
          v-bind:model-value="$field.value"
          v-on:update:model-value="(v: any) => $field.onChange({ value: v })"
          title="SQL"
          lang="SQL"
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
import Parameters from '@/workflow/nodes/database-search-node/components/parameters/index.vue'
import CodeEditor from '@/components/code-editor/index.vue'
import type { BaseNodeModel } from '@logicflow/core'

import Cascader from '@/components/cascader/index.vue'
import type { FormInstance } from '@primevue/forms'
const getModel = inject('getModel') as () => BaseNodeModel
const formRef = ref<FormInstance>()
const model = getModel()
const getNodeFieldOptions = inject('getNodeFieldOptions') as any
const options = getNodeFieldOptions()

const validate = () => {
  return formRef.value ? formRef.value.validate() : Promise.reject({ values: [], errors: [] })
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
