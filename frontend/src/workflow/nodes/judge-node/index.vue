<template>
  <SimpleNodeContainer
    ref="containerRef"
    :model="model"
    :validate="validate"
    :submit="submit"
  >
    <Content :workflowType="workflowType" :details="details" ref="contentRef" />

    <template #content>
      <div class="space-y-1.5 p-2" ref="branchContainerRef">
        <div v-for="branch in branches" :key="branch.id" class="grid grid-cols-[28px_1fr] gap-1.5">
          <div class="flex items-center justify-end pr-1 text-[11px] font-medium text-slate-400">
            {{ getBranchTitle(branch.type) }}
          </div>

          <div
            class="overflow-hidden rounded border bg-white"
            :class="hasBranchError(branch) ? 'border-red-300' : 'border-slate-200'"
          >
            <div
              v-if="branch.type === 'else'"
              class="flex h-8 items-center px-2 text-[12px] text-slate-500"
              @mouseenter="showPreview($event, getBranchTitle(branch.type), '其他所有情况')"
              @mouseleave="hidePreview"
            >
              其他所有情况
            </div>

            <template v-else>
              <div v-for="(condition, conditionIndex) in getConditions(branch)" :key="condition.id">
                <div class="grid min-h-8 grid-cols-[1fr_42px_1fr] items-center gap-1 px-1.5 py-1">
                  <!-- 左值 -->
                  <button
                    type="button"
                    class="truncate rounded px-2 py-1 text-left text-[12px]"
                    :class="
                      getConditionError(branch, condition).left
                        ? 'bg-red-50 text-red-600 ring-1 ring-red-200'
                        : 'bg-slate-100 text-slate-700 hover:bg-slate-200'
                    "
                    @mouseenter="
                      showPreview($event, '左值', formatVariable(condition.variable) || '选择变量')
                    "
                    @click="
                      showPreview($event, '左值', formatVariable(condition.variable) || '选择变量')
                    "
                    @mouseleave="hidePreview"
                    @focus="
                      showPreview($event, '左值', formatVariable(condition.variable) || '选择变量')
                    "
                    @blur="hidePreview"
                  >
                    {{ formatVariable(condition.variable) || '选择变量' }}
                  </button>

                  <!-- 比较符 -->
                  <div
                    class="truncate rounded px-1 py-1 text-center text-[11px]"
                    :class="
                      getConditionError(branch, condition).compare
                        ? 'bg-red-50 text-red-600 ring-1 ring-red-200'
                        : 'text-slate-500'
                    "
                    @mouseenter="showPreview($event, '比较符', getCompareText(condition.compare))"
                    @click="showPreview($event, '比较符', getCompareText(condition.compare))"
                    @mouseleave="hidePreview"
                  >
                    {{ getCompareText(condition.compare) }}
                  </div>

                  <!-- 右值 -->
                  <button
                    type="button"
                    class="truncate rounded px-2 py-1 text-left text-[12px]"
                    :class="
                      getConditionError(branch, condition).right
                        ? 'bg-red-50 text-red-600 ring-1 ring-red-200'
                        : 'bg-slate-100 text-slate-700 hover:bg-slate-200'
                    "
                    @mouseenter="showPreview($event, '右值', getRightValue(condition))"
                    @click="showPreview($event, '右值', getRightValue(condition))"
                    @mouseleave="hidePreview"
                    @focus="showPreview($event, '右值', getRightValue(condition))"
                    @blur="hidePreview"
                  >
                    {{ getRightValue(condition) }}
                  </button>
                </div>

                <div
                  v-if="conditionIndex < getConditions(branch).length - 1"
                  class="relative flex h-3 items-center justify-center"
                >
                  <div class="absolute left-0 right-0 top-1/2 border-t border-slate-100" />
                  <span class="relative bg-white px-1 text-[10px] text-slate-400">
                    {{ branch.logic === 'or' ? '或' : '且' }}
                  </span>
                </div>
              </div>
            </template>
          </div>
        </div>
      </div>

      <Popover ref="previewPopoverRef" @mouseenter="cancelHidePreview" @mouseleave="hidePreview">
        <div class="max-w-[260px] space-y-1">
          <div class="text-[12px] font-medium text-slate-700">
            {{ previewTitle }}
          </div>

          <div class="break-all text-[12px] leading-5 text-slate-500">
            {{ previewContent }}
          </div>
        </div>
      </Popover>
    </template>
  </SimpleNodeContainer>
