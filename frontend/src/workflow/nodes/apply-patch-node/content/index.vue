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
        <!-- 工作目录 -->
        <div class="mb-3">
          <div class="flex items-center justify-between mb-2">
            <label>工作目录</label>
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
            placeholder="请选择目录变量"
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
            placeholder="留空则使用项目根目录"
            class="w-full"
          />
        </div>

        <!-- Patch 内容 -->
        <div class="mb-3">
          <div class="flex items-center justify-between mb-2">
            <label>Patch 内容</label>
            <SelectButton
              v-model="formData.patchLocation"
              :options="fieldLocationOptions"
              option-label="label"
              option-value="value"
              size="small"
            />
          </div>
          <Cascader
            v-if="formData.patchLocation === 'reference'"
            placeholder="请选择 patch 变量"
            :config="{ labelKey: 'label', valueKey: 'value' }"
            :options="fieldOptions"
            v-model="formData.patchReference"
            optionLabel="label"
            optionGroupChildren="children"
            class="w-full"
          />
          <Message v-if="formData.patchLocation === 'reference' && errors.patchReference" severity="error" size="small" variant="simple">
            {{ errors.patchReference }}
          </Message>
          <Textarea
            v-if="formData.patchLocation === 'customize'"
            v-model="formData.patch"
            placeholder="请输入 git diff 格式的 patch 内容"
            rows="8"
            class="w-full font-mono text-sm"
          />
          <Message v-if="formData.patchLocation === 'customize' && errors.patch" severity="error" size="small" variant="simple">
            {{ errors.patch }}
          </Message>
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
  pathLocation: 'customize' as 'reference' | 'customize',
  pathReference: [] as string[],
  path: '',
  patchLocation: 'customize' as 'reference' | 'customize',
  patchReference: [] as string[],
  patch: ''
})

const errors = reactive<Record<string, string>>({})

function validate() {
  Object.keys(errors).forEach((k) => delete errors[k])

  if (formData.location === 'tool_call') {
    if (!formData.reference || formData.reference.length === 0) {
      errors.reference = '请选择引用变量'
    }
  } else {
    if (formData.patchLocation === 'reference') {
      if (!formData.patchReference || formData.patchReference.length === 0) {
        errors.patchReference = '请选择 patch 变量'
      }
    } else {
      if (!formData.patch || formData.patch.trim() === '') {
        errors.patch = '请输入 patch 内容'
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
      patchLocation: data.patchLocation || 'customize',
      patchReference: data.patchReference || [],
      patch: data.patch || ''
    })
  } else {
    model.properties.nodeData = {
      location: 'customize',
      reference: [],
      pathLocation: 'customize',
      pathReference: [],
      path: '',
      patchLocation: 'customize',
      patchReference: [],
      patch: ''
    }
  }
})
</script>

<style lang="scss" scoped></style>
