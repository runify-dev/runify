<template>
  <div class="bg-white">
    <div class="flex h-10 items-center justify-between border-b border-slate-100 px-3">
      <div class="text-[14px] font-semibold text-slate-800">跳过条件</div>
    </div>

    <div class="p-3">
      <div class="rounded-lg border border-slate-200 bg-white">
        <div class="px-3 pt-2 pb-1 text-[12px] text-slate-400">
          {{ conditions.length > 0 ? '满足以下条件时，跳过当前循环迭代' : '无条件时，直接跳过当前循环迭代' }}
        </div>

        <div class="px-3 pb-2">
          <div class="flex items-stretch">
            <div
              v-if="conditions.length > 1"
              class="relative flex w-8 shrink-0 items-center justify-center"
            >
              <div class="absolute bottom-[27px] left-[15px] top-[13px] w-px bg-slate-300" />
              <div class="absolute left-[15px] top-[13px] h-px w-[13px] bg-slate-300" />
              <div class="absolute bottom-[27px] left-[15px] h-px w-[13px] bg-slate-300" />

              <Select
                v-model="logic"
                :options="logicOptions"
                optionLabel="label"
                optionValue="value"
                size="small"
                class="logic-select relative z-10"
              />
            </div>

            <div class="min-w-0 flex-1 space-y-1.5">
              <div v-for="condition in conditions" :key="condition.id">
                <div class="grid grid-cols-[40px_minmax(0,1fr)_22px] items-start gap-1">
                  <div class="pt-6">
                    <Select
                      v-model="condition.compare"
                      :options="compareOptions"
                      optionLabel="label"
                      optionValue="value"
                      size="small"
                      placeholder="请"
                      class="compare-select"
                      :class="{ 'p-invalid': showError(condition, 'compare') }"
                      @change="onCompareChange(condition)"
                    />

                    <div
                      v-if="showError(condition, 'compare')"
                      class="mt-0.5 text-[10px] leading-3 text-red-500"
                    >
                      条件不可为空
                    </div>
                  </div>

                  <div class="min-w-0">
                    <Cascader
                      :config="{ labelKey: 'label', valueKey: 'value' }"
                      :options="options"
                      :model-value="condition.variable"
                      @update:model-value="(v) => condition.variable = v"
                      optionLabel="label"
                      optionGroupChildren="children"
                      placeholder="请选择"
                      size="small"
                      class="variable-select w-full"
                      :class="{ 'p-invalid': showError(condition, 'variable') }"
                    />

                    <div
                      v-if="showError(condition, 'variable')"
                      class="mt-0.5 text-[10px] leading-3 text-red-500"
                    >
                      变量值不可为空
                    </div>

                    <div v-if="needRightValue(condition.compare)" class="mt-1 flex gap-1">
                      <Select
                        :modelValue="getValueMode(condition)"
                        :options="valueModeOptions"
                        optionLabel="label"
                        optionValue="value"
                        size="small"
                        class="value-mode-select shrink-0"
                        @update:modelValue="(mode) => onValueModeChange(condition, mode)"
                      />

                      <InputText
                        v-if="getValueMode(condition) === 'str'"
                        v-model="condition.value"
                        size="small"
                        placeholder="输入或引用参数值"
                        class="value-input min-w-0 flex-1"
                        :class="{ 'p-invalid': showError(condition, 'value') }"
                      />

                      <Cascader
                        v-else
                        :config="{ labelKey: 'label', valueKey: 'value' }"
                        :options="options"
                        :model-value="parseVariableExpression(condition.value)"
                        @update:model-value="(v) => onValueVariableChange(condition, v)"
                        optionLabel="label"
                        optionGroupChildren="children"
                        placeholder="请选择变量"
                        size="small"
                        class="value-input min-w-0 flex-1"
                        :class="{ 'p-invalid': showError(condition, 'value') }"
                      />
                    </div>

                    <div
                      v-else
                      class="mt-1 flex h-6 items-center rounded border border-slate-200 bg-slate-50 px-1.5 text-[11px] text-slate-400"
                    >
                      当前条件不需要右值
                    </div>

                    <div
                      v-if="showError(condition, 'value') && needRightValue(condition.compare)"
                      class="mt-0.5 text-[10px] leading-3 text-red-500"
                    >
                      变量值不可为空
                    </div>
                  </div>

                  <div class="pt-6">
                    <Button
                      icon="pi pi-minus"
                      size="small"
                      text
                      rounded
                      severity="secondary"
                      class="!h-6 !w-6 !text-slate-400"
                      @click="removeCondition(condition.id)"
                    />
                  </div>
                </div>
              </div>

              <div class="flex justify-center pt-1">
                <Button
                  label="新增"
                  icon="pi pi-plus"
                  size="small"
                  class="!h-7 !rounded-md !border-none !bg-indigo-50 !px-2.5 !text-[11px] !text-indigo-500 hover:!bg-indigo-100"
                  @click="addCondition"
                />
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { BaseNodeModel } from '@logicflow/core'
import { computed, inject, onMounted, ref } from 'vue'
import cloneDeep from 'lodash/cloneDeep'

