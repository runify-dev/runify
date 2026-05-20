<template>
  <Dialog
    v-model:visible="visible"
    :header="isEditing ? '编辑上下文' : '添加上下文'"
    append-to-body
    :modal="false"
    :dismissable="false"
    style="width: 600px"
  >
    <div class="flex flex-col gap-3">
      <!-- 目标变量 -->
      <div>
        <label class="mb-1 block text-sm font-medium">目标变量</label>
        <Cascader
          placeholder="请选择变量"
          :config="{ labelKey: 'label', valueKey: 'value' }"
          :options="globalVariableOptions"
          v-model="formData.variable"
          optionLabel="label"
          optionGroupChildren="children"
          class="w-full"
          :class="{ 'p-invalid': errors.variable }"
        />
        <small v-if="errors.variable" class="text-red-500">{{ errors.variable }}</small>
      </div>

      <!-- 数据来源：引用 / 自定义 -->
      <div>
        <label class="mb-1 block text-sm font-medium">数据来源</label>
        <SelectButton
          v-model="dataSource"
          :options="dataSourceOptions"
          option-label="label"
          option-value="value"
          size="small"
        />
      </div>

      <!-- 引用模式：直接选变量 -->
      <div v-if="dataSource === 'reference'">
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

      <!-- 自定义模式 -->
      <template v-if="dataSource === 'custom'">
        <!-- 角色 + 来源合并选择 -->
        <div>
          <label class="mb-1 block text-sm font-medium">角色 & 来源</label>
          <SelectButton
            v-model="roleMode"
            :options="roleModeOptions"
            option-label="label"
            option-value="value"
            size="small"
          />
        </div>

        <!-- 来源是引用 -->
        <div v-if="currentMode === 'reference'">
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

        <!-- 来源是自定义 -->
        <div v-if="currentMode === 'custom'">
          <!-- Tool 角色：JSON 编辑器 -->
          <div v-if="currentRole === 'tool'">
            <label class="mb-1 block text-sm font-medium">内容 (JSON)</label>
            <CodeEditor
              v-model="formData.content"
              title="Tool Call"
              lang="JSON"
              style="height: 200px"
            />
            <small v-if="errors.content" class="text-red-500">{{ errors.content }}</small>
            <small class="text-gray-500">
              格式: {"toolName": "函数名", "functionArguments": "参数JSON", "content": "执行结果"}
            </small>
          </div>

          <!-- 其他角色：模板编辑器 -->
          <div v-else>
            <label class="mb-1 block text-sm font-medium">内容</label>
            <TemplateEditor
              v-model="formData.content"
              :variables="templateVariables"
              title="内容"
              style="height: 200px"
            />
            <small v-if="errors.content" class="text-red-500">{{ errors.content }}</small>
          </div>
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
import { ref, inject, reactive, computed, watch } from 'vue'
import Cascader from '@/components/cascader/index.vue'
import TemplateEditor from '@/components/template-editor/index.vue'
import CodeEditor from '@/components/code-editor/index.vue'
import type { ContextPushItem } from '../content/type'
import { toolCallTemplate } from '../content/type'
import bus from '@/bus'

const emit = defineEmits<{
  (e: 'submit', item: ContextPushItem): void
}>()

const getNodeFieldOptions = inject('getNodeFieldOptions') as any
const getGlobalFieldOptions = inject('getGlobalFieldOptions', () => []) as any
const getTemplateVariables = inject('getTemplateVariables') as any
const allFieldOptions = getNodeFieldOptions()
const globalFieldOptions = getGlobalFieldOptions()
const templateVariables = getTemplateVariables()

const globalVariableOptions = computed(() => {
  if (!globalFieldOptions || globalFieldOptions.length === 0) return []
  return globalFieldOptions
})

const visible = ref(false)
const isEditing = ref(false)

// 顶层数据来源
const dataSource = ref<'reference' | 'custom'>('custom')
const dataSourceOptions = [
  { label: '引用', value: 'reference' },
  { label: '自定义', value: 'custom' }
]

// 角色+来源合并选项
const roleMode = ref('system_custom')
const roleModeOptions = [
  { label: 'System 引用', value: 'system_reference' },
  { label: 'System 自定义', value: 'system_custom' },
  { label: 'User 引用', value: 'user_reference' },
  { label: 'User 自定义', value: 'user_custom' },
  { label: 'Assistant 引用', value: 'assistant_reference' },
  { label: 'Assistant 自定义', value: 'assistant_custom' },
  { label: 'Tool 引用', value: 'tool_reference' },
  { label: 'Tool 自定义', value: 'tool_custom' }
]

const currentRole = computed(() => roleMode.value.split('_')[0])
const currentMode = computed(() => roleMode.value.split('_')[1])

const defaultFormData = {
  variable: [] as string[],
  reference: [] as string[],
  content: ''
}

const defaultErrors = {
  variable: '',
  reference: '',
  content: ''
}

const formData = reactive({ ...defaultFormData })
const errors = reactive({ ...defaultErrors })

function clearErrors() {
  errors.variable = ''
  errors.reference = ''
  errors.content = ''
}

function open(item?: ContextPushItem) {
  isEditing.value = !!item
  clearErrors()

  if (item) {
    formData.variable = item.variable || []
    formData.reference = item.reference || []
    formData.content = item.content || ''

    if (item.mode === 'reference' && !item.role) {
      // 顶层引用模式（无 role）
      dataSource.value = 'reference'
      roleMode.value = 'system_custom'
    } else {
      dataSource.value = 'custom'
      const role = item.role || 'system'
      const mode = item.mode || 'custom'
      roleMode.value = `${role}_${mode}`
    }
  } else {
    Object.assign(formData, defaultFormData)
    dataSource.value = 'custom'
    roleMode.value = 'system_custom'
  }

  visible.value = true
}

function close() {
  visible.value = false
}

function submit() {
  clearErrors()
  let hasError = false

  // 校验目标变量
  if (!formData.variable || formData.variable.length === 0) {
    errors.variable = '请选择变量'
    hasError = true
  }

  if (dataSource.value === 'reference') {
    // 顶层引用模式
    if (!formData.reference || formData.reference.length === 0) {
      errors.reference = '请选择引用变量'
      hasError = true
    }
  } else {
    // 自定义模式
    if (currentMode.value === 'reference') {
      if (!formData.reference || formData.reference.length === 0) {
        errors.reference = '请选择引用变量'
        hasError = true
      }
    } else {
      if (!formData.content?.trim()) {
        errors.content = '请输入内容'
        hasError = true
      } else if (currentRole.value === 'tool') {
        try {
          const parsed = JSON.parse(formData.content)
          if (!parsed.toolName) {
            errors.content = '缺少 toolName 字段'
            hasError = true
          }
          if (!parsed.functionArguments) {
            errors.content = '缺少 functionArguments 字段'
            hasError = true
          }
        } catch {
          errors.content = 'JSON 格式不正确'
          hasError = true
        }
      }
    }
  }

  if (hasError) return

  let item: ContextPushItem

  if (dataSource.value === 'reference') {
    item = {
      variable: [...formData.variable],
      mode: 'reference',
      reference: [...formData.reference],
      role: 'user'
    }
  } else {
    item = {
      variable: [...formData.variable],
      mode: currentMode.value as 'reference' | 'custom',
      reference: currentMode.value === 'reference' ? [...formData.reference] : undefined,
      content: currentMode.value === 'custom' ? formData.content : undefined,
      role: currentRole.value as 'system' | 'user' | 'assistant' | 'tool'
    }
  }

  emit('submit', item)
}

defineExpose({ open, close })
</script>
