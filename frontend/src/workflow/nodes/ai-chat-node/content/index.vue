<template>
  <Form ref="formRef">
    <Fieldset legend="基本信息">
      <FormField class="mt-2" v-slot="$field: any" :initial-value="[]" name="modelId">
        <label>模型 </label>
        <Select
          :options="modelList"
          optionLabel="name"
          placeholder="请选择模型"
          optionValue="id"
          class="w-full mt-2"
        ></Select>
        <Message v-if="$field?.invalid" severity="error" size="small" variant="simple">{{
          $field.error?.message
        }}</Message>
      </FormField>
      <FormField class="mt-2" v-slot="$field: any" name="system">
        <label>系统提示词 </label>
        <TemplateEditor
          class="mt-2"
          v-bind:model-value="$field.value"
          @update:model-value="(v) => $field.onChange({ value: v })"
          :variables="variables"
          title="系统提示词"
          style="height: 200px"
        ></TemplateEditor>
        <Message v-if="$field?.invalid" severity="error" size="small" variant="simple">{{
          $field.error?.message
        }}</Message>
      </FormField>
      <FormField class="mt-2" v-slot="$field: any" name="user">
        <label>用户提示词 </label>
        <TemplateEditor
          class="mt-2"
          :variables="variables"
          v-bind:model-value="$field.value"
          @update:model-value="(v) => $field.onChange({ value: v })"
          title="用户提示词"
          style="height: 200px"
        ></TemplateEditor>
        <Message v-if="$field?.invalid" severity="error" size="small" variant="simple">{{
          $field.error?.message
        }}</Message>
      </FormField>
    </Fieldset>
  </Form>
</template>
<script setup lang="ts">
import { ref, inject, onMounted } from 'vue'
import Parameters from '@/workflow/nodes/database-search-node/components/parameters/index.vue'
import CodeEditor from '@/components/code-editor/index.vue'
import type { BaseNodeModel } from '@logicflow/core'
import TemplateEditor from '@/components/template-editor/index.vue'
import { TreeCommonAPI } from '@/api/tree'
const treeCommonAPI = new TreeCommonAPI('model')
import type { FormInstance } from '@primevue/forms'
const getModel = inject('getModel') as () => BaseNodeModel
const formRef = ref<FormInstance>()
const model = getModel()
const modelList = ref<Array<any>>()
const getTemplateVariables = inject('getTemplateVariables') as any
const variables = getTemplateVariables()

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
      label: '回答结果',
      value: 'content'
    },
    {
      label: '思考过程',
      value: 'reasoningContent'
    },
    {
      label: '拒绝原因文本',
      value: 'refusal'
    },
    { label: '是否拒绝回答', value: 'isRefusal' },
    {
      label: '工具调用',
      value: 'toolCalls'
    },
    {
      label: '结束原因',
      value: 'finishReason'
    }
  ]
}
defineExpose({ validate, submit, setField })
onMounted(() => {
  treeCommonAPI.listResource('root').then((ok) => {
    modelList.value = ok.data
  })
  if (model.properties.nodeData) {
    formRef.value?.setValues(JSON.parse(JSON.stringify(model.properties.nodeData)))
  } else {
    model.properties.nodeData = {
      modelId: '',
      system: '',
      user: ''
    }
  }
  setField()
})
</script>
<style lang="scss" scoped></style>
