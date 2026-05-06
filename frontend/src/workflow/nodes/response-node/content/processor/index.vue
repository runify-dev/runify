<template>
  <Form ref="formRef" v-slot="$form" :resolver="zodResolver(schema)">
    <Fieldset legend="基本设置">
      <FormField class="mt-2" v-slot="$field" name="status" :initial-value="200">
        <div><label>状态码</label></div>
        <InputNumber name="amount" fluid showButtons />
        <Message v-if="$field?.invalid" severity="error" size="small" variant="simple">{{
          $field.error?.message
        }}</Message>
      </FormField>
    </Fieldset>
    <FormField
      v-slot="$field: any"
      name="headers"
      :initial-value="[
        { field: 'content-type', value: 'application/json; charset=utf-8', location: 'customize' }
      ]"
    >
      <Handlers
        ref="handlersRef"
        v-bind:parameters="$field.value"
        v-on:update:parameters="(v: any) => $field.onChange({ value: v })"
      ></Handlers>
      <Message v-if="$field?.invalid" severity="error" size="small" variant="simple">{{
        $field.error?.message
      }}</Message>
    </FormField>

    <Fieldset legend="响应体">
      <FormField name="contentType" initial-value="jsonFields">
        <label>响应类型</label>
        <Select
          class="mt-2"
          :options="options"
          option-label="label"
          option-value="value"
          placeholder="请选择响应类型"
          fluid
        />
      </FormField>
      <FormField
        v-if="$form.contentType?.value === 'jsonFields'"
        class="mt-2"
        v-slot="$field: any"
        name="jsonFields"
        :initial-value="[]"
      >
        <Parameters
          ref="parametersRef"
          v-bind:parameters="$field.value"
          v-on:update:parameters="(v: any) => $field.onChange({ value: v })"
        ></Parameters>
        <Message v-if="$field?.invalid" severity="error" size="small" variant="simple">{{
          $field.error?.message
        }}</Message>
      </FormField>

      <FormField
        v-if="$form.contentType?.value === 'jsonObject'"
        class="mt-2"
        v-slot="$field: any"
        name="jsonObject"
        :initial-value="{ value: '', location: 'customize' }"
      >
        <JsonObject
          v-bind:modelValue="$field.value"
          v-on:update:modelValue="(v: any) => $field.onChange({ value: v })"
        >
        </JsonObject>
        <Message v-if="$field?.invalid" severity="error" size="small" variant="simple">{{
          $field.error?.message
        }}</Message>
      </FormField>
      <FormField
        v-if="$form.contentType?.value === 'plainText'"
        class="mt-2"
        v-slot="$field: any"
        name="plainText"
        :initial-value="{ value: '', location: 'customize' }"
      >
        <PlainText
          v-bind:modelValue="$field.value"
          v-on:update:modelValue="(v: any) => $field.onChange({ value: v })"
        >
        </PlainText>
        <Message v-if="$field?.invalid" severity="error" size="small" variant="simple">{{
          $field.error?.message
        }}</Message>
      </FormField>
    </Fieldset>
  </Form>
</template>
<script setup lang="ts">
import { ref, inject, onMounted, nextTick } from 'vue'
import Parameters from '@/workflow/nodes/response-node/components/parameters/index.vue'
import Handlers from '@/workflow/nodes/response-node/components/handlers/index.vue'
import type { BaseNodeModel } from '@logicflow/core'
import type { FormInstance } from '@primevue/forms'
import JsonObject from '@/workflow/nodes/response-node/components/json-object/index.vue'
import PlainText from '@/workflow/nodes/response-node/components/plain-text/index.vue'
import { zodResolver } from '@primevue/forms/resolvers/zod'
import { schema, validate as validateNodeData } from './validator'
const options = [
  { label: '字段配置', value: 'jsonFields' },
  { label: 'JSON对象', value: 'jsonObject' },
  { label: '文本', value: 'plainText' }
]

const getModel = inject('getModel') as () => BaseNodeModel
const formRef = ref<FormInstance>()
const model = getModel()
const handlersRef = ref<InstanceType<typeof Handlers>>()
const parametersRef = ref<InstanceType<typeof Parameters>>()
const validate = () => {
  if (formRef.value) {
    const handler = []
    handler.push(formRef.value.validate())
    if (handlersRef.value) {
      handler.push(handlersRef.value.validate())
    }
    if (parametersRef.value) {
      handler.push(parametersRef.value.validate())
    }
    return Promise.all(handler).then((ok) => {
      return ok.reduce(
        (x, y) => ({
          errors: { ...x?.errors, ...y?.errors },
          values: { ...x?.values, ...y?.values }
        }),
        { values: [], errors: [] }
      ) as any
    })
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
    return Promise.reject(errors)
  })
}
defineExpose({ validate, submit })
onMounted(() => {
  if (model.properties.nodeData) {
    formRef.value?.setValues(JSON.parse(JSON.stringify(model.properties.nodeData)))
    nextTick(() => {
      formRef.value?.setValues(JSON.parse(JSON.stringify(model.properties.nodeData)))
    })
  } else {
    model.properties.nodeData = { parameters: [], chunk: false }
  }
})
</script>
<style lang="scss" scoped></style>
