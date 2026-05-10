<template>
  <div>
    <Fieldset legend="基本信息">
      <div class="mb-3">
        <label>缓存连接</label>
        <Select
          v-model="formData.cacheId"
          placeholder="请选择缓存连接"
          :options="dataSourceList"
          optionLabel="label"
          option-value="value"
          class="w-full mt-2"
        />
        <Message v-if="errors.cacheId" severity="error" size="small" variant="simple">
          {{ errors.cacheId }}
        </Message>
      </div>

      <div class="mb-3">
        <div class="flex items-center justify-between mb-2">
          <label>Key</label>
          <SelectButton
            v-model="formData.keyLocation"
            :options="locationOptions"
            option-label="label"
            option-value="value"
            size="small"
          />
        </div>

        <Cascader
          v-if="formData.keyLocation === 'reference'"
          placeholder="请选择Key变量"
          :config="{ labelKey: 'label', valueKey: 'value' }"
          :options="fieldOptions"
          v-model="formData.keyReference"
          optionLabel="label"
          optionGroupChildren="children"
          class="w-full"
        />
        <Message v-if="formData.keyLocation === 'reference' && errors.keyReference" severity="error" size="small" variant="simple">
          {{ errors.keyReference }}
        </Message>

        <InputText
          v-if="formData.keyLocation === 'customize'"
          v-model="formData.key"
          placeholder="请输入缓存Key"
          class="w-full"
        />
        <Message v-if="formData.keyLocation === 'customize' && errors.key" severity="error" size="small" variant="simple">
          {{ errors.key }}
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
import databaseConnectionPoolAPI from '@/api/database-connection-pool.ts'
import { cloneDeep } from 'lodash'

const getModel = inject('getModel') as () => BaseNodeModel
const model = getModel()
const getNodeFieldOptions = inject('getNodeFieldOptions') as any
const fieldOptions = getNodeFieldOptions()
const dataSourceList = ref<any>([])

databaseConnectionPoolAPI.listResource('root').then((ok) => {
  dataSourceList.value = ok.data
    .filter((item: any) => item.dataSourceType === 'CACHE')
    .map((item: any) => ({ label: item.name, value: item.id }))
})

const formData = reactive({
  cacheId: '',
  keyLocation: 'customize' as 'reference' | 'customize',
  keyReference: [] as string[],
  key: ''
})

const errors = reactive<Record<string, string>>({})

function validate() {
  Object.keys(errors).forEach((k) => delete errors[k])

  if (!formData.cacheId) {
    errors.cacheId = '请选择缓存连接'
  }

  if (formData.keyLocation === 'reference') {
    if (!formData.keyReference || formData.keyReference.length === 0) {
      errors.keyReference = '请选择Key变量'
    }
  }

  if (formData.keyLocation === 'customize') {
    if (!formData.key || formData.key.trim() === '') {
      errors.key = '请输入Key'
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
      cacheId: data.cacheId || '',
      keyLocation: data.keyLocation || 'customize',
      keyReference: data.keyReference || [],
      key: data.key || ''
    })
  } else {
    model.properties.nodeData = {
      cacheId: '',
      keyLocation: 'customize',
      keyReference: [],
      key: ''
    }
  }
})
</script>

<style lang="scss" scoped></style>
