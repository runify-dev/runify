<template>
  <div
    class="border rounded-lg p-4 hover:bg-gray-50 transition-colors"
    :class="[depthClass, { 'border-l-4 border-l-blue-500': path.length > 1 }]"
  >
    <!-- 字段标签 -->
    <div class="flex items-center gap-2 mb-2 flex-wrap">
      <span class="font-medium text-gray-700">{{ field.field }}</span>
      <span v-if="field.required" class="text-red-500">*</span>
      <small class="text-gray-500 text-sm">{{ field.description }}</small>
      <span v-if="path.length > 1" class="text-xs bg-gray-100 px-2 py-1 rounded-full text-gray-600">
        {{ path.slice(0, -1).join(' > ') }}
      </span>
    </div>

    <!-- 字段输入 -->
    <div class="w-full">
      <!-- 文件类型 -->
      <div v-if="field.type === 'file'" class="space-y-2">
        <div v-if="fileValue" class="bg-gray-50 p-2 rounded space-y-1">
          <div v-if="Array.isArray(fileValue)" class="space-y-1">
            <div
              v-for="(file, index) in fileValue"
              :key="index"
              class="flex items-center gap-2 text-sm"
            >
              <i class="pi pi-file text-gray-500"></i>
              <span class="flex-1 truncate">{{ file.name }}</span>
              <Button
                icon="pi pi-times"
                class="p-button-rounded p-button-text p-button-sm"
                @click="removeFile(file)"
              />
            </div>
          </div>
          <div v-else class="flex items-center gap-2 text-sm">
            <i class="pi pi-file text-gray-500"></i>
            <span class="flex-1 truncate">{{ fileValue.name }}</span>
            <Button
              icon="pi pi-times"
              class="p-button-rounded p-button-text p-button-sm"
              @click="removeFile(fileValue)"
            />
          </div>
        </div>

        <FileUpload
          mode="basic"
          :name="field.field"
          :multiple="field.multiple || false"
          :accept="field.fileTypes?.join(',')"
          :maxFileSize="field.maxFileSize"
          @select="onFileSelectLocal"
          :fileLimit="field.multiple ? 10 : 1"
          :chooseLabel="field.multiple ? 'Choose Files' : 'Choose File'"
          :class="{ 'border-red-500': field.errors?.length }"
          :disabled="!field.multiple && !!fileValue"
          class="w-full"
        />
        <small v-if="field.errors?.length" class="text-red-500 text-sm block mt-1">{{
          field.errors[0]
        }}</small>
      </div>

      <!-- 字符串类型 -->
      <div v-else-if="field.type === 'string' || field.type === 'text'" class="w-full">
        <InputText
          :modelValue="stringValue"
          @update:modelValue="updateValue"
          :placeholder="field.placeholder || `Enter ${field.field}`"
          class="w-full"
          :class="{ 'border-red-500': field.errors?.length }"
        />
        <small v-if="field.errors?.length" class="text-red-500 text-sm block mt-1">{{
          field.errors[0]
        }}</small>
      </div>

      <!-- 数字类型 -->
      <div v-else-if="field.type === 'number' || field.type === 'integer'" class="w-full">
        <InputNumber
          :modelValue="numberValue"
          @update:modelValue="updateValue"
          :placeholder="field.placeholder || `Enter ${field.field}`"
          :min="field.min"
          :max="field.max"
          class="w-full"
          :class="{ 'border-red-500': field.errors?.length }"
        />
        <small v-if="field.errors?.length" class="text-red-500 text-sm block mt-1">{{
          field.errors[0]
        }}</small>
      </div>

      <!-- 布尔类型 -->
      <div v-else-if="field.type === 'boolean'" class="w-full">
        <SelectButton
          :modelValue="booleanValue"
          @update:modelValue="updateValue"
          :options="booleanOptions"
          optionLabel="label"
          optionValue="value"
          class="w-full"
          :class="{ 'border-red-500': field.errors?.length }"
        />
        <small v-if="field.errors?.length" class="text-red-500 text-sm block mt-1">{{
          field.errors[0]
        }}</small>
      </div>

      <!-- 数组类型 -->
      <div v-else-if="field.type === 'array'" class="space-y-2">
        <div v-for="(item, index) in arrayValue" :key="index" class="flex gap-2">
          <InputText
            :modelValue="item"
            @update:modelValue="(value) => updateArrayItem(index, value)"
            :placeholder="`${field.field}[${index}]`"
            class="flex-1"
          />
          <Button
            icon="pi pi-trash"
            class="p-button-rounded p-button-text p-button-sm"
            @click="removeArrayItem(index)"
          />
        </div>
        <Button
          :label="`Add ${field.field}`"
          icon="pi pi-plus"
          class="p-button-outlined p-button-sm"
          @click="addArrayItem"
        />
        <small v-if="field.errors?.length" class="text-red-500 text-sm block mt-1">{{
          field.errors[0]
        }}</small>
      </div>

      <!-- 对象类型 - 响应式网格布局 -->
      <div
        v-else-if="field.type === 'object' && field.children"
        class="grid gap-3"
        :class="gridLayoutClass"
      >
        <FormFieldRenderer
          v-for="child in field.children"
          :key="child.field"
          :field="child"
          :form-values="formValues"
          :file-uploads="fileUploads"
          :boolean-options="booleanOptions"
          :path="[...path, child.field]"
          @update:field-value="handleChildUpdate"
          @file-select="handleChildFileSelect"
          @file-remove="handleChildFileRemove"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

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
  field: FormDataField
  formValues: Record<string, any>
  fileUploads: Record<string, File | File[]>
  booleanOptions: Array<{ label: string; value: boolean }>
  path: string[]
}>()

