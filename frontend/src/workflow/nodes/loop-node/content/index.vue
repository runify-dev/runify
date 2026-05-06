<template>
  <Form ref="formRef" :resolver="zodResolver(schema)">
    <Fieldset legend="循环配置">
      <FormField class="mt-2" v-slot="$field: any" name="loopType">
        <label>循环类型</label>
        <Select
          :modelValue="$field.value"
          :options="loopTypeOptions"
          optionLabel="label"
          optionValue="value"
          placeholder="请选择循环类型"
          class="w-full mt-2"
          @update:modelValue="(v: any) => { formRef?.setFieldValue('loopType', v); currentLoopType = v }"
        />
        <Message v-if="$field?.invalid" severity="error" size="small" variant="simple">
          {{ $field.error?.message }}
        </Message>
      </FormField>

      <FormField v-if="showLoopCount" class="mt-2" v-slot="$field: any" name="loopCount">
        <label>循环次数</label>
        <InputNumber
          :modelValue="$field.value"
          @update:modelValue="(v: any) => formRef?.setFieldValue('loopCount', v)"
          :min="1"
          :max="1000"
          placeholder="请输入循环次数"
          class="w-full mt-2"
        />
        <Message v-if="$field?.invalid" severity="error" size="small" variant="simple">
          {{ $field.error?.message }}
        </Message>
      </FormField>

      <FormField v-if="showLoopVariable" class="mt-2" v-slot="$field: any" name="loopVariable">
        <label>循环变量</label>
        <Cascader
          :modelValue="$field.value"
          :options="options"
          :config="cascaderConfig"
          placeholder="请选择要遍历的变量"
          class="w-full mt-2"
          @update:modelValue="(val: any) => formRef?.setFieldValue('loopVariable', val)"
        />
        <Message v-if="$field?.invalid" severity="error" size="small" variant="simple">
          {{ $field.error?.message }}
        </Message>
      </FormField>
    </Fieldset>

    <Fieldset legend="循环变量设置" class="mt-4">
      <div class="flex justify-end mb-2">
        <Button
          label="添加变量"
          icon="pi pi-plus"
          size="small"
          severity="secondary"
          @click="openDialog()"
        />
      </div>

      <DataTable v-if="loopVariables.length" :value="loopVariables" size="small">
        <Column header="变量名" field="name" />
        <Column header="显示名称" field="label" />
        <Column header="数据类型">
          <template #body="{ data }">
            {{ getDataTypeLabel(data.dataType) }}
          </template>
        </Column>
        <Column header="默认值">
          <template #body="{ data }">
            <span class="text-sm text-slate-500">
              {{ formatDefaultValue(data) }}
            </span>
          </template>
        </Column>
        <Column header="操作" style="width: 100px">
          <template #body="{ data, index }">
            <Button
              icon="pi pi-pencil"
              size="small"
              severity="secondary"
              text
              @click="openDialog(data, index)"
            />
            <Button
              icon="pi pi-trash"
              size="small"
              severity="danger"
              text
              @click="removeVariable(index)"
            />
          </template>
        </Column>
      </DataTable>

      <div v-else class="text-center text-slate-400 py-4">
        暂无循环变量，点击上方按钮添加
      </div>
    </Fieldset>

    <LoopVariableDialog
      ref="dialogRef"
      :existing-names="existingNames"
      @submit="onDialogSubmit"
    />
  </Form>
</template>
<script setup lang="ts">
import { ref, inject, onMounted, computed, watch, nextTick ,provide} from 'vue'
import type { BaseNodeModel } from '@logicflow/core'
import type { FormInstance } from '@primevue/forms'
import Cascader from '@/components/cascader/index.vue'
import InputNumber from 'primevue/inputnumber'
import type { CascaderOption } from '@/workflow/nodes/judge-node/type'
import { zodResolver } from '@primevue/forms/resolvers/zod'
import { schema, validate as validateNodeData } from './validator'
import LoopVariableDialog from '@/workflow/nodes/loop-start-node/content/LoopVariableDialog.vue'
import type { LoopVariable } from '@/workflow/nodes/loop-start-node/content/LoopVariableDialog.vue'

const getModel = inject('getModel') as () => BaseNodeModel
const model = getModel()
const formRef = ref<FormInstance>()

const getNodeFieldOptions = inject('getNodeFieldOptions') as (withSlef?: boolean) => CascaderOption[]
const options = computed<CascaderOption[]>(() => {
  const result = getNodeFieldOptions?.()
  return Array.isArray(result) ? result : []
})
const cascaderConfig = { labelKey: 'label', valueKey: 'value', childrenKey: 'children' }

