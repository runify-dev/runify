<template>
  <Form ref="formRef" v-slot="$form" :resolver="zodResolver(schema)">
    <Fieldset legend="审批提示">
      <FormField name="location" initial-value="customize">
        <div class="flex items-center justify-between mb-2">
          <SelectButton
            :options="locationOptions"
            option-label="label"
            option-value="value"
            size="small"
          />
        </div>
      </FormField>

      <!-- 引用模式 -->
      <FormField
        v-if="$form.location?.value === 'reference'"
        class="mt-2"
        v-slot="$field: any"
        name="reference"
        :initial-value="[]"
      >
        <label class="mb-1 block">选择变量</label>
        <Cascader
          placeholder="请选择提示内容变量"
          :config="{ labelKey: 'label', valueKey: 'value' }"
          :options="fieldOptions"
          :model-value="$field.value"
          @update:model-value="(v) => $field.onChange({ value: v })"
          optionLabel="label"
          optionGroupChildren="children"
          class="w-full"
        />
        <Message v-if="$field?.invalid" severity="error" size="small" variant="simple">
          {{ $field.error?.message }}
        </Message>
      </FormField>

      <!-- 自定义模式 -->
      <FormField
        v-if="$form.location?.value === 'customize'"
        class="mt-2"
        v-slot="$field: any"
        name="prompt"
        initial-value=""
      >
        <label class="mb-1 block">审批提示内容</label>
        <Textarea
          :model-value="$field.value"
          @update:model-value="(v) => $field.onChange({ value: v })"
          placeholder="请输入审批提示信息，告诉用户为什么需要审批"
          rows="4"
          class="w-full"
        />
        <Message v-if="$field?.invalid" severity="error" size="small" variant="simple">
          {{ $field.error?.message }}
        </Message>
      </FormField>
    </Fieldset>
  </Form>
</template>

<script setup lang="ts">
import { ref, inject, onMounted, nextTick } from 'vue'
import type { BaseNodeModel } from '@logicflow/core'
import type { FormInstance } from '@primevue/forms'
import { zodResolver } from '@primevue/forms/resolvers/zod'
import { schema, validate as validateNodeData } from './validator'
import Cascader from '@/components/cascader/index.vue'
import { locationOptions } from './type'
import { cloneDeep } from 'lodash'

const getModel = inject('getModel') as () => BaseNodeModel
const formRef = ref<FormInstance>()
const model = getModel()
const getNodeFieldOptions = inject('getNodeFieldOptions') as any
const fieldOptions = getNodeFieldOptions()

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

defineExpose({ validate, submit })

onMounted(() => {
  if (model.properties.nodeData) {
    const data = cloneDeep(model.properties.nodeData)
    formRef.value?.setFieldValue('location', data.location)
    nextTick(() => {
      formRef.value?.setValues(data)
    })
  } else {
    model.properties.nodeData = {
      location: 'customize',
      reference: [],
      prompt: ''
    }
  }
})
</script>

<style lang="scss" scoped></style>
