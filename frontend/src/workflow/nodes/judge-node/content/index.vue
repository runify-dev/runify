<template>
  <div class="bg-white">
    <div class="flex h-10 items-center justify-between border-b border-slate-100 px-3">
      <div class="text-[14px] font-semibold text-slate-800">条件分支</div>

      <Button
        icon="pi pi-plus"
        size="small"
        text
        rounded
        class="!h-7 !w-7 !bg-indigo-50 !text-indigo-500 hover:!bg-indigo-100"
        @click="addElseIfBranch"
      />
    </div>

    <div class="space-y-2 p-3">
      <div
        v-for="branch in branches"
        :key="branch.id"
        class="rounded-lg border border-slate-200 bg-white"
      >
        <div class="flex h-8 items-center justify-between px-3">
          <div class="text-[13px] font-medium text-slate-700">
            {{ getBranchTitle(branch.type) }}
          </div>

          <Button
            v-if="branch.type === 'elseif'"
            icon="pi pi-minus"
            size="small"
            text
            rounded
            severity="secondary"
            class="!h-6 !w-6 !text-slate-400"
            @click="removeBranch(branch.id)"
          />

          <span v-else class="flex h-6 w-6 items-center justify-center text-slate-300"> - </span>
        </div>

        <div v-if="branch.type === 'else'" class="px-3 pb-2 text-[12px] text-slate-400">
          以上条件都不满足时，进入该分支
        </div>

        <div v-else class="px-3 pb-2">
          <div class="flex items-stretch">
            <div
              v-if="getConditions(branch).length > 1"
              class="relative flex w-8 shrink-0 items-center justify-center"
            >
              <div class="absolute bottom-[27px] left-[15px] top-[13px] w-px bg-slate-300" />
              <div class="absolute left-[15px] top-[13px] h-px w-[13px] bg-slate-300" />
              <div class="absolute bottom-[27px] left-[15px] h-px w-[13px] bg-slate-300" />

              <Select
                v-model="branch.logic"
                :options="logicOptions"
                optionLabel="label"
                optionValue="value"
                size="small"
                class="logic-select relative z-10"
              />
            </div>

            <div class="min-w-0 flex-1 space-y-1.5">
              <div v-for="condition in getConditions(branch)" :key="condition.id">
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
                    <CascadeSelect
                      :modelValue="findOptionByPath(options, condition.variable)"
                      :options="options"
                      :optionGroupChildren="optionGroupChildren"
                      optionLabel="label"
                      optionGroupLabel="label"
                      placeholder="请选择"
                      size="small"
                      class="variable-select w-full"
                      :class="{ 'p-invalid': showError(condition, 'variable') }"
                      @update:modelValue="(value) => onVariableChange(condition, value)"
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

                      <CascadeSelect
                        v-else
                        :modelValue="
                          findOptionByPath(options, parseVariableExpression(condition.value))
                        "
                        :options="options"
                        :optionGroupChildren="optionGroupChildren"
                        optionLabel="label"
                        optionGroupLabel="label"
                        placeholder="请选择变量"
                        size="small"
                        class="value-input min-w-0 flex-1"
                        :class="{ 'p-invalid': showError(condition, 'value') }"
                        @update:modelValue="(value) => onValueVariableChange(condition, value)"
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
                      :disabled="getConditions(branch).length <= 1"
                      @click="removeCondition(branch, condition.id)"
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
                  @click="addCondition(branch)"
                />
              </div>
            </div>
          </div>
        </div>
      </div>

      <div
        v-if="!hasElseBranch"
        class="rounded-md border border-red-200 bg-red-50 px-2 py-1.5 text-[11px] text-red-500"
      >
        必须保留否则分支
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { BaseNodeModel } from '@logicflow/core'
import { computed, inject, onMounted, ref } from 'vue'
import cloneDeep from 'lodash/cloneDeep'
import { defaulBranches } from '@/workflow/common/data'

import Button from 'primevue/button'
import Select from 'primevue/select'
import InputText from 'primevue/inputtext'
import CascadeSelect from 'primevue/cascadeselect'

import {
  type BranchType,
  type CascaderOption,
  type FormErrors,
  type JudgeBranch,
  type JudgeCondition,
  type ValueMode,
  buildOptionGroupChildren,
  compareOptions,
  createBranch,
  createCondition,
  createFormResult,
  ensureConditions,
  findOptionByPath,
  getBranchTitle,
  getCascaderOptionPath,
  getConditionErrors,
  getConditions,
  logicOptions,
  needRightValue,
  parseVariableExpression,
  valueModeOptions,
  isVariableExpression
} from '../type'
import { validate as validateNodeData } from './validator'

defineProps<{
  WorkflowType?: string
  details?: any
}>()

const getModel = inject('getModel') as () => BaseNodeModel
const model = getModel()

