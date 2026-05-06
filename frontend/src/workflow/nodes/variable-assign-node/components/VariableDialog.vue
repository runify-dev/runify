<template>
  <Dialog
    v-model:visible="visible"
    :header="isEditing ? '编辑变量' : '添加变量'"
    modal
    style="width: 600px"
  >
    <div class="flex flex-col gap-3">
      <!-- 变量（只能选择全局变量） -->
      <div>
        <label class="mb-1 block text-sm font-medium">变量</label>
        <Cascader
          placeholder="请选择全局变量"
          :config="{ labelKey: 'label', valueKey: 'value' }"
          :options="globalVariableOptions"
          v-model="formData.variable"
          optionLabel="label"
          optionGroupChildren="children"
          class="w-full"
          :class="{ 'p-invalid': errors.variable }"
        />
        <small v-if="errors.variable" class="text-red-500">{{ errors.variable }}</small>
        <small v-if="!globalVariableOptions.length" class="text-gray-400">请先在开始节点定义全局变量</small>
      </div>

      <!-- 值类型 -->
      <div>
        <label class="mb-1 block text-sm font-medium">值类型</label>
        <SelectButton
          v-model="formData.type"
          :options="valueTypeOptions"
          option-label="label"
          option-value="value"
          size="small"
        />
      </div>

      <!-- 引用变量（可以选择任意上游节点的变量） -->
      <div v-if="formData.type === 'reference'">
        <label class="mb-1 block text-sm font-medium">引用变量</label>
        <Cascader
          placeholder="请选择引用变量"
          :config="{ labelKey: 'label', valueKey: 'value' }"
          :options="allFieldOptions"
          v-model="formData.reference"
          optionLabel="label"
          optionGroupChildren="children"
          class="w-full"
          :class="{ 'p-invalid': errors.reference }"
        />
        <small v-if="errors.reference" class="text-red-500">{{ errors.reference }}</small>
      </div>

      <!-- 常量 -->
      <template v-if="formData.type === 'constant'">
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

        <!-- 字符串 -->
        <div v-if="formData.dataType === 'string'">
          <label class="mb-1 block text-sm font-medium">字符串值</label>
          <TemplateEditor
            v-model="formData.value"
            :variables="variables"
            title="字符串值"
            style="height: 150px"
          />
          <small v-if="errors.value" class="text-red-500">{{ errors.value }}</small>
        </div>

        <!-- 数组 / 字典 -->
        <div v-if="formData.dataType === 'array' || formData.dataType === 'dict'">
          <label class="mb-1 block text-sm font-medium">
            {{ formData.dataType === 'array' ? '数组值 (JSON)' : '字典值 (JSON)' }}
          </label>
          <CodeEditor
            v-model="formData.value"
            :title="formData.dataType === 'array' ? '数组' : '字典'"
            lang="JSON"
            style="height: 150px"
          />
          <small v-if="errors.value" class="text-red-500">{{ errors.value }}</small>
          <small v-if="errors.json" class="text-red-500">{{ errors.json }}</small>
        </div>

        <!-- 数字 -->
        <div v-if="formData.dataType === 'number'">
          <label class="mb-1 block text-sm font-medium">数字值</label>
          <InputNumber
            v-model="formData.value"
            class="w-full"
            :class="{ 'p-invalid': errors.value }"
          />
          <small v-if="errors.value" class="text-red-500">{{ errors.value }}</small>
        </div>

        <!-- 布尔 -->
        <div v-if="formData.dataType === 'boolean'">
          <label class="mb-1 block text-sm font-medium">布尔值</label>
          <ToggleSwitch v-model="formData.value" />
        </div>
      </template>
    </div>

    <div class="flex justify-end gap-2 mt-4">
      <Button label="取消" severity="secondary" @click="close" />
      <Button label="确定" @click="submit" />
    </div>
  </Dialog>
</template>

<script setup lang="ts">
import { ref, inject, reactive, computed } from 'vue'
import Cascader from '@/components/cascader/index.vue'
import TemplateEditor from '@/components/template-editor/index.vue'
import CodeEditor from '@/components/code-editor/index.vue'
import type { VariableItem } from '../content/type'
import { valueTypeOptions, dataTypeOptions } from '../content/type'
import bus from '@/bus'

const emit = defineEmits<{
  (e: 'submit', variable: VariableItem): void
}>()

const getNodeFieldOptions = inject('getNodeFieldOptions') as any
const getGlobalFieldOptions = inject('getGlobalFieldOptions', () => []) as any
const getTemplateVariables = inject('getTemplateVariables') as any
const allFieldOptions = getNodeFieldOptions()
const globalFieldOptions = getGlobalFieldOptions()
const variables = getTemplateVariables()

// 只显示开始节点的全局变量
const globalVariableOptions = computed(() => {
  if (!globalFieldOptions || globalFieldOptions.length === 0) return []
  return globalFieldOptions
})

const visible = ref(false)
const isEditing = ref(false)

const defaultFormData = {
  variable: [] as string[],
  type: 'reference' as 'reference' | 'constant',
  reference: [] as string[],
  dataType: 'string' as string,
  value: '' as any
}

const defaultErrors = {
  variable: '',
  reference: '',
  value: '',
  json: ''
}

const formData = reactive({ ...defaultFormData })
const errors = reactive({ ...defaultErrors })

function clearErrors() {
  errors.variable = ''
  errors.reference = ''
  errors.value = ''
  errors.json = ''
}

function open(variable?: VariableItem) {
  isEditing.value = !!variable
  clearErrors()

  if (variable) {
    formData.variable = variable.variable || []
    formData.type = variable.type
    formData.reference = variable.reference || []
    formData.dataType = variable.dataType || 'string'

    // 根据数据类型设置值
    if (variable.type === 'constant') {
      const dataType = variable.dataType || 'string'
      if (dataType === 'string') {
        formData.value = variable.value || ''
      } else if (dataType === 'array' || dataType === 'dict') {
        formData.value = variable.value || ''
      } else if (dataType === 'number') {
        formData.value = variable.value ?? null
      } else if (dataType === 'boolean') {
        formData.value = variable.value ?? false
      }
    } else {
      formData.value = ''
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

  // 校验变量
  if (!formData.variable || formData.variable.length === 0) {
    errors.variable = '请选择变量'
    hasError = true
  }

  // 校验引用
  if (formData.type === 'reference') {
    if (!formData.reference || formData.reference.length === 0) {
      errors.reference = '请选择引用变量'
      hasError = true
    }
  }

  // 校验常量值
  if (formData.type === 'constant') {
    if (formData.dataType === 'string') {
      if (!formData.value?.trim()) {
        errors.value = '请输入字符串值'
        hasError = true
      }
    } else if (formData.dataType === 'array' || formData.dataType === 'dict') {
      if (!formData.value?.trim()) {
        errors.value = '请输入JSON值'
        hasError = true
      } else {
        try {
          JSON.parse(formData.value)
        } catch {
          errors.json = 'JSON 格式不正确'
          hasError = true
        }
      }
    } else if (formData.dataType === 'number') {
      if (formData.value === null || formData.value === undefined) {
        errors.value = '请输入数字值'
        hasError = true
      }
    }
    // boolean 不需要校验
  }

  if (hasError) {
    return
  }

  const variable: VariableItem = {
    variable: [...formData.variable],
    type: formData.type,
    reference: formData.type === 'reference' ? [...formData.reference] : undefined,
    dataType: formData.type === 'constant' ? formData.dataType : undefined,
    value: formData.type === 'constant' ? formData.value : undefined
  }

  emit('submit', variable)
}

defineExpose({ open, close })
</script>
