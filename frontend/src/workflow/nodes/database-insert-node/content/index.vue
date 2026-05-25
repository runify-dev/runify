<template>
  <div>
    <Fieldset legend="基本信息">
      <div class="mb-3">
        <label>数据库连接池</label>
        <Select
          v-model="formData.poolId"
          placeholder="请选择数据库连接池"
          :options="dataSourceList"
          optionLabel="label"
          option-value="value"
          class="w-full mt-2"
        />
        <Message v-if="errors.poolId" severity="error" size="small" variant="simple">
          {{ errors.poolId }}
        </Message>
      </div>

      <div class="mb-3">
        <div class="flex items-center justify-between mb-2">
          <label>Sql</label>
          <SelectButton
            v-model="formData.location"
            :options="locationOptions"
            option-label="label"
            option-value="value"
            size="small"
          />
        </div>

        <!-- 引用模式 -->
        <Cascader
          v-if="formData.location === 'reference'"
          placeholder="请选择SQL变量"
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

        <!-- 自定义模式 -->
        <CodeEditor
          v-if="formData.location === 'customize'"
          class="mt-2"
          v-model="formData.template"
          title="SQL"
          lang="SQL"
        />
        <Message v-if="formData.location === 'customize' && errors.template" severity="error" size="small" variant="simple">
          {{ errors.template }}
        </Message>
      </div>
    </Fieldset>

    <Parameters
      ref="parametersRef"
      v-model:parameters="formData.parameters"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, inject, onMounted, reactive } from 'vue'
import Parameters from '@/workflow/nodes/database-search-node/components/parameters/index.vue'
import CodeEditor from '@/components/code-editor/index.vue'
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
    .filter((item: any) => item.dataSourceType === 'SQL')
    .map((item: any) => ({ label: item.name, value: item.id }))
})

const formData = reactive({
  poolId: '',
  location: 'customize' as 'reference' | 'customize',
  reference: [] as string[],
  template: '',
  parameters: [] as any[]
})

const errors = reactive<Record<string, string>>({})

function validate() {
  Object.keys(errors).forEach((k) => delete errors[k])

  if (!formData.poolId) {
    errors.poolId = '请选择数据库连接池'
  }

  if (formData.location === 'reference') {
    if (!formData.reference || formData.reference.length === 0) {
      errors.reference = '请选择引用变量'
    }
  }

  if (formData.location === 'customize') {
    if (!formData.template || formData.template.trim() === '') {
      errors.template = '请输入SQL'
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

function setField() {
  model.properties.field_list = [{ label: '影响行数', value: 'affectedRows' }]
}

defineExpose({ validate, submit, setField })

onMounted(() => {
  if (model.properties.nodeData) {
    const data = cloneDeep(model.properties.nodeData)
    Object.assign(formData, {
      poolId: data.poolId || '',
      location: data.location || 'customize',
      reference: data.reference || [],
      template: data.template || '',
      parameters: data.parameters || []
    })
  } else {
    model.properties.nodeData = {
      poolId: '',
      location: 'customize',
      reference: [],
      template: '',
      parameters: []
    }
  }
  setField()
})
</script>

<style lang="scss" scoped></style>