const getNodeFieldOptions = inject('getNodeFieldOptions') as () => CascaderOption[]
const containerRef = ref()
const branches = ref<JudgeBranch[]>([])
const valueModeMap = ref<Record<string, ValueMode>>({})

const options = computed<CascaderOption[]>(() => {
  const result = getNodeFieldOptions?.()
  return Array.isArray(result) ? result : []
})

const optionGroupChildren = computed(() => {
  return buildOptionGroupChildren(options.value)
})

const hasElseBranch = computed(() => {
  return branches.value.some((item) => item.type === 'else')
})

onMounted(() => {
  branches.value = getInitialBranches()
  initValueModeMap()
})

function getInitialBranches() {
  const source = model.properties?.nodeData?.branches

  const result = cloneDeep(
    Array.isArray(source) && source.length > 0
      ? (source as JudgeBranch[])
      : (defaulBranches as JudgeBranch[])
  )

  if (!result.some((item) => item.type === 'if')) {
    const ifBranch =
      cloneDeep(defaulBranches as JudgeBranch[]).find((item) => item.type === 'if') ||
      createBranch('if')

    result.unshift(ifBranch)
  }

  if (!result.some((item) => item.type === 'else')) {
    const elseBranch =
      cloneDeep(defaulBranches as JudgeBranch[]).find((item) => item.type === 'else') ||
      createBranch('else')

    result.push(elseBranch)
  }

  result.forEach((branch) => {
    if (branch.type !== 'else') {
      ensureConditions(branch)
    }
  })

  return result
}

function addElseIfBranch() {
  const elseIndex = branches.value.findIndex((item) => item.type === 'else')
  const branch = createBranch('elseif')

  if (elseIndex === -1) {
    branches.value.push(branch, createBranch('else'))
  } else {
    branches.value.splice(elseIndex, 0, branch)
  }
}

function removeBranch(branchId: string) {
  const branch = branches.value.find((item) => item.id === branchId)

  if (!branch || branch.type !== 'elseif') {
    return
  }

  branches.value = branches.value.filter((item) => item.id !== branchId)
}

function addCondition(branch: JudgeBranch) {
  if (branch.type === 'else') {
    return
  }

  ensureConditions(branch)
  branch.conditions!.push(createCondition())
}

function removeCondition(branch: JudgeBranch, conditionId: string) {
  if (branch.type === 'else') {
    return
  }

  const conditions = ensureConditions(branch)

  if (conditions.length <= 1) {
    return
  }

  branch.conditions = conditions.filter((item) => item.id !== conditionId)
}

function onVariableChange(condition: JudgeCondition, value: unknown) {
  condition.variable = getCascaderOptionPath(options.value, value)
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

function onValueVariableChange(condition: JudgeCondition, value: unknown) {
  const path = getCascaderOptionPath(options.value, value)

  if (!path.length) {
    condition.value = ''
    return
  }

  condition.value = `\${${path.join('.')}}`
}

function getValueMode(condition: JudgeCondition): ValueMode {
  if (valueModeMap.value[condition.id]) {
    return valueModeMap.value[condition.id]
  }

  return isVariableExpression(condition.value) ? 'var' : 'str'
}

function initValueModeMap() {
  const map: Record<string, ValueMode> = {}

  branches.value.forEach((branch) => {
    getConditions(branch).forEach((condition) => {
      map[condition.id] = isVariableExpression(condition.value) ? 'var' : 'str'
    })
  })

  valueModeMap.value = map
}

function validate() {
  return Promise.resolve(buildFormResult())
}

function submit() {
  const values = buildFormResult()
  model.properties.nodeData = values.values
  model.refreshDegrees()
  return Promise.resolve(values)
}

function buildFormResult() {
  const errors: FormErrors = {}

  branches.value.forEach((branch) => {
    if (branch.type === 'else') {
      return
    }

    getConditions(branch).forEach((condition) => {
      const conditionErrors = getConditionErrors(condition)

      if (conditionErrors.variable) {
        errors[`${branch.id}_${condition.id}_variable`] = [{ message: '变量值不可为空' }]
      }

      if (conditionErrors.compare) {
        errors[`${branch.id}_${condition.id}_compare`] = [{ message: '条件不可为空' }]
      }

      if (conditionErrors.value) {
        errors[`${branch.id}_${condition.id}_value`] = [{ message: '变量值不可为空' }]
      }
    })
  })

  if (Object.keys(errors).length > 0) {
    errors.branches = [{ message: '条件分支配置不完整' }]
  }

  return createFormResult(branches.value, errors)
}

function showError(condition: JudgeCondition, field: 'variable' | 'compare' | 'value') {
  return getConditionErrors(condition)[field]
}

defineExpose({
  validate,
  submit,
  getBranches: () => createFormResult(branches.value).values.branches
})
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
