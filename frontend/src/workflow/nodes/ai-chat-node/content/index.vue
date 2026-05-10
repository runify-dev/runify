<template>
  <div class="flex flex-col gap-4">
    <Fieldset legend="基本信息">
      <div class="flex flex-col gap-3 mt-2">
        <div>
          <label class="mb-1 block text-sm font-medium">模型</label>
          <Select
            v-model="formData.modelId"
            :options="modelList"
            optionLabel="name"
            placeholder="请选择模型"
            optionValue="id"
            class="w-full"
          />
        </div>
        <div>
          <label class="mb-1 block text-sm font-medium">系统提示词</label>
          <TemplateEditor
            v-model="formData.system"
            :variables="variables"
            title="系统提示词"
            style="height: 200px"
          />
        </div>
        <div>
          <label class="mb-1 block text-sm font-medium">
            用户提示词
            <span v-if="!formData.enableContext" class="text-red-500">*</span>
            <span v-else class="text-gray-400 text-xs">（可选）</span>
          </label>
          <TemplateEditor
            v-model="formData.user"
            :variables="variables"
            title="用户提示词"
            style="height: 200px"
          />
        </div>
      </div>
    </Fieldset>

    <Fieldset legend="自定义上下文">
      <div class="flex flex-col gap-3 mt-2">
        <div class="flex items-center gap-2">
          <ToggleSwitch v-model="formData.enableContext" />
          <label>启用自定义上下文</label>
        </div>
        <template v-if="formData.enableContext">
          <div>
            <label class="mb-1 block text-sm font-medium">上下文变量</label>
            <Cascader
              placeholder="请选择上下文变量"
              :config="{ labelKey: 'label', valueKey: 'value' }"
              :options="fieldOptions"
              v-model="formData.contextVariable"
              optionLabel="label"
              optionGroupChildren="children"
              class="w-full"
            />
          </div>
          <div>
            <label class="mb-1 block text-sm font-medium">上下文轮次</label>
            <InputNumber
              v-model="formData.contextNumber"
              placeholder="0 表示全部"
              :min="0"
              class="w-full"
            />
            <small class="text-gray-500">限制历史消息轮次，0 或留空表示全部</small>
          </div>
        </template>
      </div>
    </Fieldset>

    <Fieldset legend="附件">
      <div class="flex flex-col gap-3 mt-2">
        <div>
          <label class="mb-1 block text-sm font-medium">图片</label>
          <div class="flex flex-col gap-2">
            <div
              v-for="(img, index) in formData.images"
              :key="index"
              class="flex items-center gap-2"
            >
              <Cascader
                placeholder="请选择图片变量"
                :config="{ labelKey: 'label', valueKey: 'value' }"
                :options="fieldOptions"
                v-model="formData.images[index]"
                optionLabel="label"
                optionGroupChildren="children"
                class="flex-1"
              />
              <Button
                icon="pi pi-times"
                severity="secondary"
                size="small"
                @click="formData.images.splice(index, 1)"
              />
            </div>
            <Button
              label="添加图片"
              icon="pi pi-plus"
              size="small"
              severity="secondary"
              @click="formData.images.push([])"
            />
          </div>
        </div>
        <div>
          <label class="mb-1 block text-sm font-medium">视频</label>
          <div class="flex flex-col gap-2">
            <div
              v-for="(vid, index) in formData.videos"
              :key="index"
              class="flex items-center gap-2"
            >
              <Cascader
                placeholder="请选择视频变量"
                :config="{ labelKey: 'label', valueKey: 'value' }"
                :options="fieldOptions"
                v-model="formData.videos[index]"
                optionLabel="label"
                optionGroupChildren="children"
                class="flex-1"
              />
              <Button
                icon="pi pi-times"
                severity="secondary"
                size="small"
                @click="formData.videos.splice(index, 1)"
              />
            </div>
            <Button
              label="添加视频"
              icon="pi pi-plus"
              size="small"
              severity="secondary"
              @click="formData.videos.push([])"
            />
          </div>
        </div>
      </div>
    </Fieldset>

    <Fieldset legend="工具配置">
      <Tools v-model="toolsConfig" />
    </Fieldset>
  </div>
</template>
<script setup lang="ts">
import { ref, inject, onMounted, reactive } from 'vue'
import type { BaseNodeModel } from '@logicflow/core'
import TemplateEditor from '@/components/template-editor/index.vue'
import InputNumber from 'primevue/inputnumber'
import { TreeCommonAPI } from '@/api/tree'
import Cascader from '@/components/cascader/index.vue'
import Tools from '@/workflow/nodes/ai-chat-node/components/tools/index.vue'
const treeCommonAPI = new TreeCommonAPI('model')
const getModel = inject('getModel') as () => BaseNodeModel
const model = getModel()
const modelList = ref<Array<any>>()
const getTemplateVariables = inject('getTemplateVariables') as any
const variables = getTemplateVariables()
const getNodeFieldOptions = inject('getNodeFieldOptions') as any
const fieldOptions = getNodeFieldOptions()

import type { ToolsConfig } from '../components/tools/type'

const toolsConfig = ref<ToolsConfig>({
  location: 'customize',
  reference: [],
  tools: []
})

const defaultFormData = {
  modelId: '',
  system: '',
  user: '',
  enableContext: false,
  contextVariable: [] as string[],
  contextNumber: 0,
  images: [] as any[],
  videos: [] as any[]
}

const formData = reactive({ ...defaultFormData })

const validate = () => {
  const errors: Record<string, string> = {}
  if (!formData.modelId) {
    errors.modelId = '请选择模型'
  }
  return Promise.resolve({ values: { ...formData }, errors })
}

const submit = () => {
  return validate().then(({ values, errors }:any) => {
    if (Object.keys(errors).length == 0) {
      values.tools = JSON.parse(JSON.stringify(toolsConfig.value))
      model.properties.nodeData = values
      return Promise.resolve(values)
    }
    return Promise.resolve(errors)
  })
}

const setField = () => {
  model.properties.field_list = [
    { label: '回答结果', value: 'content' },
    { label: '思考过程', value: 'reasoningContent' },
    { label: '拒绝原因文本', value: 'refusal' },
    { label: '是否拒绝回答', value: 'isRefusal' },
    { label: '工具调用', value: 'toolCalls' },
    { label: '结束原因', value: 'finishReason' }
  ]
}

defineExpose({ validate, submit, setField })

onMounted(() => {
  treeCommonAPI.listResource('root').then((ok) => {
    modelList.value = ok.data
  })
  if (model.properties.nodeData) {
    const data = JSON.parse(JSON.stringify(model.properties.nodeData))
    Object.assign(formData, defaultFormData, data)
    if (data.tools) {
      toolsConfig.value = data.tools
    }
  } else {
    model.properties.nodeData = { ...defaultFormData }
  }
  setField()
})
</script>
<style lang="scss" scoped></style>
