<template>
  <div>
    <Fieldset legend="基本信息">
      <div class="mb-3">
        <div class="flex items-center justify-between mb-2">
          <label>模式</label>
          <SelectButton
            v-model="formData.location"
            :options="locationOptions"
            :option-label="(v: string) => locationLabels[v]"
            size="small"
          />
        </div>
      </div>

      <!-- tool_call 模式 -->
      <template v-if="formData.location === 'tool_call'">
        <div class="mb-3">
          <label class="mb-2 block">引用变量</label>
          <Cascader
            placeholder="请选择 tool_call 变量"
            :config="{ labelKey: 'label', valueKey: 'value' }"
            :options="fieldOptions"
            v-model="formData.reference"
            optionLabel="label"
            optionGroupChildren="children"
            class="w-full"
          />
          <Message v-if="errors.reference" severity="error" size="small" variant="simple">
            {{ errors.reference }}
          </Message>
        </div>
      </template>

      <!-- 自定义模式 -->
      <template v-else>
        <!-- 文件路径 -->
        <div class="mb-3">
          <div class="flex items-center justify-between mb-2">
            <label>文件路径</label>
            <SelectButton
              v-model="formData.pathLocation"
              :options="fieldLocationOptions"
              :option-label="(v: string) => fieldLocationLabels[v]"
              size="small"
            />
          </div>
          <Cascader
            v-if="formData.pathLocation === 'reference'"
            placeholder="请选择路径变量"
            :config="{ labelKey: 'label', valueKey: 'value' }"
            :options="fieldOptions"
            v-model="formData.pathReference"
            optionLabel="label"
            optionGroupChildren="children"
            class="w-full"
          />
          <Message v-if="formData.pathLocation === 'reference' && errors.pathReference" severity="error" size="small" variant="simple">
            {{ errors.pathReference }}
          </Message>
          <InputText
            v-if="formData.pathLocation === 'customize'"
            v-model="formData.path"
            placeholder="请输入服务器文件路径"
            class="w-full"
          />
          <Message v-if="formData.pathLocation === 'customize' && errors.path" severity="error" size="small" variant="simple">
            {{ errors.path }}
          </Message>
        </div>

        <!-- 文件名 -->
        <div class="mb-3">
          <label class="mb-2 block">文件名 <span class="text-xs text-muted-color font-normal">（可选）</span></label>
          <InputText
            v-model="formData.fileName"
            placeholder="留空则自动取原文件名"
            class="w-full"
          />
        </div>
      </template>
    </Fieldset>
  </div>
</template>

<script setup lang="ts">
import { reactive, inject, onMounted } from 'vue'
import Cascader from '@/components/cascader/index.vue'
import type { BaseNodeModel } from '@logicflow/core'
import { cloneDeep } from 'lodash'

const locationOptions = ['tool_call', 'customize']
const locationLabels: Record<string, string> = { tool_call: '工具调用', customize: '自定义' }
const fieldLocationOptions = ['reference', 'customize']
const fieldLocationLabels: Record<string, string> = { reference: '引用', customize: '自定义' }

const getModel = inject('getModel') as () => BaseNodeModel
const getNodeFieldOptions = inject('getNodeFieldOptions') as () => any[]
const model = getModel()
const fieldOptions = getNodeFieldOptions()

const formData = reactive({
  location: 'customize' as 'tool_call' | 'customize',
  reference: [] as string[],
  pathLocation: 'customize' as 'reference' | 'customize',
  pathReference: [] as string[],
  path: '',
  fileName: ''
})

const errors = reactive<Record<string, string>>({})

const validate = () => {
  Object.keys(errors).forEach((k) => delete errors[k])

  if (formData.location === 'tool_call') {
    if (!formData.reference || formData.reference.length === 0) {
      errors.reference = '请选择引用变量'
    }
  } else {
    if (formData.pathLocation === 'reference') {
      if (!formData.pathReference || formData.pathReference.length === 0) {
        errors.pathReference = '请选择路径变量'
      }
    } else {
      if (!formData.path || formData.path.trim() === '') {
        errors.path = '请输入文件路径'
      }
    }
  }

  const valid = Object.keys(errors).length === 0
  const values = cloneDeep({ ...formData })
  return Promise.resolve({ values, errors: valid ? {} : errors })
}

const submit = (): Promise<Record<string, string>> => {
  return validate().then(({ values, errors: errs }) => {
    if (Object.keys(errs).length === 0) {
      model.properties.nodeData = values
      return Promise.resolve({} as Record<string, string>)
    }
    return Promise.resolve(errs)
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
      fileName: data.fileName || ''
    })
  } else {
    model.properties.nodeData = {
      location: 'customize',
      reference: [],
      pathLocation: 'customize',
      pathReference: [],
      path: '',
      fileName: ''
    }
  }
  model.properties.field_list = [
    { label: '文件ID', value: 'fileId' },
    { label: '文件名', value: 'fileName' },
    { label: '文件大小', value: 'fileSize' }
  ]
})
</script>

<style lang="scss" scoped></style>
