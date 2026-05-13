<template>
  <Dialog
    v-model:visible="visible"
    :header="isEditing ? '编辑变量' : '添加变量'"
    modal
    append-to-body
    style="width: 500px"
  >
    <div class="flex flex-col gap-3">
      <!-- 变量名 -->
      <div>
        <label class="mb-1 block text-sm font-medium">变量名</label>
        <InputText
          v-model="formData.name"
          class="w-full"
          :class="{ 'p-invalid': errors.name }"
          placeholder="例如: apiKey"
        />
        <small v-if="errors.name" class="text-red-500">{{ errors.name }}</small>
      </div>

      <!-- 显示名称 -->
      <div>
        <label class="mb-1 block text-sm font-medium">显示名称</label>
        <InputText
          v-model="formData.label"
          class="w-full"
          :class="{ 'p-invalid': errors.label }"
          placeholder="例如: API密钥"
        />
        <small v-if="errors.label" class="text-red-500">{{ errors.label }}</small>
      </div>

      <!-- 数据类型 -->
      <div>
        <label class="mb-1 block text-sm font-medium">数据类型</label>
        <Select
          v-model="formData.dataType"
          :options="dataTypeOptions"
          option-label="label"
          option-value="value"
          class="w-full"
        />
      </div>

      <!-- 默认值 -->
      <div v-if="formData.dataType === 'string'">
        <label class="mb-1 block text-sm font-medium">默认值</label>
        <InputText
          v-model="formData.defaultValue"
          class="w-full"
          placeholder="请输入默认值"
        />
      </div>

      <div v-if="formData.dataType === 'number'">
        <label class="mb-1 block text-sm font-medium">默认值</label>
        <InputNumber
          v-model="formData.defaultValue"
          class="w-full"
        />
      </div>

      <div v-if="formData.dataType === 'boolean'">
        <label class="mb-1 block text-sm font-medium">默认值</label>
        <ToggleSwitch v-model="formData.defaultValue" />
      </div>

      <div v-if="formData.dataType === 'array' || formData.dataType === 'dict'">
        <label class="mb-1 block text-sm font-medium">默认值 (JSON)</label>
        <CodeEditor
          v-model="formData.defaultValue"
          :title="formData.dataType === 'array' ? '数组' : '字典'"
          lang="JSON"
          style="height: 150px"
        />
        <small v-if="errors.defaultValue" class="text-red-500">{{ errors.defaultValue }}</small>
      </div>
    </div>

    <div class="flex justify-end gap-2 mt-4">
      <Button label="取消" severity="secondary" @click="close" />
      <Button label="确定" @click="submit" />
    </div>
  </Dialog>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import CodeEditor from '@/components/code-editor/index.vue'
import type { GlobalVariable } from './type'
import { dataTypeOptions } from './type'
import bus from '@/bus'

const emit = defineEmits<{
  (e: 'submit', variable: GlobalVariable): void
}>()

defineProps<{
  existingNames?: string[]
}>()

const visible = ref(false)
const isEditing = ref(false)

const defaultFormData = {
  name: '',
  label: '',
  dataType: 'string' as string,
  defaultValue: '' as any
}

const defaultErrors = {
  name: '',
  label: '',
  defaultValue: ''
}

const formData = reactive({ ...defaultFormData })
const errors = reactive({ ...defaultErrors })

function clearErrors() {
  errors.name = ''
  errors.label = ''
  errors.defaultValue = ''
}

function open(variable?: GlobalVariable) {
  isEditing.value = !!variable
  clearErrors()

  if (variable) {
    formData.name = variable.name
    formData.label = variable.label
    formData.dataType = variable.dataType

    if (variable.dataType === 'number') {
      formData.defaultValue = variable.defaultValue ?? null
    } else if (variable.dataType === 'boolean') {
      formData.defaultValue = variable.defaultValue ?? false
    } else {
      formData.defaultValue = variable.defaultValue || ''
    }
  } else {
    Object.assign(formData, defaultFormData)
  }

  visible.value = true
}

function close() {
  visible.value = false
}

function submit() {
  clearErrors()
  let hasError = false

  if (!formData.name?.trim()) {
    errors.name = '请输入变量名'
    hasError = true
  }

  if (!formData.label?.trim()) {
    errors.label = '请输入显示名称'
    hasError = true
  }

  if ((formData.dataType === 'array' || formData.dataType === 'dict') && formData.defaultValue) {
    try {
      JSON.parse(formData.defaultValue)
    } catch {
      errors.defaultValue = 'JSON 格式不正确'
      hasError = true
    }
  }

  if (hasError) return

  const variable: GlobalVariable = {
    name: formData.name,
    label: formData.label,
    dataType: formData.dataType as any,
    defaultValue: formData.defaultValue
  }

  emit('submit', variable)
}

defineExpose({ open, close })
</script>
