<template>
  <div class="flex flex-col gap-4">
    <Fieldset legend="文件配置">
      <div class="flex flex-col gap-3">
        <div>
          <label class="text-sm font-medium">文件路径</label>
          <SelectButton
            class="mt-1"
            v-model="formData.pathLocation"
            :options="locationOptions"
            option-label="label"
            option-value="value"
            fluid
          />
          <Cascader
            v-if="formData.pathLocation === 'reference'"
            class="mt-1 w-full"
            :config="{ labelKey: 'label', valueKey: 'value' }"
            :options="fieldOptions"
            v-model="formData.pathReference"
            placeholder="请选择引用变量"
            fluid
          />
          <InputText
            v-else
            class="mt-1"
            v-model="formData.path"
            placeholder="请输入服务器文件路径"
            fluid
          />
          <Message v-if="errors.pathLocation" severity="error" size="small" variant="simple">
            {{ errors.pathLocation }}
          </Message>
        </div>

        <div>
          <label class="text-sm font-medium">文件名 <span class="text-muted-color font-normal">（可选，留空则自动取原文件名）</span></label>
          <InputText
            class="mt-1"
            v-model="formData.fileName"
            placeholder="自定义文件名"
            fluid
          />
        </div>
      </div>
    </Fieldset>
  </div>
</template>
<script setup lang="ts">
import { reactive, inject, onMounted } from 'vue'
import Cascader from '@/components/cascader/index.vue'
import type { BaseNodeModel } from '@logicflow/core'
import { validate as validateNodeData } from './validator'

const getModel = inject('getModel') as () => BaseNodeModel
const getNodeFieldOptions = inject('getNodeFieldOptions') as () => any[]
const model = getModel()
const fieldOptions = getNodeFieldOptions()

const locationOptions = [
  { label: '引用', value: 'reference' },
  { label: '自定义', value: 'customize' }
]

const formData = reactive({
  pathLocation: 'customize' as 'reference' | 'customize',
  pathReference: [] as string[],
  path: '',
  fileName: ''
})

const errors = reactive<Record<string, string>>({})

const validate = () => {
  const result = validateNodeData(formData)
  Object.keys(errors).forEach((k) => delete errors[k])
  if (!result.valid && result.errors) {
    Object.assign(errors, result.errors)
  }
  if (result.valid) {
    return Promise.resolve({ values: { ...formData }, errors: {} })
  }
  return Promise.resolve({ values: {}, errors: result.errors ?? {} })
}

const submit = () => {
  return validate().then(({ values, errors: errs }) => {
    if (Object.keys(errs).length === 0) {
      model.properties.nodeData = { ...formData }
      return Promise.resolve(values)
    }
    return Promise.resolve(errs)
  })
}

const setField = () => {
  model.properties.field_list = [
    { label: '文件ID', value: 'fileId' },
    { label: '文件名', value: 'fileName' },
    { label: '文件大小', value: 'fileSize' }
  ]
}

defineExpose({ validate, submit, setField })

onMounted(() => {
  if (model.properties.nodeData) {
    const data = model.properties.nodeData
    formData.pathLocation = data.pathLocation || 'customize'
    formData.pathReference = data.pathReference ? JSON.parse(JSON.stringify(data.pathReference)) : []
    formData.path = data.path || ''
    formData.fileName = data.fileName || ''
  } else {
    model.properties.nodeData = {
      pathLocation: 'customize',
      pathReference: [],
      path: '',
      fileName: ''
    }
  }
  setField()
})
</script>
<style lang="scss" scoped></style>
