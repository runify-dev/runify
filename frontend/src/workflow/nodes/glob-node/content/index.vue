<template>
  <div>
    <Fieldset legend="基本信息">
      <div class="mb-3">
        <div class="flex items-center justify-between mb-2">
          <label>模式</label>
          <SelectButton v-model="formData.location" :options="locationOptions" option-label="label" option-value="value" size="small" />
        </div>
      </div>

      <template v-if="formData.location === 'tool_call'">
        <div class="mb-3">
          <label class="mb-2 block">引用变量</label>
          <Cascader placeholder="请选择 tool_call 变量" :config="{ labelKey: 'label', valueKey: 'value' }" :options="fieldOptions" v-model="formData.reference" optionLabel="label" optionGroupChildren="children" class="w-full" />
          <Message v-if="errors.reference" severity="error" size="small" variant="simple">{{ errors.reference }}</Message>
        </div>
      </template>

      <template v-else>
        <div class="mb-3">
          <div class="flex items-center justify-between mb-2">
            <label>Glob 模式</label>
            <SelectButton v-model="formData.patternLocation" :options="fieldLocationOptions" option-label="label" option-value="value" size="small" />
          </div>
          <Cascader v-if="formData.patternLocation === 'reference'" placeholder="请选择变量" :config="{ labelKey: 'label', valueKey: 'value' }" :options="fieldOptions" v-model="formData.patternReference" optionLabel="label" optionGroupChildren="children" class="w-full" />
          <Message v-if="formData.patternLocation === 'reference' && errors.patternReference" severity="error" size="small" variant="simple">{{ errors.patternReference }}</Message>
          <InputText v-if="formData.patternLocation === 'customize'" v-model="formData.pattern" placeholder="如 **/*.tsx" class="w-full" />
          <Message v-if="formData.patternLocation === 'customize' && errors.pattern" severity="error" size="small" variant="simple">{{ errors.pattern }}</Message>
        </div>

        <div class="mb-3">
          <div class="flex items-center justify-between mb-2">
            <label>搜索路径（可选）</label>
            <SelectButton v-model="formData.pathLocation" :options="fieldLocationOptions" option-label="label" option-value="value" size="small" />
          </div>
          <Cascader v-if="formData.pathLocation === 'reference'" placeholder="请选择变量" :config="{ labelKey: 'label', valueKey: 'value' }" :options="fieldOptions" v-model="formData.pathReference" optionLabel="label" optionGroupChildren="children" class="w-full" />
          <InputText v-if="formData.pathLocation === 'customize'" v-model="formData.path" placeholder="如 src/" class="w-full" />
        </div>

        <div class="mb-3">
          <div class="flex items-center justify-between mb-2">
            <label>最大结果数（可选）</label>
            <SelectButton v-model="formData.maxResultsLocation" :options="fieldLocationOptions" option-label="label" option-value="value" size="small" />
          </div>
          <Cascader v-if="formData.maxResultsLocation === 'reference'" placeholder="请选择" :config="{ labelKey: 'label', valueKey: 'value' }" :options="fieldOptions" v-model="formData.maxResultsReference" optionLabel="label" optionGroupChildren="children" class="w-full" />
          <InputNumber v-if="formData.maxResultsLocation === 'customize'" v-model="formData.maxResults" :min="1" placeholder="默认 1000" class="w-full" />
        </div>
      </template>
    </Fieldset>
  </div>
</template>

<script setup lang="ts">
import { inject, onMounted, reactive } from 'vue'
import Cascader from '@/components/cascader/index.vue'
import type { BaseNodeModel } from '@logicflow/core'
import { cloneDeep } from 'lodash'

const locationOptions = [
  { label: '工具调用', value: 'tool_call' },
  { label: '自定义', value: 'customize' }
]
const fieldLocationOptions = [
  { label: '引用', value: 'reference' },
  { label: '自定义', value: 'customize' }
]

const getModel = inject('getModel') as () => BaseNodeModel
const model = getModel()
const getNodeFieldOptions = inject('getNodeFieldOptions') as any
const fieldOptions = getNodeFieldOptions()

const formData = reactive({
  location: 'customize' as 'tool_call' | 'customize',
  reference: [] as string[],
  patternLocation: 'customize' as 'reference' | 'customize',
  patternReference: [] as string[],
  pattern: '',
  pathLocation: 'customize' as 'reference' | 'customize',
  pathReference: [] as string[],
  path: '',
  maxResultsLocation: 'customize' as 'reference' | 'customize',
  maxResultsReference: [] as string[],
  maxResults: null as number | null
})

const errors = reactive<Record<string, string>>({})

function validate() {
  Object.keys(errors).forEach((k) => delete errors[k])
  if (formData.location === 'tool_call') {
    if (!formData.reference || formData.reference.length === 0) errors.reference = '请选择引用变量'
  } else {
    if (formData.patternLocation === 'reference' && (!formData.patternReference || formData.patternReference.length === 0)) errors.patternReference = '请选择 glob 模式变量'
    if (formData.patternLocation === 'customize' && (!formData.pattern || formData.pattern.trim() === '')) errors.pattern = '请输入 glob 模式'
  }
  const valid = Object.keys(errors).length === 0
  return Promise.resolve({ values: cloneDeep({ ...formData }), errors: valid ? {} : errors })
}

function submit() {
  return validate().then(({ values, errors: errs }) => {
    if (Object.keys(errs).length === 0) {
      model.properties.nodeData = values
      return {} as Record<string, string>
    }
    return errs
  })
}

defineExpose({ validate, submit })

onMounted(() => {
  if (model.properties.nodeData) {
    const data = cloneDeep(model.properties.nodeData)
    Object.assign(formData, {
      location: data.location || 'customize',
      reference: data.reference || [],
      patternLocation: data.patternLocation || 'customize',
      patternReference: data.patternReference || [],
      pattern: data.pattern || '',
      pathLocation: data.pathLocation || 'customize',
      pathReference: data.pathReference || [],
      path: data.path || '',
      maxResultsLocation: data.maxResultsLocation || 'customize',
      maxResultsReference: data.maxResultsReference || [],
      maxResults: data.maxResults ?? null
    })
  } else {
    model.properties.nodeData = {
      location: 'customize', reference: [],
      patternLocation: 'customize', patternReference: [], pattern: '',
      pathLocation: 'customize', pathReference: [], path: '',
      maxResultsLocation: 'customize', maxResultsReference: [], maxResults: null
    }
  }
})
</script>

<style lang="scss" scoped></style>
