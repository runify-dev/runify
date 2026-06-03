<template>
  <Form ref="formRef" v-slot="$form" :resolver="zodResolver(schema)">
    <Fieldset legend="基本信息">
      <FormField name="runtime" initial-value="local">
        <div class="flex items-center justify-between mb-2">
          <label>运行环境</label>
          <SelectButton
            :options="runtimeOptions"
            option-label="label"
            option-value="value"
            size="small"
          />
        </div>
      </FormField>

      <FormField name="location" initial-value="customize">
        <div class="flex items-center justify-between mb-2">
          <label>模式</label>
          <SelectButton
            :options="locationOptions"
            option-label="label"
            option-value="value"
            size="small"
          />
        </div>
      </FormField>

      <!-- tool_call 模式 -->
      <FormField
        v-if="$form.location?.value === 'tool_call'"
        class="mt-2"
        v-slot="$field: any"
        name="reference"
        :initial-value="[]"
      >
        <label class="mb-1 block">选择 tool_call 变量</label>
        <Cascader
          placeholder="请选择 tool_call 变量"
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
      <template v-if="$form.location?.value === 'customize'">
        <!-- 代码 -->
        <FormField name="codeLocation" initial-value="customize">
          <div class="flex items-center justify-between mb-2 mt-2">
            <label>代码</label>
            <SelectButton
              :options="fieldLocationOptions"
              option-label="label"
              option-value="value"
              size="small"
            />
          </div>
        </FormField>

        <FormField
          v-if="$form.codeLocation?.value === 'reference'"
          class="mt-2"
          v-slot="$field: any"
          name="codeReference"
          :initial-value="[]"
        >
          <Cascader
            placeholder="请选择代码变量"
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

        <FormField
          v-if="$form.codeLocation?.value === 'customize'"
          class="mt-2"
          v-slot="$field: any"
          name="code"
          initial-value=""
        >
          <TemplateEditor
            :model-value="$field.value"
            @update:model-value="(v) => $field.onChange({ value: v })"
            :variables="variables"
            title="代码"
            style="height: 300px"
          />
          <Message v-if="$field?.invalid" severity="error" size="small" variant="simple">
            {{ $field.error?.message }}
          </Message>
        </FormField>

        <!-- 超时 -->
        <FormField name="timeoutLocation" initial-value="customize">
          <div class="flex items-center justify-between mb-2 mt-3">
            <label>超时时间（秒）</label>
            <SelectButton
              :options="fieldLocationOptions"
              option-label="label"
              option-value="value"
              size="small"
            />
          </div>
        </FormField>

        <FormField
          v-if="$form.timeoutLocation?.value === 'reference'"
          class="mt-2"
          v-slot="$field: any"
          name="timeoutReference"
          :initial-value="[]"
        >
          <Cascader
            placeholder="请选择超时变量"
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

        <FormField
          v-if="$form.timeoutLocation?.value === 'customize'"
          class="mt-2"
          v-slot="$field: any"
          name="timeout"
          :initial-value="30"
        >
          <InputNumber
            :model-value="$field.value"
            @update:model-value="(v) => $field.onChange({ value: v })"
            :min="1"
            :max="3600"
            placeholder="默认 30 秒"
            class="w-full"
          />
          <Message v-if="$field?.invalid" severity="error" size="small" variant="simple">
            {{ $field.error?.message }}
          </Message>
        </FormField>
      </template>
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
import TemplateEditor from '@/components/template-editor/index.vue'
import { runtimeOptions, locationOptions, fieldLocationOptions } from './type'
import { cloneDeep } from 'lodash'

const getModel = inject('getModel') as () => BaseNodeModel
const formRef = ref<FormInstance>()
const model = getModel()
const getNodeFieldOptions = inject('getNodeFieldOptions') as any
const getTemplateVariables = inject('getTemplateVariables') as any
const fieldOptions = getNodeFieldOptions()
const variables = getTemplateVariables()

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
    formRef.value?.setFieldValue('runtime', data.runtime || 'local')
    formRef.value?.setFieldValue('location', data.location)
    formRef.value?.setFieldValue('codeLocation', data.codeLocation || 'customize')
    formRef.value?.setFieldValue('timeoutLocation', data.timeoutLocation || 'customize')
    nextTick(() => {
      formRef.value?.setValues(data)
    })
  } else {
    model.properties.nodeData = {
      runtime: 'local',
      location: 'customize',
      reference: [],
      codeLocation: 'customize',
      codeReference: [],
      code: '',
      timeoutLocation: 'customize',
      timeoutReference: [],
      timeout: 30
    }
  }
})
</script>

<style lang="scss" scoped></style>
