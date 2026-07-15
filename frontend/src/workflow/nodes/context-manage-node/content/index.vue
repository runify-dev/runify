<template>
  <div>
    <Fieldset legend="上下文（messages）">
      <div class="mb-3">
        <label>初始值 · 入参（必填，待压缩上下文：如「上下文查询」节点的历史）</label>
        <Cascader
          placeholder="请选择待压缩的上下文变量"
          :config="{ labelKey: 'label', valueKey: 'value' }"
          :options="fieldOptions"
          v-model="formData.sourceSeedVariable"
          optionLabel="label"
          optionGroupChildren="children"
          class="w-full mt-2"
        />
        <Message v-if="errors.sourceSeedVariable" severity="error" size="small" variant="simple">
          {{ errors.sourceSeedVariable }}
        </Message>
      </div>
      <div class="mb-3">
        <label>变化值 · 出参（可选，压缩+拼接摘要/便签后的结果写回此变量）</label>
        <Cascader
          placeholder="不选则只从 messages 输出口取用"
          :config="{ labelKey: 'label', valueKey: 'value' }"
          :options="fieldOptions"
          v-model="formData.sourceVariable"
          optionLabel="label"
          optionGroupChildren="children"
          class="w-full mt-2"
        />
        <Message v-if="errors.sourceVariable" severity="error" size="small" variant="simple">
          {{ errors.sourceVariable }}
        </Message>
      </div>
      <p class="text-xs text-surface-400 dark:text-surface-500 mt-1">流向：初始值(入参) → 压缩并拼接摘要/便签 → 写入变化值(出参)。出参须与入参不同；ai-chat 直接引用变化值即可（也可用 messages 输出口）。</p>
    </Fieldset>

    <Fieldset legend="摘要">
      <div class="mb-3">
        <label>初始值（可选：跨对话摘要种子，仅变化值为空的首轮生效）</label>
        <Cascader
          placeholder="不选则从零开始"
          :config="{ labelKey: 'label', valueKey: 'value' }"
          :options="fieldOptions"
          v-model="formData.summarySeedVariable"
          optionLabel="label"
          optionGroupChildren="children"
          class="w-full mt-2"
        />
      </div>
      <div class="mb-3">
        <label>变化值（读取并写回，需指向循环变量等跨迭代持久的变量）</label>
        <Cascader
          placeholder="不选则摘要不跨轮保留"
          :config="{ labelKey: 'label', valueKey: 'value' }"
          :options="fieldOptions"
          v-model="formData.summaryVariable"
          optionLabel="label"
          optionGroupChildren="children"
          class="w-full mt-2"
        />
      </div>
    </Fieldset>

    <Fieldset legend="便签（含产物）">
      <div class="mb-3">
        <label>初始值（可选：常驻便签种子，仅变化值为空的首轮生效）</label>
        <Cascader
          placeholder="不选则从零开始"
          :config="{ labelKey: 'label', valueKey: 'value' }"
          :options="fieldOptions"
          v-model="formData.factsSeedVariable"
          optionLabel="label"
          optionGroupChildren="children"
          class="w-full mt-2"
        />
      </div>
      <div class="mb-3">
        <label>变化值（读取并写回，需指向循环变量等跨迭代持久的变量）</label>
        <Cascader
          placeholder="不选则便签不跨轮保留"
          :config="{ labelKey: 'label', valueKey: 'value' }"
          :options="fieldOptions"
          v-model="formData.factsVariable"
          optionLabel="label"
          optionGroupChildren="children"
          class="w-full mt-2"
        />
      </div>
      <div class="mb-3">
        <label>提取哪些便签子区</label>
        <MultiSelect
          v-model="formData.factSections"
          :options="sectionOptions"
          optionLabel="label"
          optionValue="value"
          placeholder="不选则使用全部启用子区"
          class="w-full mt-2"
        />
        <div class="text-xs text-surface-400 dark:text-surface-500 mt-1">
          选项来自应用「便签设置」（含自定义子区）；留空表示压缩时纳入全部启用的子区。
        </div>
      </div>
    </Fieldset>

    <Fieldset legend="预算与压缩">
      <!-- 预算 -->
      <div class="mb-3">
        <label>上下文预算（token）</label>
        <InputNumber
          v-model="formData.budget"
          :min="1000"
          :step="1000"
          class="w-full mt-2"
        />
        <Message v-if="errors.budget" severity="error" size="small" variant="simple">
          {{ errors.budget }}
        </Message>
      </div>

      <!-- 保护最近条数 -->
      <div class="mb-3">
        <label>保护最近内容条数（不压缩）</label>
        <InputNumber
          v-model="formData.keepRecentItems"
          :min="0"
          :max="100"
          class="w-full mt-2"
        />
      </div>

      <!-- 剥离多模态 -->
      <div class="mb-3 flex items-center justify-between">
        <label>剥离多模态内容（模型不支持图片/视频时开启）</label>
        <ToggleSwitch v-model="formData.stripMultimodal" />
      </div>
    </Fieldset>

    <Fieldset legend="LLM 摘要">
      <div class="mb-3 flex items-center justify-between">
        <label>启用 LLM 摘要（仅规则压缩后仍超阈值时调用一次，失败自动回退抽取式）</label>
        <ToggleSwitch v-model="formData.enableSummarizer" />
      </div>
      <div v-if="formData.enableSummarizer" class="mb-3">
        <label>摘要模型</label>
        <Select
          v-model="formData.summarizerModelId"
          :options="modelList"
          optionLabel="name"
          optionValue="id"
          placeholder="请选择模型"
          class="w-full mt-2"
        />
        <Message v-if="errors.summarizerModelId" severity="error" size="small" variant="simple">
          {{ errors.summarizerModelId }}
        </Message>
      </div>
      <div v-if="formData.enableSummarizer" class="mb-3">
        <label>摘要方法</label>
        <Select
          v-model="formData.summarizerMethod"
          :options="summarizerMethodOptions"
          optionLabel="label"
          option-value="value"
          class="w-full mt-2"
        />
      </div>
    </Fieldset>

    <Fieldset legend="高级">
      <div class="mb-3">
        <label>token 编码</label>
        <Select
          v-model="formData.tokenEncoding"
          :options="tokenEncodingOptions"
          optionLabel="label"
          option-value="value"
          class="w-full mt-2"
        />
      </div>
      <div class="mb-3">
        <label>高水位比例</label>
        <InputNumber
          v-model="formData.highRatio"
          :min="0.1"
          :max="1"
          :step="0.05"
          :maxFractionDigits="2"
          class="w-full mt-2"
        />
      </div>
      <div class="mb-3">
        <label>低水位比例</label>
        <InputNumber
          v-model="formData.lowRatio"
          :min="0.1"
          :max="1"
          :step="0.05"
          :maxFractionDigits="2"
          class="w-full mt-2"
        />
        <Message v-if="errors.lowRatio" severity="error" size="small" variant="simple">
          {{ errors.lowRatio }}
        </Message>
      </div>
      <div class="mb-3">
        <label>固定开销预留（token）</label>
        <InputNumber
          v-model="formData.reservedTokens"
          :min="0"
          :step="500"
          class="w-full mt-2"
        />
      </div>
    </Fieldset>
  </div>