const loopTypeOptions = [
  { label: '数组循环', value: 'foreach' },
  { label: '无限循环', value: 'infinite' },
  { label: '指定次数循环', value: 'count' }
]

const currentLoopType = ref('foreach')

const showLoopCount = computed(() => currentLoopType.value === 'count')
const showLoopVariable = computed(() => currentLoopType.value === 'foreach')

watch(currentLoopType, (newType, oldType) => {
  if (newType === oldType) return
  if (newType !== 'count') formRef.value?.setFieldValue('loopCount', undefined)
  if (newType !== 'foreach') formRef.value?.setFieldValue('loopVariable', undefined)
})

// ── 循环变量设置 ──
const loopVariables = ref<LoopVariable[]>([])
const dialogRef = ref()
const editingIndex = ref(-1)

const dataTypeOptions = [
  { label: '字符串', value: 'string' },
  { label: '数字', value: 'number' },
  { label: '布尔', value: 'boolean' },
  { label: '数组', value: 'array' },
  { label: '字典', value: 'dict' }
]

const existingNames = computed(() => {
  return loopVariables.value.map(v => v.name).filter(Boolean)
})

function getDataTypeLabel(dataType?: string) {
  const option = dataTypeOptions.find(opt => opt.value === dataType)
  return option?.label || '字符串'
}

function formatDefaultValue(data: LoopVariable) {
  if (data.defaultValue === undefined || data.defaultValue === null) return '未设置'
  if (data.dataType === 'boolean') return data.defaultValue ? 'true' : 'false'
  if (data.dataType === 'array' || data.dataType === 'dict') {
    try {
      const parsed = JSON.parse(data.defaultValue)
      if (Array.isArray(parsed)) return `数组(${parsed.length}项)`
      return `对象(${Object.keys(parsed).length}个属性)`
    } catch {
      return data.defaultValue
    }
  }
  return String(data.defaultValue)
}

function openDialog(variable?: LoopVariable, index?: number) {
  editingIndex.value = index ?? -1
  dialogRef.value?.open(variable)
}

function onDialogSubmit(variable: LoopVariable) {
  const currentVariables = [...loopVariables.value]
  if (editingIndex.value >= 0) {
    currentVariables[editingIndex.value] = variable
  } else {
    currentVariables.push(variable)
  }
  loopVariables.value = currentVariables
  editingIndex.value = -1
  dialogRef.value?.close()
  updateFieldList()
}

function removeVariable(index: number) {
  loopVariables.value.splice(index, 1)
  updateFieldList()
}

function updateFieldList() {
  // 默认变量 + 用户自定义变量
  const defaultFields = [
    { label: '当前项', value: 'item' },
    { label: '当前索引', value: 'index' }
  ]
  const customFields = loopVariables.value
    .filter(v => v.name?.trim())
    .map(v => ({ label: v.label || v.name, value: v.name }))
  model.properties.field_list = [...defaultFields, ...customFields]
  model.properties.loopFieldList = customFields
}

// ── 校验 & 提交 ──
const validate = () => {
  if (formRef.value) {
    return formRef.value.validate()
  }
  const result = validateNodeData(model.properties.nodeData)
  if (result.valid) {
    return Promise.resolve({ values: model.properties.nodeData, errors: {} })
  }
  return Promise.resolve({ values: {}, errors: result.errors })
}

const submit = () => {
  return validate().then((result: any) => {
    if (result === true) return Promise.resolve(true)
    const { values, errors } = result
    if (Object.keys(errors).length === 0) {
      const formValues = { ...values }
      formValues.loopVariables = loopVariables.value
      model.properties.nodeData = formValues
      updateFieldList()
      return Promise.resolve(formValues)
    }
    return Promise.resolve(errors)
  })
}

onMounted(async () => {
  if (model.properties.nodeData) {
    const data = JSON.parse(JSON.stringify(model.properties.nodeData))
    currentLoopType.value = data.loopType || 'foreach'
    if (data.loopType) formRef.value?.setFieldValue('loopType', data.loopType)
    await nextTick()
    if (data.loopCount != null) formRef.value?.setFieldValue('loopCount', data.loopCount)
    if (data.loopVariable) formRef.value?.setFieldValue('loopVariable', data.loopVariable)
    if (data.loopVariables) loopVariables.value = data.loopVariables
  }
  updateFieldList()
})

defineExpose({ validate, submit })
</script>
<style lang="scss" scoped></style>
