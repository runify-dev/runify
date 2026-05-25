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

      <!-- Key -->
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

      <!-- Value -->
      <div class="mb-3">
        <div class="flex items-center justify-between mb-2">
          <label>Value</label>
          <SelectButton
            v-model="formData.valueLocation"
            :options="locationOptions"
            option-label="label"
            option-value="value"
            size="small"
          />
        </div>

        <Cascader
          v-if="formData.valueLocation === 'reference'"
          placeholder="请选择Value变量"
          :config="{ labelKey: 'label', valueKey: 'value' }"
          :options="fieldOptions"
          v-model="formData.valueReference"
          optionLabel="label"
          optionGroupChildren="children"
          class="w-full"
        />
        <Message v-if="formData.valueLocation === 'reference' && errors.valueReference" severity="error" size="small" variant="simple">
          {{ errors.valueReference }}
        </Message>

        <InputText
          v-if="formData.valueLocation === 'customize'"
          v-model="formData.value"
          placeholder="请输入缓存Value"
          class="w-full"
        />
        <Message v-if="formData.valueLocation === 'customize' && errors.value" severity="error" size="small" variant="simple">
          {{ errors.value }}
        </Message>
      </div>

      <!-- TTL -->
      <div class="mb-3">
        <label>过期时间（秒，可选）</label>
        <InputNumber
          v-model="formData.ttl"
          :min="0"
          placeholder="留空则不过期"
          class="w-full mt-2"
        />
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
import {ROOT_FOLDER_ID} from "@/constants/common.ts";

const getModel = inject('getModel') as () => BaseNodeModel
const model = getModel()
const getNodeFieldOptions = inject('getNodeFieldOptions') as any
const fieldOptions = getNodeFieldOptions()
const dataSourceList = ref<any>([])

databaseConnectionPoolAPI.listResource(ROOT_FOLDER_ID).then((ok) => {
  dataSourceList.value = ok.data
    .filter((item: any) => item.dataSourceType === 'CACHE')
    .map((item: any) => ({ label: item.name, value: item.id }))
})

const formData = reactive({
  cacheId: '',
  keyLocation: 'customize' as 'reference' | 'customize',
  keyReference: [] as string[],
  key: '',
  valueLocation: 'customize' as 'reference' | 'customize',
  valueReference: [] as string[],
  value: '',
  ttl: null as number | null
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

  if (formData.valueLocation === 'reference') {
    if (!formData.valueReference || formData.valueReference.length === 0) {
      errors.valueReference = '请选择Value变量'
    }
  }
  if (formData.valueLocation === 'customize') {
    if (!formData.value || formData.value.trim() === '') {
      errors.value = '请输入Value'
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
      key: data.key || '',
      valueLocation: data.valueLocation || 'customize',
      valueReference: data.valueReference || [],
      value: data.value || '',
      ttl: data.ttl ?? null
    })
  } else {
    model.properties.nodeData = {
      cacheId: '',
      keyLocation: 'customize',
      keyReference: [],
      key: '',
      valueLocation: 'customize',
      valueReference: [],
      value: '',
      ttl: null
    }
  }
})
</script>

<style lang="scss" scoped></style>
