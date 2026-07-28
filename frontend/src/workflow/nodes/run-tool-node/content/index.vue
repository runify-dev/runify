<template>
  <div>
    <div class="flex items-center gap-2 mb-3 px-2 py-1.5 rounded bg-surface-100 dark:bg-surface-800">
      <i class="pi pi-wrench text-primary-500"></i>
      <span class="text-sm font-medium truncate">{{ toolName || '未知工具' }}</span>
    </div>

    <Message v-if="!formData.toolId" severity="error" size="small" variant="simple">
      未指定工具，请重新从「添加节点 · 工具」选择
    </Message>

    <Fieldset v-if="inputSchema.length > 0" legend="入参">
      <div v-for="field in inputSchema" :key="field.field" class="mb-3">
        <div class="flex items-center justify-between mb-2">
          <label>{{ fieldLabel(field) }}<span v-if="field.required" class="text-red-500">*</span></label>
          <SelectButton
            v-model="inputLocations[field.field]"
            :options="fieldLocationOptions"
            option-label="label"
            option-value="value"
            size="small"
          />
        </div>
        <Cascader
          v-if="inputLocations[field.field] === 'reference'"
          placeholder="请选择变量"
          :config="{ labelKey: 'label', valueKey: 'value' }"
          :options="fieldOptions"
          v-model="inputReferences[field.field]"
          optionLabel="label"
          optionGroupChildren="children"
          class="w-full"
        />
        <InputText v-else v-model="inputValues[field.field]" :placeholder="fieldLabel(field)" class="w-full" />
      </div>
    </Fieldset>

    <Fieldset v-if="configSchema.length > 0" legend="配置" class="mt-3">
      <div v-for="field in configSchema" :key="field.field" class="mb-3">
        <label class="mb-2 block">
          {{ fieldLabel(field) }}
          <span v-if="isSecret(field)" class="text-xs text-surface-400">(密钥)</span>
        </label>
        <Password
          v-if="isSecret(field)"
          v-model="formData.config[field.field]"
          :feedback="false"
          toggle-mask
          class="w-full"
          input-class="w-full"
          placeholder="留空则使用工具默认值"
        />
        <InputText
          v-else
          v-model="formData.config[field.field]"
          placeholder="留空则使用工具默认值"
          class="w-full"
        />
      </div>
    </Fieldset>
  </div>
</template>

<script setup lang="ts">
import { inject, onMounted, reactive, ref } from 'vue'
import Cascader from '@/components/cascader/index.vue'
import type { BaseNodeModel } from '@logicflow/core'
import { cloneDeep } from 'lodash'
import toolApi, { type ToolDetail } from '@/api/tool'

const fieldLocationOptions = [
  { label: '引用', value: 'reference' },
  { label: '自定义', value: 'customize' }
]

const getModel = inject('getModel') as () => BaseNodeModel
const model = getModel()
const getNodeFieldOptions = inject('getNodeFieldOptions') as any
const fieldOptions = getNodeFieldOptions()

const formData = reactive<{ toolId: string; config: Record<string, any> }>({
  toolId: '',
  config: {}
})

const toolName = ref('')
const inputSchema = ref<any[]>([])
const outputSchema = ref<any[]>([])
const configSchema = ref<any[]>([])
const inputLocations = reactive<Record<string, 'reference' | 'customize'>>({})
const inputReferences = reactive<Record<string, string[]>>({})
const inputValues = reactive<Record<string, any>>({})

const errors = reactive<Record<string, string>>({})

const fieldLabel = (field: any) => {
  const label = field.label
  if (label && typeof label === 'object') return label.value || field.field
  return label || field.field
}
const isSecret = (field: any) => field.secret === true || field.type === 'PasswordInput'

const applySchemas = (detail: ToolDetail) => {
  toolName.value = detail.label || detail.name
  inputSchema.value = detail.inputSchema || []
  outputSchema.value = detail.outputSchema || []
  configSchema.value = detail.configSchema || []
  inputSchema.value.forEach((f: any) => {
    if (!inputLocations[f.field]) inputLocations[f.field] = 'reference'
    if (!inputReferences[f.field]) inputReferences[f.field] = []
  })
  const list = (outputSchema.value || []).map((o: any) => ({ label: fieldLabel(o), value: o.field }))
  list.push({ label: '执行结果', value: 'result' })
  model.properties.field_list = list
}

const loadDetail = async (toolId: string) => {
  if (!toolId) return
  const res = await toolApi.getResource(toolId)
  applySchemas(res.data)
}

const buildInputs = () =>
  inputSchema.value.map((f: any) => {
    const location = inputLocations[f.field] || 'reference'
    return {
      field: f.field,
      location,
      value: location === 'reference' ? inputReferences[f.field] || [] : inputValues[f.field]
    }
  })

function validate() {
  Object.keys(errors).forEach((k) => delete errors[k])
  if (!formData.toolId) errors.toolId = '未指定工具'
  const valid = Object.keys(errors).length === 0
  const values = { toolId: formData.toolId, inputs: buildInputs(), config: cloneDeep(formData.config) }
  return Promise.resolve({ values, errors: valid ? {} : errors })
}

function submit() {
  return validate().then(({ values, errors: errs }) => {
    if (Object.keys(errs).length === 0) {
      model.properties.nodeData = values
      return {} as Record<string, string>
    }
    return errs
  })
}

defineExpose({ validate, submit })

onMounted(async () => {
  const data = model.properties.nodeData ? cloneDeep(model.properties.nodeData) : null
  formData.toolId = data?.toolId || ''
  formData.config = data?.config || {}
  if (formData.toolId) {
    await loadDetail(formData.toolId)
    ;(data?.inputs || []).forEach((b: any) => {
      inputLocations[b.field] = b.location || 'reference'
      if (b.location === 'reference') inputReferences[b.field] = b.value || []
      else inputValues[b.field] = b.value
    })
  }
})
</script>

<style lang="scss" scoped></style>