import Button from 'primevue/button'
import Select from 'primevue/select'
import InputText from 'primevue/inputtext'
import Cascader from '@/components/cascader/index.vue'

import {
  type BranchLogic,
  type CascaderOption,
  type FormErrors,
  type JudgeCondition,
  type ValueMode,
  compareOptions,
  createCondition,
  createFormResult,
  getConditionErrors,
  logicOptions,
  needRightValue,
  parseVariableExpression,
  valueModeOptions,
  isVariableExpression
} from '@/workflow/nodes/judge-node/type'
import { validate as validateNodeData } from './validator'

const getModel = inject('getModel') as () => BaseNodeModel
const model = getModel()

const getNodeFieldOptions = inject('getNodeFieldOptions') as () => CascaderOption[]

const options = computed<CascaderOption[]>(() => {
  const result = getNodeFieldOptions?.()
  return Array.isArray(result) ? result : []
})

const conditions = ref<JudgeCondition[]>([])
const logic = ref<BranchLogic>('and')
const valueModeMap = ref<Record<string, ValueMode>>({})

onMounted(() => {
  const nodeData = model.properties?.nodeData
  if (nodeData?.conditions?.length) {
    conditions.value = cloneDeep(nodeData.conditions)
    logic.value = nodeData.logic || 'and'
  } else {
    conditions.value = []
    logic.value = 'and'
  }
  initValueModeMap()
})

function initValueModeMap() {
  const map: Record<string, ValueMode> = {}
  conditions.value.forEach((condition) => {
    map[condition.id] = isVariableExpression(condition.value) ? 'var' : 'str'
  })
  valueModeMap.value = map
}

function addCondition() {
  conditions.value.push(createCondition())
}

function removeCondition(conditionId: string) {
  conditions.value = conditions.value.filter((item) => item.id !== conditionId)
}

function onVariableChange(condition: JudgeCondition, value: string[]) {
  condition.variable = value
}

function onCompareChange(condition: JudgeCondition) {
  if (!needRightValue(condition.compare)) {
    condition.value = ''
  }
}

function onValueModeChange(condition: JudgeCondition, mode: ValueMode) {
  valueModeMap.value[condition.id] = mode
  condition.value = ''
}

function onValueVariableChange(condition: JudgeCondition, value: string[]) {
  if (!value.length) {
    condition.value = ''
    return
  }
  condition.value = `\${${value.join('.')}}`
}

function getValueMode(condition: JudgeCondition): ValueMode {
  if (valueModeMap.value[condition.id]) {
    return valueModeMap.value[condition.id]
  }
  return isVariableExpression(condition.value) ? 'var' : 'str'
}

function showError(condition: JudgeCondition, field: 'variable' | 'compare' | 'value') {
  return getConditionErrors(condition)[field]
}

function validate() {
  return Promise.resolve(buildFormResult())
}

function submit() {
  const values = buildFormResult()
  model.properties.nodeData = values.values
  return Promise.resolve(values)
}

function buildFormResult() {
  const errors: FormErrors = {}

  if (conditions.value.length > 0) {
    conditions.value.forEach((condition) => {
      const conditionErrors = getConditionErrors(condition)

      if (conditionErrors.variable) {
        errors[`condition_${condition.id}_variable`] = [{ message: '变量值不可为空' }]
      }
      if (conditionErrors.compare) {
        errors[`condition_${condition.id}_compare`] = [{ message: '条件不可为空' }]
      }
      if (conditionErrors.value) {
        errors[`condition_${condition.id}_value`] = [{ message: '变量值不可为空' }]
      }
    })

    if (Object.keys(errors).length > 0) {
      errors.conditions = [{ message: '跳过条件配置不完整' }]
    }
  }

  return {
    valid: Object.keys(errors).length === 0,
    errors,
    values: {
      conditions: conditions.value.map((c) => ({
        id: c.id,
        variable: [...c.variable],
        compare: c.compare,
        value: c.value || ''
      })),
      logic: logic.value
    }
  }
}

defineExpose({ validate, submit })
</script>

<style scoped>
:deep(.logic-select) {
  width: 30px;
}

:deep(.compare-select) {
  width: 40px;
}

:deep(.value-mode-select) {
  width: 38px;
}

:deep(.p-cascadeselect),
:deep(.p-select),
:deep(.p-inputtext) {
  min-height: 24px;
  height: 24px;
  border-radius: 5px;
  font-size: 11px;
}

:deep(.p-cascadeselect-label),
:deep(.p-select-label) {
  padding-top: 2px;
  padding-bottom: 2px;
  font-size: 11px;
  line-height: 18px;
}

:deep(.p-inputtext) {
  padding: 2px 6px;
  font-size: 11px;
  line-height: 18px;
}

:deep(.p-select-dropdown),
:deep(.p-cascadeselect-dropdown) {
  width: 16px;
}

:deep(.p-invalid) {
  border-color: #ef4444 !important;
}
</style>
