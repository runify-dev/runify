<template>
  <div>
    <SelectButton
      v-model="data.location"
      :options="locationOptions"
      option-label="label"
      option-value="value"
      class="mb-2"
    />

    <!-- 引用模式 -->
    <Cascader
      v-if="data.location === 'reference'"
      :config="{ labelKey: 'label', valueKey: 'value' }"
      :options="options"
      v-model="data.reference"
      optionLabel="label"
      :optionGroupChildren="['children']"
      class="w-full"
      placeholder="请选择工具变量"
    />

    <!-- 自定义模式 -->
    <div v-else>
      <div class="flex gap-2">
        <Button
          label="添加函数"
          icon="pi pi-plus"
          size="small"
          severity="secondary"
          @click="openDialog()"
        />
        <Button
          label="批量导入"
          icon="pi pi-upload"
          size="small"
          severity="secondary"
          @click="showImportDialog = true"
        />
        <Button
          label="导出"
          icon="pi pi-download"
          size="small"
          severity="secondary"
          @click="exportTools"
        />
      </div>

      <DataTable v-if="data?.tools?.length" :value="data.tools" class="mt-2" size="small">
        <Column field="function.name" header="名称" />
        <Column field="function.description" header="描述" />
        <Column header="操作" style="width: 100px">
          <template #body="{ data: row, index }">
            <Button
              icon="pi pi-pencil"
              size="small"
              severity="secondary"
              text
              @click="openDialog(row, index)"
            />
            <Button
              icon="pi pi-trash"
              size="small"
              severity="danger"
              text
              @click="removeTool(index)"
            />
          </template>
        </Column>
      </DataTable>
    </div>

    <!-- 函数配置弹窗 -->
    <ToolDialog
      ref="toolDialogRef"
      :existing-names="existingToolNames"
      @submit="onDialogSubmit"
    />

    <!-- 批量导入弹窗 -->
    <Dialog
      v-model:visible="showImportDialog"
      header="批量导入工具"
      :modal="false"
      :dismissable="false"
      style="width: 600px"
    >
      <div>
        <label class="mb-1 block text-sm font-medium">粘贴工具 JSON</label>
        <Textarea
          v-model="importJson"
          rows="10"
          class="w-full"
          placeholder='粘贴 OpenAI tools 格式 JSON，例如:
[
  {
    "type": "function",
    "function": {
      "name": "get_weather",
      "description": "获取天气",
      "parameters": {
        "type": "object",
        "properties": {
          "location": { "type": "string", "description": "城市" }
        },
        "required": ["location"]
      }
    }
  }
]'
        />
        <small v-if="importError" class="text-red-500">{{ importError }}</small>
      </div>

      <template #footer>
        <Button label="取消" severity="secondary" @click="showImportDialog = false" />
        <Button label="导入" @click="parseAndImport" />
      </template>
    </Dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, inject, computed } from 'vue'
import Cascader from '@/components/cascader/index.vue'
import ToolDialog from './ToolDialog.vue'
import type { Tool, ToolsConfig } from './type'

const props = defineProps<{
  modelValue: ToolsConfig
}>()

const emit = defineEmits(['update:modelValue'])

const locationOptions = [
  { label: '引用', value: 'reference' },
  { label: '自定义', value: 'customize' }
]

const getNodeFieldOptions = inject('getNodeFieldOptions') as any
const options = getNodeFieldOptions()

const existingToolNames = computed(() => {
  return props.modelValue?.tools?.map((t: Tool) => t.function.name) || []
})

const data = computed({
  get: () => {
    return props.modelValue
  },
  set: (value) => {
    emit('update:modelValue', value)
  }
})

// 函数配置弹窗
const toolDialogRef = ref()
const editingIndex = ref(-1)

function openDialog(tool?: Tool, index?: number) {
  editingIndex.value = index ?? -1
  toolDialogRef.value?.open(tool)
}

function onDialogSubmit(tool: Tool) {
  const currentData = data.value || { location: 'customize', reference: [], tools: [] }
  const currentTools = [...(currentData.tools || [])]

  if (editingIndex.value >= 0) {
    currentTools[editingIndex.value] = tool
  } else {
    currentTools.push(tool)
  }

  data.value = { ...currentData, tools: currentTools }
  editingIndex.value = -1
  toolDialogRef.value?.close()
}

function removeTool(index: number) {
  const currentData = data.value || { location: 'customize', reference: [], tools: [] }
  const currentTools = [...(currentData.tools || [])]
  currentTools.splice(index, 1)
  data.value = { ...currentData, tools: currentTools }
}

// 批量导入
const showImportDialog = ref(false)
const importJson = ref('')
const importError = ref('')

function parseAndImport() {
  importError.value = ''

  try {
    const parsed = JSON.parse(importJson.value)
    const tools = Array.isArray(parsed) ? parsed : [parsed]

    const validTools: Tool[] = []

    for (const item of tools) {
      if (item.type === 'function' && item.function?.name) {
        validTools.push({
          type: 'function',
          function: {
            name: item.function.name,
            description: item.function.description || '',
            parameters: item.function.parameters || { type: 'object', properties: {} }
          }
        })
      }
    }

    if (validTools.length === 0) {
      importError.value = '未找到有效的工具定义'
      return
    }

    const currentData = data.value || { location: 'customize', reference: [], tools: [] }
    const currentTools = [...(currentData.tools || []), ...validTools]
    data.value = { ...currentData, tools: currentTools }
    showImportDialog.value = false
    importJson.value = ''
  } catch (e) {
    importError.value = 'JSON 解析失败，请检查格式'
  }
}

function exportTools() {
  const tools = data.value?.tools || []
  if (tools.length === 0) return
  const json = JSON.stringify(tools, null, 2)
  const blob = new Blob([json], { type: 'application/json' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = 'tools.json'
  a.click()
  URL.revokeObjectURL(url)
}
</script>
