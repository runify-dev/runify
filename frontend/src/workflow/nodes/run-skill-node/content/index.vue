<template>
  <div>
    <Fieldset legend="基本信息">
      <div class="mb-3">
        <div class="flex items-center justify-between mb-2">
          <label>运行环境</label>
          <SelectButton
            v-model="formData.runtime"
            :options="runtimeOptions"
            option-label="label"
            option-value="value"
            size="small"
          />
        </div>
      </div>

      <div class="mb-3">
        <div class="flex items-center justify-between mb-2">
          <label>模式</label>
          <SelectButton
            v-model="formData.location"
            :options="locationOptions"
            option-label="label"
            option-value="value"
            size="small"
          />
        </div>
      </div>

      <!-- tool_call 模式 -->
      <template v-if="formData.location === 'tool_call'">
        <div class="mb-3">
          <label class="mb-2 block">引用变量</label>
          <Cascader
            placeholder="请选择 tool_call 变量"
            :config="{ labelKey: 'label', valueKey: 'value' }"
            :options="fieldOptions"
            v-model="formData.reference"
            optionLabel="label"
            optionGroupChildren="children"
            class="w-full"
          />
          <Message v-if="errors.reference" severity="error" size="small" variant="simple">
            {{ errors.reference }}
          </Message>
        </div>
      </template>

      <!-- 自定义模式 -->
      <template v-else>
        <!-- 技能ID -->
        <div class="mb-3">
          <div class="flex items-center justify-between mb-2">
            <label>技能</label>
            <SelectButton
              v-model="formData.skillIdLocation"
              :options="fieldLocationOptions"
              option-label="label"
              option-value="value"
              size="small"
            />
          </div>
          <Cascader
            v-if="formData.skillIdLocation === 'reference'"
            placeholder="请选择技能变量"
            :config="{ labelKey: 'label', valueKey: 'value' }"
            :options="fieldOptions"
            v-model="formData.skillIdReference"
            optionLabel="label"
            optionGroupChildren="children"
            class="w-full"
          />
          <Message v-if="formData.skillIdLocation === 'reference' && errors.skillIdReference" severity="error" size="small" variant="simple">
            {{ errors.skillIdReference }}
          </Message>
          <InputText
            v-if="formData.skillIdLocation === 'customize'"
            v-model="formData.skillId"
            placeholder="技能名称，如 apply_patch"
            class="w-full"
          />
          <Message v-if="formData.skillIdLocation === 'customize' && errors.skillId" severity="error" size="small" variant="simple">
            {{ errors.skillId }}
          </Message>
        </div>

        <!-- 执行命令 -->
        <div class="mb-3">
          <div class="flex items-center justify-between mb-2">
            <label>执行命令（可选，留空则使用默认脚本）</label>
            <SelectButton
              v-model="formData.commandLocation"
              :options="fieldLocationOptions"
              option-label="label"
              option-value="value"
              size="small"
            />
          </div>
          <Cascader
            v-if="formData.commandLocation === 'reference'"
            placeholder="请选择脚本变量"
            :config="{ labelKey: 'label', valueKey: 'value' }"
            :options="fieldOptions"
            v-model="formData.commandReference"
            optionLabel="label"
            optionGroupChildren="children"
            class="w-full"
          />
          <InputText
            v-if="formData.commandLocation === 'customize'"
            v-model="formData.command"
            placeholder="如 python main.py --input data.csv"
            class="w-full"
          />
        </div>
      </template>
    </Fieldset>
  </div>
</template>

<script setup lang="ts">
import { inject, onMounted, reactive } from 'vue'
import Cascader from '@/components/cascader/index.vue'
import type { BaseNodeModel } from '@logicflow/core'
import { locationOptions, fieldLocationOptions, runtimeOptions } from './type'
import { cloneDeep } from 'lodash'

const getModel = inject('getModel') as () => BaseNodeModel
const model = getModel()
const getNodeFieldOptions = inject('getNodeFieldOptions') as any
const fieldOptions = getNodeFieldOptions()

const formData = reactive({
  runtime: 'local' as 'local' | 'docker',
  location: 'customize' as 'tool_call' | 'customize',
  reference: [] as string[],
  skillIdLocation: 'customize' as 'reference' | 'customize',
  skillIdReference: [] as string[],
  skillId: '',
  commandLocation: 'customize' as 'reference' | 'customize',
  commandReference: [] as string[],
  command: ''
})

const errors = reactive<Record<string, string>>({})

function validate() {
  Object.keys(errors).forEach((k) => delete errors[k])

  if (formData.location === 'tool_call') {
    if (!formData.reference || formData.reference.length === 0) {
      errors.reference = '请选择引用变量'
    }
  } else {
    if (formData.skillIdLocation === 'reference') {
      if (!formData.skillIdReference || formData.skillIdReference.length === 0) {
        errors.skillIdReference = '请选择技能变量'
      }
    } else {
      if (!formData.skillId || formData.skillId.trim() === '') {
        errors.skillId = '请输入技能名称'
      }
    }
  }

  const valid = Object.keys(errors).length === 0
  return Promise.resolve({ values: cloneDeep({ ...formData }), errors: valid ? {} : errors })
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
      runtime: data.runtime || 'local',
      location: data.location || 'customize',
      reference: data.reference || [],
      skillIdLocation: data.skillIdLocation || 'customize',
      skillIdReference: data.skillIdReference || [],
      skillId: data.skillId || '',
      commandLocation: data.commandLocation || 'customize',
      commandReference: data.commandReference || [],
      command: data.command || ''
    })
  } else {
    model.properties.nodeData = {
      location: 'customize',
      reference: [],
      skillIdLocation: 'customize',
      skillIdReference: [],
      skillId: '',
      commandLocation: 'customize',
      commandReference: [],
      command: ''
    }
  }
  model.properties.field_list = [
    {
        label: '工具执行',
        value: 'tool'
    },
    { label: '执行结果', value: 'result' },
    { label: '标准输出', value: 'stdout' },
    { label: '错误输出', value: 'stderr' },
    { label: '退出码', value: 'exitCode' }
  ]
})
</script>

<style lang="scss" scoped></style>
