<template>
  <div>
    <Fieldset legend="写入内容">
      <div class="mb-3">
        <label class="inline-flex items-center gap-1">
          摘要引用（如 [外循环 / 摘要状态]）
          <i
            class="pi pi-info-circle text-surface-400 cursor-help"
            style="font-size: 0.85rem"
            v-tooltip.top="{ value: summaryTip, escape: false }"
          />
        </label>
        <Cascader
          placeholder="请选择摘要变量"
          :config="{ labelKey: 'label', valueKey: 'value' }"
          :options="fieldOptions"
          v-model="formData.summaryReference"
          optionLabel="label"
          optionGroupChildren="children"
          class="w-full mt-2"
        />
        <Message v-if="errors.summaryReference" severity="error" size="small" variant="simple">
          {{ errors.summaryReference }}
        </Message>
      </div>

      <div class="mb-3">
        <label>便签引用（如 [外循环 / 便签]，含产物）</label>
        <Cascader
          placeholder="请选择便签变量"
          :config="{ labelKey: 'label', valueKey: 'value' }"
          :options="fieldOptions"
          v-model="formData.factsReference"
          optionLabel="label"
          optionGroupChildren="children"
          class="w-full mt-2"
        />
      </div>

      <p class="text-sm text-slate-500 leading-6">
        对话结束时（循环之后）执行一次，把摘要 / 便签固化到跨对话存储；写入失败仅记日志，不阻断。
      </p>
    </Fieldset>
  </div>
</template>

<script setup lang="ts">
import { inject, onMounted, reactive } from 'vue'
import Cascader from '@/components/cascader/index.vue'
import Tooltip from 'primevue/tooltip'
import type { BaseNodeModel } from '@logicflow/core'
import { validate as validateNodeData } from './validator'
import { cloneDeep } from 'lodash'

// 局部注册 tooltip 指令（不依赖全局注册），供摘要引用的说明 icon 使用
const vTooltip = Tooltip

// 摘要引用说明：接「上下文压缩」节点摘要输出的固定结构
const summaryTip = [
  '接「上下文压缩」节点的摘要输出，固定结构：',
  '{',
  '&nbsp;&nbsp;text: 摘要正文,',
  '&nbsp;&nbsp;covered: 已覆盖条目数,',
  '&nbsp;&nbsp;seedCovered: 是否覆盖完历史,',
  '&nbsp;&nbsp;coveredUpto: 游标时间戳',
  '}',
  '游标随此对象自动推进 covered_upto，无需单独配置。'
].join('<br>')

const getModel = inject('getModel') as () => BaseNodeModel
const model = getModel()
const getNodeFieldOptions = inject('getNodeFieldOptions') as any
const fieldOptions = getNodeFieldOptions()

const formData = reactive({
  summaryReference: [] as string[],
  factsReference: [] as string[]
})

const errors = reactive<Record<string, string>>({})

function validate() {
  Object.keys(errors).forEach((k) => delete errors[k])
  const { errors: errs } = validateNodeData({ ...formData })
  Object.assign(errors, errs)
  const valid = Object.keys(errors).length === 0
  const values = cloneDeep({ ...formData })
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

onMounted(() => {
  if (model.properties.nodeData) {
    const data = cloneDeep(model.properties.nodeData)
    Object.assign(formData, {
      summaryReference: data.summaryReference || [],
      factsReference: data.factsReference || []
    })
  }
})
</script>

<style lang="scss" scoped></style>
