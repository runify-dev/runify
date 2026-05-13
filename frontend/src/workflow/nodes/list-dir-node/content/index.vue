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
        <div class="mb-3">
          <div class="flex items-center justify-between mb-2">
            <label>目录路径</label>
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
            placeholder="如 src/"
            class="w-full"
          />
          <Message v-if="formData.pathLocation === 'customize' && errors.path" severity="error" size="small" variant="simple">
            {{ errors.path }}
          </Message>
        </div>

        <div class="mb-3">
          <div class="flex items-center justify-between mb-2">
            <label>最大深度（可选）</label>
            <SelectButton
              v-model="formData.depthLocation"
              :options="fieldLocationOptions"
              option-label="label"
              option-value="value"
              size="small"
            />
          </div>
          <Cascader
            v-if="formData.depthLocation === 'reference'"
            placeholder="请选择深度变量"
            :config="{ labelKey: 'label', valueKey: 'value' }"
            :options="fieldOptions"
            v-model="formData.depthReference"
            optionLabel="label"
            optionGroupChildren="children"
            class="w-full"
          />
          <InputNumber
            v-if="formData.depthLocation === 'customize'"
            v-model="formData.depth"
            :min="1"
            :max="10"
            placeholder="默认 3"
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
  depthLocation: 'customize' as 'reference' | 'customize',
  depthReference: [] as string[],
  depth: null as number | null
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
        errors.pathReference = '请选择目录路径变量'
      }
    }
    if (formData.pathLocation === 'customize') {
      if (!formData.path || formData.path.trim() === '') {
        errors.path = '请输入目录路径'
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
      depthLocation: data.depthLocation || 'customize',
      depthReference: data.depthReference || [],
      depth: data.depth ?? null
    })
  } else {
    model.properties.nodeData = {
      location: 'customize',
      reference: [],
      pathLocation: 'customize',
      pathReference: [],
      path: '',
      depthLocation: 'customize',
      depthReference: [],
      depth: null
    }
  }
})
</script>

<style lang="scss" scoped></style>