</template>

<script setup lang="ts">
import { inject, onMounted, reactive, ref } from 'vue'
// 显式 import PrimeVue 的 MultiSelect：否则被 dynamics-form-plus 的同名组件全局遮蔽，
// 便签子区会错误渲染成"选项显示字段/选项值/选项数据"编辑器
import MultiSelect from 'primevue/multiselect'
import Cascader from '@/components/cascader/index.vue'
import type { BaseNodeModel } from '@logicflow/core'
import { summarizerMethodOptions, tokenEncodingOptions } from './type'
import { validate as validateNodeData } from './validator'
import { cloneDeep } from 'lodash'
import { TreeCommonAPI } from '@/api/tree'
import { ROOT_FOLDER_ID } from '@/constants/common.ts'
import { useRoute } from 'vue-router'
import ApplicationAPI from '@/api/application'

const getModel = inject('getModel') as () => BaseNodeModel
const model = getModel()
const getNodeFieldOptions = inject('getNodeFieldOptions') as any
const fieldOptions = getNodeFieldOptions()
const modelList = ref<Array<any>>()
new TreeCommonAPI('model').listResource(ROOT_FOLDER_ID).then((ok) => {
  modelList.value = ok.data
})

// 便签子区选项来自应用「便签设置」（含自定义子区），与「AI 便签提取」节点一致
const route = useRoute()
const sectionOptions = ref<Array<{ label: string; value: string }>>([])
const applicationId = (route.params as any).id
if (applicationId) {
  ApplicationAPI.getSections(applicationId).then((res: any) => {
    sectionOptions.value = (res?.data ?? [])
      .filter((s: any) => s.enabled !== false)
      .map((s: any) => ({ label: s.label || s.sectionKey, value: s.sectionKey }))
  })
}

const formData = reactive({
  sourceSeedVariable: [] as string[],
  sourceVariable: [] as string[],
  summarySeedVariable: [] as string[],
  summaryVariable: [] as string[],
  factsSeedVariable: [] as string[],
  factsVariable: [] as string[],
  budget: 32000,
  highRatio: 0.85,
  lowRatio: 0.6,
  keepRecentItems: 10,
  stripMultimodal: true,
  enableSummarizer: false,
  summarizerModelId: '',
  summarizerMethod: 'fc',
  factSections: ['convention', 'preference', 'env', 'goal', 'todo'] as string[],
  reservedTokens: 3000,
  tokenEncoding: 'cl100k'
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
      sourceSeedVariable: data.sourceSeedVariable || [],
      sourceVariable: data.sourceVariable || [],
      summarySeedVariable: data.summarySeedVariable || [],
      summaryVariable: data.summaryVariable || [],
      factsSeedVariable: data.factsSeedVariable || [],
      factsVariable: data.factsVariable || [],
      budget: data.budget ?? 32000,
      highRatio: data.highRatio ?? 0.85,
      lowRatio: data.lowRatio ?? 0.6,
      keepRecentItems: data.keepRecentItems ?? 10,
      stripMultimodal: data.stripMultimodal ?? true,
      enableSummarizer: data.enableSummarizer ?? false,
      summarizerModelId: data.summarizerModelId || '',
      summarizerMethod: data.summarizerMethod || 'fc',
      factSections: data.factSections || ['convention', 'preference', 'env', 'goal', 'todo'],
      reservedTokens: data.reservedTokens ?? 3000,
      tokenEncoding: data.tokenEncoding || 'cl100k'
    })
  }
})
</script>

<style lang="scss" scoped></style>
