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
        <!-- 文件路径 -->
        <div class="mb-3">
          <div class="flex items-center justify-between mb-2">
            <label>文件路径</label>
            <SelectButton v-model="formData.pathLocation" :options="fieldLocationOptions" option-label="label" option-value="value" size="small" />
          </div>
          <Cascader v-if="formData.pathLocation === 'reference'" placeholder="请选择变量" :config="{ labelKey: 'label', valueKey: 'value' }" :options="fieldOptions" v-model="formData.pathReference" optionLabel="label" optionGroupChildren="children" class="w-full" />
          <Message v-if="formData.pathLocation === 'reference' && errors.pathReference" severity="error" size="small" variant="simple">{{ errors.pathReference }}</Message>
          <InputText v-if="formData.pathLocation === 'customize'" v-model="formData.path" placeholder="如 src/new-file.ts" class="w-full" />
          <Message v-if="formData.pathLocation === 'customize' && errors.path" severity="error" size="small" variant="simple">{{ errors.path }}</Message>
        </div>

        <!-- 文件内容 -->
        <div class="mb-3">
          <div class="flex items-center justify-between mb-2">
            <label>文件内容</label>
            <SelectButton v-model="formData.contentLocation" :options="fieldLocationOptions" option-label="label" option-value="value" size="small" />
          </div>
          <Cascader v-if="formData.contentLocation === 'reference'" placeholder="请选择变量" :config="{ labelKey: 'label', valueKey: 'value' }" :options="fieldOptions" v-model="formData.contentReference" optionLabel="label" optionGroupChildren="children" class="w-full" />
          <Message v-if="formData.contentLocation === 'reference' && errors.contentReference" severity="error" size="small" variant="simple">{{ errors.contentReference }}</Message>
          <Textarea v-if="formData.contentLocation === 'customize'" v-model="formData.content" placeholder="请输入文件内容" rows="6" class="w-full" />
          <Message v-if="formData.contentLocation === 'customize' && errors.content" severity="error" size="small" variant="simple">{{ errors.content }}</Message>
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

  pathLocation: 'customize' as 'reference' | 'customize',
  pathReference: [] as string[],
  path: '',

  contentLocation: 'customize' as 'reference' | 'customize',
  contentReference: [] as string[],
  content: ''
})

const errors = reactive<Record<string, string>>({})

function validate() {
  Object.keys(errors).forEach((k) => delete errors[k])
  if (formData.location === 'tool_call') {
    if (!formData.reference || formData.reference.length === 0) errors.reference = '请选择引用变量'
  } else {
    if (formData.pathLocation === 'reference' && (!formData.pathReference || formData.pathReference.length === 0)) errors.pathReference = '请选择文件路径变量'
    if (formData.pathLocation === 'customize' && (!formData.path || formData.path.trim() === '')) errors.path = '请输入文件路径'
    if (formData.contentLocation === 'reference' && (!formData.contentReference || formData.contentReference.length === 0)) errors.contentReference = '请选择文件内容变量'
    if (formData.contentLocation === 'customize' && (!formData.content || formData.content.trim() === '')) errors.content = '请输入文件内容'
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
      pathLocation: data.pathLocation || 'customize',
      pathReference: data.pathReference || [],
      path: data.path || '',
      contentLocation: data.contentLocation || 'customize',
      contentReference: data.contentReference || [],
      content: data.content || ''
    })
  } else {
    model.properties.nodeData = {
      location: 'customize', reference: [],
      pathLocation: 'customize', pathReference: [], path: '',
      contentLocation: 'customize', contentReference: [], content: ''
    }
  }
})
</script>

<style lang="scss" scoped></style>
