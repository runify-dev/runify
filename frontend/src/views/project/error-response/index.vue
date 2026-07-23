<template>
  <div class="flex flex-col h-full">
    <!-- 顶栏 -->
    <div
      class="flex items-center justify-between gap-4 px-6 py-4 border-b border-[var(--p-content-border-color)]"
    >
      <div class="flex items-center gap-3 min-w-0">
        <div
          class="w-10 h-10 shrink-0 rounded-lg bg-primary-50 text-primary-600 flex items-center justify-center"
        >
          <i class="pi pi-shield text-lg"></i>
        </div>
        <div class="min-w-0">
          <div class="text-base font-semibold text-surface-900">
            {{ t('project.unifiedErrorResponse') }}
          </div>
          <div class="text-xs text-surface-500 mt-0.5 truncate">
            项目下所有处理器入参校验失败时的默认响应;开始节点选「自定义」可覆盖
          </div>
        </div>
      </div>
      <Button
        icon="pi pi-save"
        :label="t('common.save')"
        class="shrink-0"
        :loading="saving"
        @click="save"
      />
    </div>

    <!-- 内容区:左表单 / 右预览 -->
    <div class="flex-1 overflow-auto p-6">
      <div class="grid grid-cols-1 xl:grid-cols-5 gap-6 max-w-6xl">
        <div class="xl:col-span-3 flex flex-col gap-4">
          <Message v-for="err in errors" :key="err" severity="error" size="small">{{
            err
          }}</Message>

          <Fieldset legend="基本设置">
            <label>状态码</label>
            <InputNumber
              v-model="config.status"
              :use-grouping="false"
              fluid
              showButtons
              class="mt-2"
            />
          </Fieldset>

          <Handlers
            :parameters="config.headers"
            @update:parameters="(v: any) => (config.headers = v)"
          ></Handlers>

          <Fieldset legend="响应体">
            <div>
              <label>响应类型</label>
              <Select
                v-model="config.contentType"
                class="mt-2"
                :options="contentTypeOptions"
                option-label="label"
                option-value="value"
                placeholder="请选择响应类型"
                fluid
              />
            </div>
            <div class="mt-2" v-if="config.contentType === 'jsonFields'">
              <Parameters
                :parameters="config.jsonFields"
                @update:parameters="(v: any) => (config.jsonFields = v)"
              ></Parameters>
            </div>
            <div class="mt-2" v-if="config.contentType === 'jsonObject'">
              <JsonObject
                :model-value="config.jsonObject"
                @update:model-value="(v: any) => (config.jsonObject = v)"
              ></JsonObject>
            </div>
            <div class="mt-2" v-if="config.contentType === 'plainText'">
              <PlainText
                :model-value="config.plainText"
                @update:model-value="(v: any) => (config.plainText = v)"
              ></PlainText>
            </div>
          </Fieldset>
        </div>

        <!-- 响应预览 -->
        <div class="xl:col-span-2">
          <div
            class="sticky top-0 rounded-xl border border-[var(--p-content-border-color)] bg-[var(--p-surface-0)] overflow-hidden"
          >
            <div
              class="flex items-center justify-between px-4 py-3 border-b border-[var(--p-content-border-color)] bg-surface-50"
            >
              <span class="text-sm font-medium text-surface-700">
                <i class="pi pi-eye mr-1.5 text-surface-400"></i>响应预览
              </span>
              <span
                class="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium"
                :class="
                  config.status >= 200 && config.status < 300
                    ? 'bg-green-50 text-green-700'
                    : 'bg-orange-50 text-orange-700'
                "
              >
                HTTP {{ config.status }}
              </span>
            </div>

            <div class="px-4 py-3 border-b border-[var(--p-content-border-color)]">
              <div class="text-xs text-surface-400 mb-1.5">Headers</div>
              <div
                v-for="header in config.headers"
                :key="header.field"
                class="text-xs font-mono text-surface-600 leading-5 break-all"
              >
                <span class="text-surface-900">{{ header.field }}</span
                >:
                {{ header.location === 'reference' ? '<引用变量>' : header.value }}
              </div>
              <div v-if="!config.headers?.length" class="text-xs text-surface-400">-</div>
            </div>

            <div class="px-4 py-3">
              <div class="text-xs text-surface-400 mb-1.5">Body</div>
              <pre
                class="text-xs font-mono text-surface-700 leading-5 whitespace-pre-wrap break-all m-0"
                >{{ previewBody }}</pre
              >
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, provide, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { t } from '@/locales'
import bus from '@/bus'
import projectAPI from '@/api/project'
import Handlers from '@/workflow/nodes/response-node/components/handlers/index.vue'
import Parameters from '@/workflow/nodes/response-node/components/parameters/index.vue'
import JsonObject from '@/workflow/nodes/response-node/components/json-object/index.vue'
import PlainText from '@/workflow/nodes/response-node/components/plain-text/index.vue'
import { validateResponseConfig } from '@/workflow/nodes/response-node/components/validate-response'

const route = useRoute()

// 开始节点画布 id 固定为 'start-node',错误信息引用跨处理器可移植
const errorFieldOptions = () => [
  {
    label: '开始节点',
    value: 'start-node',
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
    {
      field: 'message',
      description: '错误信息',
      location: 'reference',
      reference: ['start-node', 'error']
    },
    { field: 'data', description: '数据', location: 'customize', value: null }
  ],
  jsonObject: { value: '', location: 'customize' },
  plainText: { value: '', location: 'customize' }
})

const config = reactive<any>(defaultValue())
const errors = ref<string[]>([])
const saving = ref(false)

onMounted(() => {
  projectAPI.getErrorResponse(route.params.id as string).then((ok) => {
    if (ok.data?.validationError?.contentType) {
      Object.assign(config, ok.data.validationError)
    }
  })
})

// 与后端 ResponseRenderer.coerceLenient 同口径:按声明类型展示为数字
const displayValue = (value: any, type?: string) => {
  if (typeof value !== 'string' || !type) return value
  if (['integer', 'long'].includes(type)) {
    const n = parseInt(value.trim(), 10)
    return Number.isNaN(n) ? value : n
  }
  if (type === 'double') {
    const n = parseFloat(value.trim())
    return Number.isNaN(n) ? value : n
  }
  return value
}

const previewBody = computed(() => {
  if (config.contentType === 'jsonFields') {
    const obj: Record<string, any> = {}
    for (const field of config.jsonFields || []) {
      obj[field.field] =
        field.location === 'reference' ? '<错误信息>' : displayValue(field.value, field.type)
    }
    return JSON.stringify(obj, null, 2)
  }
  if (config.contentType === 'jsonObject') {
    if (config.jsonObject?.location === 'reference') return '<引用变量>'
    try {
      return JSON.stringify(JSON.parse(config.jsonObject?.value || ''), null, 2)
    } catch {
      return config.jsonObject?.value || ''
    }
  }
  return config.plainText?.location === 'reference' ? '<引用变量>' : config.plainText?.value || ''
})

const save = () => {
  errors.value = validateResponseConfig(config)
  if (errors.value.length) {
    bus.emit('message:error', errors.value[0])
    return
  }
  projectAPI
    .editErrorResponse(route.params.id as string, { validationError: { ...config } }, saving)
    .then(() => {
      bus.emit('message:success', t('common.saveSuccess'))
    })
}
</script>
<style lang="scss" scoped></style>
