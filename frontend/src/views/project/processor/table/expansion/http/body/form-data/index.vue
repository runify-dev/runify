<template>
  <div class="w-full">
    <Tabs value="value" class="w-full">
      <TabList class="border-b">
        <Tab value="value" class="px-4 py-2">Edit Value</Tab>
        <Tab value="schema" class="px-4 py-2">Schema</Tab>
      </TabList>
      <TabPanels class="p-4">
        <TabPanel value="schema">
          <TreeTable :value="treeData" class="w-full">
            <Column field="field" header="Field" expander>
              <template #body="slotProps">
                <div class="flex items-center gap-2">
                  <span class="font-medium">{{ slotProps.node.data.field }}</span>

                  <span v-if="slotProps.node.data.required" class="text-red-500">*</span>
                </div>
              </template>
            </Column>
            <Column field="type" header="Type">
              <template #body="slotProps">
                <Chip
                  :label="slotProps.node.data.type"
                  :class="getTypeClass(slotProps.node.data.type)"
                  size="small"
                />
              </template>
            </Column>
            <Column field="description" header="Description">
              <template #body="slotProps">
                <span class="text-gray-500">{{ slotProps.node.data.description }}</span>
              </template>
            </Column>
          </TreeTable>
        </TabPanel>
        <TabPanel value="value">
          <div class="space-y-4">
            <FormFieldRenderer
              v-for="field in rootFields"
              :key="field.field"
              :field="field"
              :form-values="formValues"
              :file-uploads="fileUploads"
              :boolean-options="booleanOptions"
              :path="[field.field]"
              @update:field-value="updateFieldValue"
              @file-select="onFileSelect"
              @file-remove="onFileRemove"
            />
          </div>
          <button @click="submit">ss</button>
        </TabPanel>
      </TabPanels>
    </Tabs>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, reactive } from 'vue'
import FormFieldRenderer from './FormFieldRenderer.vue'

interface FormDataField {
  field: string
  type: string
  description?: string
  required?: boolean
  multiple?: boolean
  fileTypes?: string[]
  maxFileSize?: number
  placeholder?: string
  min?: number
  max?: number
  children?: FormDataField[]
  defaultValue?: any
  errors?: string[]
}

const props = defineProps<{
  requestBody: FormDataField[]
}>()

const formValues = reactive<Record<string, any>>({})
const fileUploads = reactive<Record<string, File | File[]>>({})
const fieldDefinitions = ref<Map<string, FormDataField>>(new Map())

const booleanOptions = [
  { label: 'Yes', value: true },
  { label: 'No', value: false }
]

// 根字段
const rootFields = computed<FormDataField[]>(() => {
  return processFields(props.requestBody, [])
})

// 处理字段，建立路径映射
function processFields(fields: FormDataField[], parentPath: string[]): FormDataField[] {
  return fields.map((field) => {
    const currentPath = [...parentPath, field.field]
    const pathKey = currentPath.join('.')

    // 存储字段定义
    fieldDefinitions.value.set(pathKey, field)

    // 初始化值
    initializeFieldValue(field, currentPath)

    // 处理子字段
    let processedChildren: FormDataField[] | undefined
    if (field.children?.length) {
      processedChildren = processFields(field.children, currentPath)
    }

    return {
      ...field,
      children: processedChildren
    }
  })
}

// 初始化字段值
function initializeFieldValue(field: FormDataField, path: string[]): void {
  const value = getValueByPath(formValues, path)

  if (value !== undefined) return

  if (field.defaultValue !== undefined) {
    setValueByPath(formValues, path, field.defaultValue)
  } else if (field.type === 'object' && field.children) {
    const obj: Record<string, any> = {}
    field.children.forEach((child) => {
      const childPath = [...path, child.field]
      if (child.defaultValue !== undefined) {
        setValueByPath(formValues, childPath, child.defaultValue)
      } else {
        setValueByPath(formValues, childPath, getDefaultValueForType(child.type))
      }
    })
    setValueByPath(formValues, path, obj)
  } else if (field.type !== 'file') {
    setValueByPath(formValues, path, getDefaultValueForType(field.type))
  }
}

// 通过路径获取值
function getValueByPath(obj: any, path: string[]): any {
  let current = obj
  for (const key of path) {
    if (current === undefined || current === null) return undefined
    current = current[key]
  }
  return current
}

// 通过路径设置值
function setValueByPath(obj: any, path: string[], value: any): void {
  let current = obj
  for (let i = 0; i < path.length - 1; i++) {
    const key = path[i]
    if (!current[key] || typeof current[key] !== 'object') {
      current[key] = {}
    }
    current = current[key]
  }
  current[path[path.length - 1]] = value
}

function getDefaultValueForType(type: string): any {
  switch (type) {
    case 'boolean':
      return false
    case 'number':
    case 'integer':
      return 0
    case 'array':
      return []
    default:
      return ''
  }
}

// 更新字段值（支持路径）
function updateFieldValue(path: string[], value: any): void {
  setValueByPath(formValues, path, value)

  // 触发响应式更新
  if (path.length > 1) {
    const parentPath = path.slice(0, -1)
    const parent = getValueByPath(formValues, parentPath)
    if (parent) {
      setValueByPath(formValues, parentPath, { ...parent })
    }
  }

  const pathKey = path.join('.')
  validateField(pathKey)
}

