<template>
  <div>
    <Fieldset legend="基本信息">
      <div class="mb-3">
        <div class="flex items-center justify-between mb-2">
          <label>模式</label>
          <SelectButton
            v-model="formData.location"
            :options="locationOptions"
            option-label="label"
            option-value="value"
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
        <!-- 文件ID -->
        <div class="mb-3">
          <div class="flex items-center justify-between mb-2">
            <label>文件ID</label>
            <SelectButton
              v-model="formData.fileIdLocation"
              :options="fieldLocationOptions"
              option-label="label"
              option-value="value"
              size="small"
            />
          </div>
          <Cascader
            v-if="formData.fileIdLocation === 'reference'"
            placeholder="请选择文件ID变量"
            :config="{ labelKey: 'label', valueKey: 'value' }"
            :options="fieldOptions"
            v-model="formData.fileIdReference"
            optionLabel="label"
            optionGroupChildren="children"
            class="w-full"
          />
          <Message v-if="formData.fileIdLocation === 'reference' && errors.fileIdReference" severity="error" size="small" variant="simple">
            {{ errors.fileIdReference }}
          </Message>
          <InputText
            v-if="formData.fileIdLocation === 'customize'"
            v-model="formData.fileId"
            placeholder="请输入文件ID"
            class="w-full"
          />
          <Message v-if="formData.fileIdLocation === 'customize' && errors.fileId" severity="error" size="small" variant="simple">
            {{ errors.fileId }}
          </Message>
        </div>

        <!-- 输出路径 -->
        <div class="mb-3">
          <div class="flex items-center justify-between mb-2">
            <label>输出路径（可选）</label>
            <SelectButton
              v-model="formData.pathLocation"
              :options="fieldLocationOptions"
              option-label="label"
              option-value="value"
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
          <InputText
            v-if="formData.pathLocation === 'customize'"
            v-model="formData.path"
            placeholder="留空则使用原文件名"
            class="w-full"
          />
        </div>
      </template>
    </Fieldset>
  </div>
</template>

<script setup lang="ts">
import { inject, onMounted, reactive } from 'vue'
import Cascader from '@/components/cascader/index.vue'
import type { BaseNodeModel } from '@logicflow/core'
import { locationOptions, fieldLocationOptions } from './type'
import { cloneDeep } from 'lodash'

const getModel = inject('getModel') as () => BaseNodeModel
const model = getModel()
const getNodeFieldOptions = inject('getNodeFieldOptions') as any
const fieldOptions = getNodeFieldOptions()

const formData = reactive({
  location: 'customize' as 'tool_call' | 'customize',
  reference: [] as string[],
  fileIdLocation: 'customize' as 'reference' | 'customize',
  fileIdReference: [] as string[],
  fileId: '',
  pathLocation: 'customize' as 'reference' | 'customize',
  pathReference: [] as string[],
  path: ''
})

const errors = reactive<Record<string, string>>({})

function validate() {
  Object.keys(errors).forEach((k) => delete errors[k])

  if (formData.location === 'tool_call') {
    if (!formData.reference || formData.reference.length === 0) {
      errors.reference = '请选择引用变量'
    }
  } else {
    if (formData.fileIdLocation === 'reference') {
      if (!formData.fileIdReference || formData.fileIdReference.length === 0) {
        errors.fileIdReference = '请选择文件ID变量'
      }
    } else {
      if (!formData.fileId || formData.fileId.trim() === '') {
        errors.fileId = '请输入文件ID'
      }
    }
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
      fileIdLocation: data.fileIdLocation || 'customize',
      fileIdReference: data.fileIdReference || [],
      fileId: data.fileId || '',
      pathLocation: data.pathLocation || 'customize',
      pathReference: data.pathReference || [],
      path: data.path || ''
    })
  } else {
    model.properties.nodeData = {
      location: 'customize',
      reference: [],
      fileIdLocation: 'customize',
      fileIdReference: [],
      fileId: '',
      pathLocation: 'customize',
      pathReference: [],
      path: ''
    }
  }
})
</script>

<style lang="scss" scoped></style>
