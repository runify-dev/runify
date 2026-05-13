<template>
  <div>
    <Fieldset legend="基本信息">
      <!-- Patch 来源 -->
      <div class="mb-3">
        <div class="flex items-center justify-between mb-2">
          <label>Patch 内容</label>
          <SelectButton
            v-model="formData.location"
            :options="locationOptions"
            option-label="label"
            option-value="value"
            size="small"
          />
        </div>

        <Cascader
          v-if="formData.location === 'reference'"
          placeholder="请选择 patch 变量"
          :config="{ labelKey: 'label', valueKey: 'value' }"
          :options="fieldOptions"
          v-model="formData.reference"
          optionLabel="label"
          optionGroupChildren="children"
          class="w-full"
        />
        <Message v-if="formData.location === 'reference' && errors.reference" severity="error" size="small" variant="simple">
          {{ errors.reference }}
        </Message>

        <Textarea
          v-if="formData.location === 'customize'"
          v-model="formData.patch"
          placeholder="请输入 git diff 格式的 patch 内容"
          rows="8"
          class="w-full font-mono text-sm"
        />
        <Message v-if="formData.location === 'customize' && errors.patch" severity="error" size="small" variant="simple">
          {{ errors.patch }}
        </Message>
      </div>
    </Fieldset>
  </div>
</template>

<script setup lang="ts">
import { ref, inject, onMounted, reactive } from 'vue'
import Cascader from '@/components/cascader/index.vue'
import type { BaseNodeModel } from '@logicflow/core'
import { locationOptions } from './type'
import { cloneDeep } from 'lodash'

const getModel = inject('getModel') as () => BaseNodeModel
const model = getModel()
const getNodeFieldOptions = inject('getNodeFieldOptions') as any
const fieldOptions = getNodeFieldOptions()

const formData = reactive({
  location: 'customize' as 'reference' | 'customize',
  reference: [] as string[],
  patch: ''
})

const errors = reactive<Record<string, string>>({})

function validate() {
  Object.keys(errors).forEach((k) => delete errors[k])

  if (formData.location === 'reference') {
    if (!formData.reference || formData.reference.length === 0) {
      errors.reference = '请选择 patch 变量'
    }
  }
  if (formData.location === 'customize') {
    if (!formData.patch || formData.patch.trim() === '') {
      errors.patch = '请输入 patch 内容'
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
      patch: data.patch || ''
    })
  } else {
    model.properties.nodeData = {
      location: 'customize',
      reference: [],
      patch: ''
    }
  }
})
</script>

<style lang="scss" scoped></style>
