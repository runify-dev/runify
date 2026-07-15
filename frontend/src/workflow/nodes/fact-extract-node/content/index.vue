<template>
  <div>
    <Fieldset legend="源消息">
      <div class="mb-3">
        <label>要从中提取便签的消息变量（如「开始节点」的上下文、或循环变量）</label>
        <Cascader
          placeholder="请选择源消息变量"
          :config="{ labelKey: 'label', valueKey: 'value' }"
          :options="fieldOptions"
          v-model="formData.sourceReference"
          optionLabel="label"
          optionGroupChildren="children"
          class="w-full mt-2"
        />
        <Message v-if="errors.sourceReference" severity="error" size="small" variant="simple">
          {{ errors.sourceReference }}
        </Message>
      </div>
    </Fieldset>

    <Fieldset legend="便签写回">
      <div class="mb-3">
        <label>便签写回变量（选填）：不填则便签仅输出到本节点；需跨迭代累积时，指向跨迭代持久的父层变量</label>
        <Cascader
          placeholder="请选择便签变量"
          :config="{ labelKey: 'label', valueKey: 'value' }"
          :options="fieldOptions"
          v-model="formData.factsVariable"
          optionLabel="label"
          optionGroupChildren="children"
          class="w-full mt-2"
        />
        <Message v-if="errors.factsVariable" severity="error" size="small" variant="simple">
          {{ errors.factsVariable }}
        </Message>
      </div>
    </Fieldset>

    <Fieldset legend="提取">
      <div class="mb-3">
        <label>提取模型</label>
        <Select
          v-model="formData.modelId"
          :options="modelList"
          optionLabel="name"
          optionValue="id"
          placeholder="请选择模型"
          class="w-full mt-2"
        />
        <Message v-if="errors.modelId" severity="error" size="small" variant="simple">
          {{ errors.modelId }}
        </Message>
      </div>
      <div class="mb-3">
        <label>提取方法</label>
        <Select
          v-model="formData.method"
          :options="methodOptions"
          optionLabel="label"
          option-value="value"
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
          placeholder="不选则提取全部启用子区"
          class="w-full mt-2"
        />
        <div class="text-xs text-surface-400 mt-1">
          选项来自应用「便签设置」（含自定义子区）；留空表示提取全部启用的子区。
        </div>
      </div>
    </Fieldset>
  </div>
</template>

<script setup lang="ts">
import { inject, onMounted, reactive } from 'vue'
import { ref } from 'vue'
// 显式 import PrimeVue 的 MultiSelect：项目按文件名自动注册 src/components/**，
// dynamics-form-plus 里的 MultiSelect.vue 会全局遮蔽 PrimeVue 的同名组件（渲染成选项编辑器），
// 本地 import 优先级更高，覆盖回正确的多选框。
import MultiSelect from 'primevue/multiselect'
import Cascader from '@/components/cascader/index.vue'
import type { BaseNodeModel } from '@logicflow/core'
import { methodOptions } from './type'
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

// 便签子区选项来自应用「便签设置」
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
  sourceReference: [] as string[],
  factsVariable: [] as string[],
  modelId: '',
  method: 'fc',
  factSections: [] as string[]
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
      sourceReference: data.sourceReference || [],
      factsVariable: data.factsVariable || [],
      modelId: data.modelId || '',
      method: data.method || 'fc',
      factSections: data.factSections || []
    })
  }
})
</script>

<style lang="scss" scoped></style>
