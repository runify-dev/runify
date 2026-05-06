<template>
  <div>
    <Fieldset legend="变量列表">
      <div class="flex justify-end mb-2">
        <Button
          label="添加变量"
          icon="pi pi-plus"
          size="small"
          severity="secondary"
          @click="openDialog()"
        />
      </div>

      <DataTable v-if="variables.length" :value="variables" size="small">
        <Column header="变量">
          <template #body="{ data }">
            <span class="text-sm">
              {{ formatVariable(data.variable) }}
            </span>
          </template>
        </Column>
        <Column header="值类型">
          <template #body="{ data }">
            {{ data.type === 'reference' ? '引用' : '常量' }}
          </template>
        </Column>
        <Column header="数据类型">
          <template #body="{ data }">
            <span v-if="data.type === 'constant'" class="text-sm">
              {{ getDataTypeLabel(data.dataType) }}
            </span>
            <span v-else class="text-sm text-slate-400">-</span>
          </template>
        </Column>
        <Column header="值">
          <template #body="{ data }">
            <span v-if="data.type === 'reference'" class="text-sm text-slate-500">
              {{ formatVariable(data.reference) }}
            </span>
            <span v-else class="text-sm">
              {{ formatConstantValue(data) }}
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
        暂无变量，点击上方按钮添加
      </div>
    </Fieldset>

    <VariableDialog
      ref="variableDialogRef"
      :existing-names="existingNames"
      @submit="onDialogSubmit"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, inject, onMounted, watch, computed } from 'vue'
import type { BaseNodeModel } from '@logicflow/core'
import VariableDialog from '../components/VariableDialog.vue'
import type { VariableItem } from './type'
import { dataTypeOptions } from './type'
import { validate as validateNodeData } from './validator'

const getModel = inject('getModel') as () => BaseNodeModel
const model = getModel()
const getGlobalFieldOptions = inject('getGlobalFieldOptions', () => []) as any
const globalFieldOptions = getGlobalFieldOptions()

// 只显示开始节点的全局变量
const globalVariableOptions = computed(() => {
  if (!globalFieldOptions || globalFieldOptions.length === 0) return []
  return globalFieldOptions
})

const variables = ref<VariableItem[]>([])
const variableDialogRef = ref()
const editingIndex = ref(-1)

const existingNames = computed(() => {
  return variables.value.map(v => v.variable?.join('.')).filter(Boolean)
})

function formatVariable(variable?: string[]) {
  if (!variable || variable.length === 0) return '未选择'

  // 从全局变量选项中查找标签
  const labels: string[] = []
  let currentOptions = globalVariableOptions.value

  for (const value of variable) {
    const option = currentOptions?.find((opt: any) => opt.value === value)
    if (option) {
      labels.push(option.label)
      currentOptions = option.children
    } else {
      labels.push(value)
    }
  }

  return labels.join(' / ')
}

function getDataTypeLabel(dataType?: string) {
  const option = dataTypeOptions.find(opt => opt.value === dataType)
  return option?.label || '字符串'
}

function formatConstantValue(data: VariableItem) {
  if (data.dataType === 'boolean') {
    return data.value ? 'true' : 'false'
  }
  if (data.dataType === 'number') {
    return String(data.value ?? '')
  }
  if (data.dataType === 'array' || data.dataType === 'dict') {
    try {
      const parsed = JSON.parse(data.value)
      if (Array.isArray(parsed)) {
        return `数组(${parsed.length}项)`
      }
      return `对象(${Object.keys(parsed).length}个属性)`
    } catch {
      return data.value || ''
    }
  }
  return data.value || ''
}

function openDialog(variable?: VariableItem, index?: number) {
  editingIndex.value = index ?? -1
  variableDialogRef.value?.open(variable)
}

function onDialogSubmit(variable: VariableItem) {
  const currentVariables = [...variables.value]

  if (editingIndex.value >= 0) {
    currentVariables[editingIndex.value] = variable
  } else {
    currentVariables.push(variable)
  }

  variables.value = currentVariables
  editingIndex.value = -1
  variableDialogRef.value?.close()
}

function removeVariable(index: number) {
  variables.value.splice(index, 1)
}



const validate = () => {
  const data = { variables: variables.value }
  return Promise.resolve(validateNodeData(data))
}

const submit = () => {
  return validate().then(({ values, errors }) => {
    if (Object.keys(errors).length === 0) {
      model.properties.nodeData = { variables: variables.value }
      return Promise.resolve({ variables: variables.value })
    }
    return Promise.resolve(errors)
  })
}

defineExpose({ validate, submit })

onMounted(() => {
  if (model.properties.nodeData?.variables) {
    variables.value = model.properties.nodeData.variables
  }

})

watch(variables, () => {

}, { deep: true })
</script>

<style lang="scss" scoped></style>