const emit = defineEmits<{
  (e: 'update:fieldValue', path: string[], value: any): void
  (e: 'file-select', event: any, path: string[]): void
  (e: 'file-remove', event: any, path: string[]): void
}>()

// 根据子字段数量决定网格列数
const gridLayoutClass = computed(() => {
  const childCount = props.field.children?.length || 0

  // 手机端默认1列
  if (childCount === 1) return 'grid-cols-1'
  if (childCount === 2) return 'grid-cols-1 sm:grid-cols-2'
  if (childCount === 3) return 'grid-cols-1 sm:grid-cols-2 lg:grid-cols-3'
  if (childCount === 4) return 'grid-cols-1 sm:grid-cols-2 lg:grid-cols-4'

  // 5个以上字段，使用响应式多列
  return 'grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4'
})

// 根据嵌套深度添加不同的内边距
const depthClass = computed(() => {
  const depth = props.path.length - 1
  if (depth === 0) return ''
  if (depth === 1) return 'ml-2'
  if (depth === 2) return 'ml-4'
  if (depth === 3) return 'ml-6'
  return 'ml-8'
})

// 获取当前路径的值
const currentValue = computed(() => {
  let value = props.formValues
  for (const key of props.path) {
    if (value === undefined || value === null) return undefined
    value = value[key]
  }
  return value
})

// 字符串值
const stringValue = computed<string>({
  get: () => {
    const value = currentValue.value
    return value !== undefined && value !== null ? String(value) : ''
  },
  set: (value) => {
    emit('update:fieldValue', props.path, value)
  }
})

// 数字值
const numberValue = computed<number | null>({
  get: () => {
    const value = currentValue.value
    if (value === undefined || value === null) return null
    const num = Number(value)
    return isNaN(num) ? null : num
  },
  set: (value) => {
    emit('update:fieldValue', props.path, value)
  }
})

// 布尔值
const booleanValue = computed<boolean | null>({
  get: () => {
    const value = currentValue.value
    if (value === undefined || value === null) return null
    return Boolean(value)
  },
  set: (value) => {
    emit('update:fieldValue', props.path, value)
  }
})

// 数组值
const arrayValue = computed<any[]>({
  get: () => {
    const value = currentValue.value
    return Array.isArray(value) ? value : []
  },
  set: (value) => {
    emit('update:fieldValue', props.path, value)
  }
})

// 文件值
const fileValue = computed<File | File[] | null>(() => {
  const pathKey = props.path.join('.')
  return props.fileUploads[pathKey] || null
})

function updateValue(value: any): void {
  emit('update:fieldValue', props.path, value)
}

function onFileSelectLocal(event: any): void {
  emit('file-select', event, props.path)
}

function removeFile(file: File): void {
  const fakeEvent = { files: [file] }
  emit('file-remove', fakeEvent, props.path)
}

function addArrayItem(): void {
  arrayValue.value = [...arrayValue.value, '']
}

function updateArrayItem(index: number, value: any): void {
  const newArray = [...arrayValue.value]
  newArray[index] = value
  arrayValue.value = newArray
}

function removeArrayItem(index: number): void {
  arrayValue.value = arrayValue.value.filter((_, i) => i !== index)
}

function handleChildUpdate(path: string[], value: any): void {
  emit('update:fieldValue', path, value)
}

function handleChildFileSelect(event: any, path: string[]): void {
  emit('file-select', event, path)
}

function handleChildFileRemove(event: any, path: string[]): void {
  emit('file-remove', event, path)
}
</script>

<style scoped>
/* 确保网格布局在小屏幕上也能正常工作 */
@media (max-width: 640px) {
  .grid {
    gap: 0.75rem;
  }
}
</style>