// 文件选择
function onFileSelect(event: any, path: string[]): void {
  const files = event.files as File[]
  const pathKey = path.join('.')
  const field = fieldDefinitions.value.get(pathKey)

  if (field?.multiple) {
    const existing = (fileUploads[pathKey] as File[]) || []
    fileUploads[pathKey] = [...existing, ...files]
  } else {
    fileUploads[pathKey] = files[0]
  }

  validateField(pathKey)
}

// 文件移除
function onFileRemove(event: any, path: string[]): void {
  const filesToRemove = event.files as File[]
  const pathKey = path.join('.')
  const current = fileUploads[pathKey]

  if (Array.isArray(current)) {
    const remaining = current.filter((f) => !filesToRemove.includes(f))
    if (remaining.length) {
      fileUploads[pathKey] = remaining
    } else {
      delete fileUploads[pathKey]
    }
  } else {
    delete fileUploads[pathKey]
  }

  validateField(pathKey)
}

// 验证字段
function validateField(pathKey: string): void {
  const field = fieldDefinitions.value.get(pathKey)
  if (!field?.required) return

  const errors: string[] = []
  const path = pathKey.split('.')

  if (field.type === 'file') {
    if (!fileUploads[pathKey]) {
      errors.push(`${field.field} is required`)
    }
  } else {
    const value = getValueByPath(formValues, path)
    if (value === undefined || value === null || value === '') {
      errors.push(`${field.field} is required`)
    }
  }

  field.errors = errors
}

function getTypeClass(type: string): string {
  const classMap: Record<string, string> = {
    file: 'bg-blue-100 text-blue-800',
    text: 'bg-gray-100 text-gray-800',
    string: 'bg-gray-100 text-gray-800',
    integer: 'bg-green-100 text-green-800',
    number: 'bg-green-100 text-green-800',
    boolean: 'bg-purple-100 text-purple-800',
    object: 'bg-yellow-100 text-yellow-800',
    array: 'bg-pink-100 text-pink-800'
  }
  return classMap[type] || 'bg-gray-100 text-gray-800'
}

const treeData = computed(() => convertToTreeNodes(props.requestBody))

function convertToTreeNodes(fields: FormDataField[], parentKey: string = ''): any[] {
  return fields.map((field, index) => {
    const key = parentKey ? `${parentKey}_${field.field}_${index}` : `${field.field}_${index}`
    return {
      key,
      data: {
        field: field.field,
        type: field.type,
        description: field.description,
        required: field.required
      },
      children: field.children ? convertToTreeNodes(field.children, key) : null
    }
  })
}

// 提交表单
function submit(): FormData {
  // 验证所有字段
  validateAllFields()

  // 检查错误
  if (checkForErrors()) {
    throw new Error('Form validation failed')
  }

  const formData = new FormData()
  const result = flattenValues(formValues)

  // 构建 FormData
  Object.entries(result).forEach(([key, value]) => {
    if (value !== undefined && value !== null) {
      formData.append(key, String(value))
    }
  })

  // 添加文件
  Object.entries(fileUploads).forEach(([key, value]) => {
    const fileName = key.split('.').pop() || key
    if (Array.isArray(value)) {
      value.forEach((file, index) => {
        formData.append(`${fileName}[${index}]`, file, file.name)
      })
    } else if (value instanceof File) {
      formData.append(fileName, value, value.name)
    }
  })

  return formData
}

const getBody = () => {
  validateAllFields()
  if (checkForErrors()) {
    return Promise.reject({ errors: getErrors() })
  }
  const formData = new FormData()
  const result = flattenValues(formValues)

  // 构建 FormData
  Object.entries(result).forEach(([key, value]) => {
    if (value !== undefined && value !== null) {
      formData.append(key, String(value))
    }
  })

  // 添加文件
  Object.entries(fileUploads).forEach(([key, value]) => {
    const fileName = key.split('.').pop() || key
    if (Array.isArray(value)) {
      value.forEach((file, index) => {
        formData.append(`${fileName}[${index}]`, file, file.name)
      })
    } else if (value instanceof File) {
      formData.append(fileName, value, value.name)
    }
  })

  return Promise.resolve({ errors: getErrors(), values: formData })
}

// 扁平化嵌套对象
function flattenValues(obj: any, prefix: string = ''): Record<string, any> {
  const result: Record<string, any> = {}

  Object.entries(obj).forEach(([key, value]) => {
    const newKey = prefix ? `${prefix}.${key}` : key

    if (value && typeof value === 'object' && !Array.isArray(value) && !(value instanceof File)) {
      Object.assign(result, flattenValues(value, newKey))
    } else if (Array.isArray(value)) {
      value.forEach((item, index) => {
        result[`${newKey}[${index}]`] = item
      })
    } else {
      result[newKey] = value
    }
  })

  return result
}

// 获取当前表单值（树形结构）
function getFormValues(): Record<string, any> {
  return JSON.parse(JSON.stringify(formValues))
}

function validateAllFields(): void {
  fieldDefinitions.value.forEach((_, key) => {
    validateField(key)
  })
}

function checkForErrors(): boolean {
  let hasError = false
  fieldDefinitions.value.forEach((field) => {
    if (field.errors?.length) hasError = true
  })
  return hasError
}
function getErrors(): Record<string, string[]> {
  const errors: Record<string, string[]> = {}
  fieldDefinitions.value.forEach((field, key) => {
    if (field.errors?.length) {
      errors[key] = [...field.errors]
    }
  })
  return errors
}
defineExpose({ submit, getFormValues, getBody })
onMounted(() => {
  processFields(props.requestBody, [])
})
</script>
