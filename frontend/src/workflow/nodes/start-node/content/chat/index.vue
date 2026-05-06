<template>
  <div>
    <Fieldset legend="全局变量设置">
      <div class="flex justify-end mb-2">
        <Button
          label="添加变量"
          icon="pi pi-plus"
          size="small"
          severity="secondary"
          @click="openDialog()"
        />
      </div>

      <DataTable v-if="globalVariables.length" :value="globalVariables" size="small">
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
        暂无全局变量，点击上方按钮添加
      </div>
    </Fieldset>
    <Fieldset legend="全局变量" v-if="globalFieldList.length>0">
      <TreeNode
        v-for="field in globalFieldList"
        :key="field.value"
        :node="field"
        name="global"
        @copy="copy"
      />
    </Fieldset>
    <GlobalVariableDialog
      ref="dialogRef"
      :existing-names="existingNames"
      @submit="onDialogSubmit"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, inject, onMounted, computed } from 'vue'
import type { BaseNodeModel } from '@logicflow/core'
import GlobalVariableDialog from './GlobalVariableDialog.vue'
import { dataTypeOptions } from './type'
import TreeNode from "@/workflow/common/TreeNode.vue";
import Clipboard from "vue-clipboard3";
import bus from "@/bus";

interface GlobalVariable {
  name: string
  label: string
  dataType: 'string' | 'number' | 'boolean' | 'array' | 'dict'
  defaultValue?: any
}
const copy = (text: string) => {
  const { toClipboard } = Clipboard()
  toClipboard(text)
    .then(() => {
      bus.emit('message:success', '复制成功')
    })
    .catch(() => {
      bus.emit('message:error', '复制失败')
    })
}
const getModel = inject('getModel') as () => BaseNodeModel
const model = getModel()

const globalVariables = ref<GlobalVariable[]>([])
const dialogRef = ref()
const editingIndex = ref(-1)

const existingNames = computed(() => {
  return globalVariables.value.map(v => v.name).filter(Boolean)
})

function getDataTypeLabel(dataType?: string) {
  const option = dataTypeOptions.find(opt => opt.value === dataType)
  return option?.label || '字符串'
}

function formatDefaultValue(data: GlobalVariable) {
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

function openDialog(variable?: GlobalVariable, index?: number) {
  editingIndex.value = index ?? -1
  dialogRef.value?.open(variable)
}

function onDialogSubmit(variable: GlobalVariable) {
  const currentVariables = [...globalVariables.value]

  if (editingIndex.value >= 0) {
    currentVariables[editingIndex.value] = variable
  } else {
    currentVariables.push(variable)
  }

  globalVariables.value = currentVariables
  editingIndex.value = -1
  dialogRef.value?.close()
  updateFieldList()
}

function removeVariable(index: number) {
  globalVariables.value.splice(index, 1)
  updateFieldList()
}

const globalFieldList=computed(()=>{
  return model.properties.globalFieldList
})
function updateFieldList() {
  // 全局变量单独存储
  model.properties.globalFieldList = globalVariables.value
    .filter(v => v.name?.trim())
    .map(v => ({
      label: v.label || v.name,
      value: v.name
    }))
}

const validate = () => {
  return Promise.resolve({ values: { globalVariables: globalVariables.value }, errors: {} })
}

const submit = () => {
  return validate().then(({ values, errors }) => {
    if (Object.keys(errors).length === 0) {
      model.properties.nodeData = {
        ...model.properties.nodeData,
        globalVariables: globalVariables.value
      }
      updateFieldList()
      return Promise.resolve(values)
    }
    return Promise.resolve(errors)
  })
}

defineExpose({ validate, submit })

onMounted(() => {
  if (model.properties.nodeData?.globalVariables) {
    globalVariables.value = model.properties.nodeData.globalVariables
  }
  updateFieldList()
})
</script>

<style lang="scss" scoped></style>
