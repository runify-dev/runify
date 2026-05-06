<template>
  <Dialog
    v-model:visible="visible"
    :header="isEditing ? '编辑上下文' : '添加上下文'"
    modal
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

      <!-- 角色 -->
      <div>
        <label class="mb-1 block text-sm font-medium">角色</label>
        <Select
          v-model="formData.role"
          :options="roleOptions"
          option-label="label"
          option-value="value"
          class="w-full"
        />
      </div>

      <!-- 数据来源 -->
      <div>
        <label class="mb-1 block text-sm font-medium">数据来源</label>
        <SelectButton
          v-model="formData.mode"
          :options="modeOptions"
          option-label="label"
          option-value="value"
          size="small"
        />
      </div>

      <!-- 引用模式 -->
      <div v-if="formData.mode === 'reference'">
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
      <div v-if="formData.mode === 'custom'">
        <!-- Tool 角色使用 JSON 编辑器 -->
        <div v-if="formData.role === 'tool'">
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

        <!-- 其他角色使用 TemplateEditor -->
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
import type { ContextPushItem } from '../content/type'
import { modeOptions, roleOptions, toolCallTemplate } from '../content/type'
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

const defaultFormData = {
  variable: [] as string[],
  mode: 'custom' as 'reference' | 'custom',
  reference: [] as string[],
  content: '',
  role: 'system' as 'system' | 'user' | 'assistant' | 'tool'
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
    formData.mode = item.mode
    formData.reference = item.reference || []
    formData.content = item.content || ''
    formData.role = item.role
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
  if (formData.mode === 'reference') {
    if (!formData.reference || formData.reference.length === 0) {
      errors.reference = '请选择引用变量'
      hasError = true
    }
  }

  // 校验自定义内容
  if (formData.mode === 'custom') {
    if (!formData.content?.trim()) {
      errors.content = '请输入内容'
      hasError = true
    } else if (formData.role === 'tool') {
      // Tool 角色需要校验 JSON 格式
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

  if (hasError) return

  const item: ContextPushItem = {
    variable: [...formData.variable],
    mode: formData.mode,
    reference: formData.mode === 'reference' ? [...formData.reference] : undefined,
    content: formData.mode === 'custom' ? formData.content : undefined,
    role: formData.role
  }

  emit('submit', item)
}

defineExpose({ open, close })
</script>
