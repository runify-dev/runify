<template>
  <div>
    <Fieldset legend="上下文列表">
      <div class="flex justify-end mb-2">
        <Button
          label="添加上下文"
          icon="pi pi-plus"
          size="small"
          severity="secondary"
          @click="openDialog()"
        />
      </div>

      <DataTable v-if="items.length" :value="items" size="small">
        <Column header="变量">
          <template #body="{ data }">
            <span class="text-sm">
              {{ formatVariable(data.variable) }}
            </span>
          </template>
        </Column>
        <Column header="角色">
          <template #body="{ data }">
            <span class="text-sm">{{ data.role }}</span>
          </template>
        </Column>
        <Column header="数据来源">
          <template #body="{ data }">
            <span class="text-sm">
              {{ data.mode === 'reference' ? '引用' : '自定义' }}
            </span>
          </template>
        </Column>
        <Column header="内容">
          <template #body="{ data }">
            <span v-if="data.mode === 'reference'" class="text-sm text-slate-500">
              {{ formatVariable(data.reference) }}
            </span>
            <span v-else class="text-sm truncate max-w-[200px] inline-block">
              {{ data.content }}
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
              @click="removeItem(index)"
            />
          </template>
        </Column>
      </DataTable>

      <div v-else class="text-center text-slate-400 py-4">
        暂无上下文，点击上方按钮添加
      </div>
    </Fieldset>

    <ContextPushDialog
      ref="dialogRef"
      @submit="onDialogSubmit"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, inject, onMounted } from 'vue'
import type { BaseNodeModel } from '@logicflow/core'
import ContextPushDialog from '../components/ContextPushDialog.vue'
import type { ContextPushItem } from './type'
import { validate as validateNodeData } from './validator'

const getModel = inject('getModel') as () => BaseNodeModel
const model = getModel()
const getGlobalFieldOptions = inject('getGlobalFieldOptions', () => []) as any
const globalFieldOptions = getGlobalFieldOptions()
const items = ref<ContextPushItem[]>([])
const dialogRef = ref()
const editingIndex = ref(-1)

function formatVariable(variable?: string[]) {
  if (!variable || variable.length === 0) return '未选择'

  const labels: string[] = []
  let currentOptions = globalFieldOptions

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

function openDialog(item?: ContextPushItem, index?: number) {
  editingIndex.value = index ?? -1
  dialogRef.value?.open(item)
}

function onDialogSubmit(item: ContextPushItem) {
  const currentItems = [...items.value]

  if (editingIndex.value >= 0) {
    currentItems[editingIndex.value] = item
  } else {
    currentItems.push(item)
  }

  items.value = currentItems
  editingIndex.value = -1
  dialogRef.value?.close()
}

function removeItem(index: number) {
  items.value.splice(index, 1)
}

const validate = () => {
  const data = { items: items.value }
  return Promise.resolve(validateNodeData(data))
}

const submit = () => {
  return validate().then(({ errors }) => {
    if (Object.keys(errors).length === 0) {
      model.properties.nodeData = { items: items.value }
      return {} as Record<string, string>
    }
    return errors
  })
}

defineExpose({ validate, submit })

onMounted(() => {
  if (model.properties.nodeData?.items) {
    items.value = model.properties.nodeData.items
  }
})
</script>

<style lang="scss" scoped></style>