</template>

<script setup lang="ts">
import SimpleNodeContainer from '@/workflow/common/SimpleNodeContainer.vue'
import type { BaseNodeModel } from '@logicflow/core'
import Content from './content/index.vue'
import { init } from './content'
import Popover from 'primevue/popover'
import { computed, inject, ref, onMounted, onBeforeUnmount } from 'vue'
import { WorkflowType, defaulBranches } from '@/workflow/common/data'
import { useNodeValidator } from '@/workflow/common/useNodeValidator'

import {
  type CompareValue,
  type FormErrors,
  type JudgeBranch,
  type JudgeCondition,
  buildValidationMap,
  compareTextMap,
  createFormResult,
  formatVariable,
  getBranchTitle,
  getConditionKey,
  getConditions,
  needRightValue
} from './type'

const workflowType = inject('WorkflowType') || WorkflowType.APPLICATION
const details = (inject('getDetails') as any)()
const getModel = inject('getModel') as () => BaseNodeModel
const model = getModel()

const contentRef = ref<InstanceType<typeof Content>>()

const previewPopoverRef = ref<InstanceType<typeof Popover>>()
const previewTitle = ref('')
const previewContent = ref('')

let previewTimer: number | undefined
const branchContainerRef = ref<HTMLElement>()
const branches = computed<JudgeBranch[]>(() => {
  const value = model.properties?.nodeData?.branches

  if (Array.isArray(value) && value.length > 0) {
    return value as JudgeBranch[]
  }

  return defaulBranches as JudgeBranch[]
})

const validationMap = computed(() => {
  return buildValidationMap(branches.value)
})

const firstErrorKey = computed(() => {
  for (const branch of branches.value) {
    if (branch.type === 'else') {
      continue
    }

    for (const condition of getConditions(branch)) {
      const key = getConditionKey(branch, condition)
      const error = validationMap.value[key]

      if (error?.message) {
        return key
      }
    }
  }

  return ''
})

const validate = () => {
  return contentRef.value?.validate?.() || validateCurrentBranches()
}
const containerRef = ref<InstanceType<typeof SimpleNodeContainer>>()
const submit = () => {
  const height = branchContainerRef.value!.clientHeight + 84
  console.log(height)
  model.height = height
  model.properties.height = height
  return contentRef.value?.submit?.() || validateCurrentBranches()
}

function validateCurrentBranches() {
  const errors: FormErrors = {}

  Object.entries(validationMap.value).forEach(([key, item]) => {
    if (item.message) {
      errors[key] = [{ message: item.message }]
    }
  })

  if (Object.keys(errors).length > 0) {
    errors.branches = [
      {
        message: validationMap.value[firstErrorKey.value]?.message || '判断器条件不完整'
      }
    ]
  }

  return Promise.resolve(createFormResult(branches.value, errors))
}

function getCompareText(compare?: CompareValue) {
  if (!compare) {
    return '比较'
  }

  return compareTextMap[compare] || compare
}

function getRightValue(condition: JudgeCondition) {
  if (condition.compare && !needRightValue(condition.compare)) {
    return '-'
  }

  return condition.value || '输入值'
}

function getConditionError(branch: JudgeBranch, condition: JudgeCondition) {
  return (
    validationMap.value[getConditionKey(branch, condition)] || {
      left: false,
      compare: false,
      right: false,
      message: ''
    }
  )
}

function hasBranchError(branch: JudgeBranch) {
  if (branch.type === 'else') {
    return false
  }

  return getConditions(branch).some((condition) => {
    return Boolean(getConditionError(branch, condition).message)
  })
}

function showPreview(event: Event, title: string, content: string) {
  cancelHidePreview()

  previewTitle.value = title
  previewContent.value = content

  previewPopoverRef.value?.show(event)
}

function hidePreview() {
  cancelHidePreview()

  previewTimer = window.setTimeout(() => {
    previewPopoverRef.value?.hide()
  }, 120)
}

function cancelHidePreview() {
  if (previewTimer) {
    window.clearTimeout(previewTimer)
    previewTimer = undefined
  }
}

useNodeValidator(model, containerRef)

onMounted(() => {
  init({ model, workflowType: workflowType as string, details })
  model.refreshDegrees()
})

onBeforeUnmount(() => {
  cancelHidePreview()
})
</script>

<style lang="scss" scoped></style>
