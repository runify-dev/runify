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

      <!-- customize 模式 -->
      <template v-else>
        <!-- 文件路径 -->
        <div class="mb-3">
          <div class="flex items-center justify-between mb-2">
            <label>文件路径</label>
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
          <Message v-if="formData.pathLocation === 'reference' && errors.pathReference" severity="error" size="small" variant="simple">
            {{ errors.pathReference }}
          </Message>
          <InputText
            v-if="formData.pathLocation === 'customize'"
            v-model="formData.path"
            placeholder="如 src/api.ts"
            class="w-full"
          />
          <Message v-if="formData.pathLocation === 'customize' && errors.path" severity="error" size="small" variant="simple">
            {{ errors.path }}
          </Message>
        </div>

        <!-- 偏移行号 -->
        <div class="mb-3">
          <div class="flex items-center justify-between mb-2">
            <label>起始行（可选）</label>
            <SelectButton
              v-model="formData.offsetLocation"
              :options="fieldLocationOptions"
              option-label="label"
              option-value="value"
              size="small"
            />
          </div>
          <Cascader
            v-if="formData.offsetLocation === 'reference'"
            placeholder="请选择偏移变量"
            :config="{ labelKey: 'label', valueKey: 'value' }"
            :options="fieldOptions"
            v-model="formData.offsetReference"
            optionLabel="label"
            optionGroupChildren="children"
            class="w-full"
          />
          <InputNumber
            v-if="formData.offsetLocation === 'customize'"
            v-model="formData.offset"
            :min="0"
            placeholder="默认 0"
            class="w-full"
          />
        </div>

        <!-- 读取行数 -->
        <div class="mb-3">
          <div class="flex items-center justify-between mb-2">
            <label>读取行数（可选）</label>
            <SelectButton
              v-model="formData.limitLocation"
              :options="fieldLocationOptions"
              option-label="label"
              option-value="value"
              size="small"
            />
          </div>
          <Cascader
            v-if="formData.limitLocation === 'reference'"
            placeholder="请选择行数变量"
            :config="{ labelKey: 'label', valueKey: 'value' }"
            :options="fieldOptions"
            v-model="formData.limitReference"
            optionLabel="label"
            optionGroupChildren="children"
            class="w-full"
          />
          <InputNumber
            v-if="formData.limitLocation === 'customize'"
            v-model="formData.limit"
            :min="1"
            placeholder="默认全部"
            class="w-full"
          />
        </div>
      </template>
    </Fieldset>
  </div>
</template>

<script setup lang="ts">
import { ref, inject, onMounted, reactive } from 'vue'
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

  pathLocation: 'customize' as 'reference' | 'customize',
  pathReference: [] as string[],
  path: '',

  offsetLocation: 'customize' as 'reference' | 'customize',
  offsetReference: [] as string[],
  offset: null as number | null,

  limitLocation: 'customize' as 'reference' | 'customize',
  limitReference: [] as string[],
  limit: null as number | null
})

const errors = reactive<Record<string, string>>({})

function validate() {
  Object.keys(errors).forEach((k) => delete errors[k])

  if (formData.location === 'tool_call') {
    if (!formData.reference || formData.reference.length === 0) {
      errors.reference = '请选择引用变量'
    }
  } else {
    if (formData.pathLocation === 'reference') {
      if (!formData.pathReference || formData.pathReference.length === 0) {
        errors.pathReference = '请选择文件路径变量'
      }
    }
    if (formData.pathLocation === 'customize') {
      if (!formData.path || formData.path.trim() === '') {
        errors.path = '请输入文件路径'
      }
    }
  }

  const valid = Object.keys(errors).length === 0
  const values = cloneDeep({ ...formData })
  return Promise.resolve({ values, errors: valid ? {} : errors })
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
      offsetLocation: data.offsetLocation || 'customize',
      offsetReference: data.offsetReference || [],
      offset: data.offset ?? null,
      limitLocation: data.limitLocation || 'customize',
      limitReference: data.limitReference || [],
      limit: data.limit ?? null
    })
  } else {
    model.properties.nodeData = {
      location: 'customize',
      reference: [],
      pathLocation: 'customize',
      pathReference: [],
      path: '',
      offsetLocation: 'customize',
      offsetReference: [],
      offset: null,
      limitLocation: 'customize',
      limitReference: [],
      limit: null
    }
  }
})
</script>

<style lang="scss" scoped></style>
