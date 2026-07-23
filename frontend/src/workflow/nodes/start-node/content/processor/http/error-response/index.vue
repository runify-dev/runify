<template>
  <Fieldset legend="错误响应">
    <SelectButton
      :model-value="localSource"
      @update:model-value="(v: any) => v && emit('update:source', v)"
      :options="sourceOptions"
      option-label="label"
      option-value="value"
      size="small"
      class="mb-3"
    />

    <template v-if="localSource === 'custom'">
      <div class="flex items-center justify-between">
        <div class="text-sm">{{ summary }}</div>
        <Button label="编辑" icon="pi pi-pencil" variant="text" size="small" @click="open" />
      </div>
      <div class="text-surface-500 text-xs mt-2 leading-5">
        入参校验失败时返回的响应;校验错误信息可在响应体中引用「错误信息」变量。
      </div>
    </template>
    <template v-else>
      <div class="text-surface-500 text-xs leading-5">
        使用项目的统一异常配置(项目 → 统一异常配置);项目未配置时返回默认信封
        <code>{ code: 400, message, data: null }</code>。
      </div>
    </template>

    <Dialog
      v-model:visible="visible"
      header="错误响应"
      append-to-body
      :modal="false"
      :style="{ width: '35rem' }"
    >
      <Message
        v-for="err in dialogErrors"
        :key="err"
        severity="error"
        size="small"
        class="mb-2"
        >{{ err }}</Message
      >

      <div class="mb-3">
        <label>状态码</label>
        <InputNumber v-model="draft.status" :use-grouping="false" fluid showButtons class="mt-2" />
      </div>

      <Handlers
        :parameters="draft.headers"
        @update:parameters="(v: any) => (draft.headers = v)"
      ></Handlers>

      <Fieldset legend="响应体">
        <div>
          <label>响应类型</label>
          <Select
            v-model="draft.contentType"
            class="mt-2"
            :options="contentTypeOptions"
            option-label="label"
            option-value="value"
            placeholder="请选择响应类型"
            fluid
          />
        </div>
        <div class="mt-2" v-if="draft.contentType === 'jsonFields'">
          <Parameters
            :parameters="draft.jsonFields"
            @update:parameters="(v: any) => (draft.jsonFields = v)"
          ></Parameters>
        </div>
        <div class="mt-2" v-if="draft.contentType === 'jsonObject'">
          <JsonObject
            :model-value="draft.jsonObject"
            @update:model-value="(v: any) => (draft.jsonObject = v)"
          ></JsonObject>
        </div>
        <div class="mt-2" v-if="draft.contentType === 'plainText'">
          <PlainText
            :model-value="draft.plainText"
            @update:model-value="(v: any) => (draft.plainText = v)"
          ></PlainText>
        </div>
      </Fieldset>

      <template #footer>
        <div class="flex justify-end gap-2">
          <Button label="取消" severity="secondary" @click="visible = false" />
          <Button label="确认" @click="confirm" />
        </div>
      </template>
    </Dialog>
  </Fieldset>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch, provide, inject, onMounted } from 'vue'
import { cloneDeep, isEqual } from 'lodash'
import Handlers from '@/workflow/nodes/response-node/components/handlers/index.vue'
import Parameters from '@/workflow/nodes/response-node/components/parameters/index.vue'
import JsonObject from '@/workflow/nodes/response-node/components/json-object/index.vue'
import PlainText from '@/workflow/nodes/response-node/components/plain-text/index.vue'
import { validateResponseConfig } from '@/workflow/nodes/response-node/components/validate-response'
import type { BaseNodeModel } from '@logicflow/core'
import bus from '@/bus'

const props = defineProps<{
  value?: Record<string, any>
  source?: string
}>()
const emit = defineEmits(['update:value', 'update:source'])

const sourceOptions = [
  { label: '使用统一配置', value: 'global' },
  { label: '自定义', value: 'custom' }
]
const localSource = computed(() => props.source || 'global')

const getModel = inject('getModel') as () => BaseNodeModel
const model = getModel()

// 错误响应里可引用的变量只有开始节点写入的「错误信息」,
// 就近覆盖响应节点子组件注入的 getNodeFieldOptions/getTemplateVariables
const errorFieldOptions = () => [
  {
    label: model.properties.name || '开始节点',
    value: model.id,
    disabled: true,
    children: [{ label: '错误信息', value: 'error' }]
  }
]
provide('getNodeFieldOptions', errorFieldOptions)
provide('getTemplateVariables', errorFieldOptions)

const contentTypeOptions = [
  { label: '字段配置', value: 'jsonFields' },
  { label: 'JSON对象', value: 'jsonObject' },
  { label: '文本', value: 'plainText' }
]

// 默认值贴合统一信封约定:HTTP 200 + {code:400, message:<错误信息>, data:null}
const defaultValue = () => ({
  status: 200,
  headers: [
    { field: 'content-type', value: 'application/json; charset=utf-8', location: 'customize' }
  ],
  contentType: 'jsonFields',
  jsonFields: [
    { field: 'code', description: '错误码', location: 'customize', type: 'integer', value: '400' },
    { field: 'message', description: '错误信息', location: 'reference', reference: [model.id, 'error'] },
    { field: 'data', description: '数据', location: 'customize', value: null }
  ],
  jsonObject: { value: '', location: 'customize' },
  plainText: { value: '', location: 'customize' }
})

// contentType 缺失说明是空配置或旧结构,回落默认值
const isValid = (v?: Record<string, any>) => !!v?.contentType
const saved = ref<any>(isValid(props.value) ? cloneDeep(props.value) : defaultValue())

// 父级 Form setValues 在 mounted 后异步发生,需同步已保存的配置进来;
// isEqual 守卫:自身 emit 回流的等值对象不再赋值
watch(
  () => props.value,
  (v) => {
    if (isValid(v) && !isEqual(v, saved.value)) saved.value = cloneDeep(v)
  }
)

onMounted(() => {
  // 空配置时把默认值回写给表单,保证保存时带上
  if (!isValid(props.value)) {
    emit('update:value', cloneDeep(saved.value))
  }
})

const summary = computed(() => {
  const s = saved.value
  const contentType = (
    s.headers?.find((h: any) => String(h.field).toLowerCase() === 'content-type')?.value || '—'
  )
    .split(';')[0]
    .trim()
  const bodyLabel =
    s.contentType === 'jsonFields'
      ? `字段配置(${s.jsonFields?.length || 0})`
      : s.contentType === 'jsonObject'
        ? 'JSON对象'
        : '文本'
  return `${s.status} · ${contentType} · ${bodyLabel}`
})

// ── Dialog:编辑副本 + 提交校验,不过不关 ──
const visible = ref(false)
const draft = reactive<any>(defaultValue())
const dialogErrors = ref<string[]>([])

const open = () => {
  Object.assign(draft, cloneDeep(saved.value))
  dialogErrors.value = []
  visible.value = true
}

const confirm = () => {
  const errors = validateResponseConfig(draft)
  if (errors.length) {
    dialogErrors.value = errors
    bus.emit('message:error', errors[0])
    return
  }
  saved.value = cloneDeep(draft)
  emit('update:value', cloneDeep(saved.value))
  visible.value = false
}
</script>
<style lang="scss" scoped></style>
