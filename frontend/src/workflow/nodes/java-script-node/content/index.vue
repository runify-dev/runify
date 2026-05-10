<template>
  <div class="flex flex-col gap-4">
    <Fieldset legend="基本信息">
      <div class="flex flex-col gap-3">
        <div>
          <label class="text-sm font-medium">执行模式</label>
          <SelectButton
            class="mt-1"
            v-model="formData.mode"
            :options="modeOptions"
            option-label="label"
            option-value="value"
            fluid
          />
        </div>

        <div v-if="formData.mode === 'function'">
          <label class="text-sm font-medium">函数名称</label>
          <InputText type="text" class="mt-1" v-model="formData.functionName" fluid />
          <Message v-if="errors.functionName" severity="error" size="small" variant="simple">
            {{ errors.functionName }}
          </Message>
        </div>

        <div>
          <label class="text-sm font-medium">JavaScript</label>
          <SelectButton
            class="mt-1"
            v-model="formData.codeLocation"
            :options="locationOptions"
            option-label="label"
            option-value="value"
            fluid
          />
          <Cascader
            v-if="formData.codeLocation === 'reference'"
            class="mt-1 w-full"
            :config="{ labelKey: 'label', valueKey: 'value' }"
            :options="fieldOptions"
            v-model="formData.codeReference"
            placeholder="请选择引用变量"
            fluid
          />
          <CodeEditor
            v-else
            class="mt-1"
            v-model="formData.code"
            title="JavaScript"
            lang="JAVASCRIPT"
          />
          <Message v-if="errors.codeLocation" severity="error" size="small" variant="simple">
            {{ errors.codeLocation }}
          </Message>
        </div>

        <div class="flex items-center justify-between py-1">
          <div>
            <label class="text-sm font-medium">允许IO和网络访问</label>
            <p class="text-xs text-muted-color mt-0.5">开启后可读写文件和发起网络请求</p>
          </div>
          <ToggleSwitch v-model="formData.allowIO" />
        </div>
      </div>
    </Fieldset>

    <Parameters
      ref="parametersRef"
      v-model:parameters="formData.parameters"
    />
  </div>
</template>
<script setup lang="ts">
import { reactive, inject, onMounted } from 'vue'
import Parameters from '@/workflow/nodes/java-script-node/components/parameters/index.vue'
import CodeEditor from '@/components/code-editor/index.vue'
import Cascader from '@/components/cascader/index.vue'
import type { BaseNodeModel } from '@logicflow/core'
import { validate as validateNodeData } from './validator'

const getModel = inject('getModel') as () => BaseNodeModel
const getNodeFieldOptions = inject('getNodeFieldOptions') as () => any[]
const model = getModel()
const fieldOptions = getNodeFieldOptions()

const modeOptions = [
  { label: '脚本', value: 'script' },
  { label: '函数', value: 'function' }
]

const locationOptions = [
  { label: '引用', value: 'reference' },
  { label: '自定义', value: 'customize' }
]

const formData = reactive({
  mode: 'script' as 'script' | 'function',
  codeLocation: 'customize' as 'reference' | 'customize',
  codeReference: [] as string[],
  functionName: '',
  code: '',
  allowIO: false,
  parameters: [] as any[]
})

const errors = reactive<Record<string, string>>({})

const validate = () => {
  const result = validateNodeData(formData)
  Object.keys(errors).forEach((k) => delete errors[k])
  if (!result.valid && result.errors) {
    Object.assign(errors, result.errors)
  }
  if (result.valid) {
    return Promise.resolve({ values: { ...formData }, errors: {} })
  }
  return Promise.resolve({ values: {}, errors: result.errors ?? {} })
}

const submit = () => {
  return validate().then(({ values, errors: errs }) => {
    if (Object.keys(errs).length === 0) {
      model.properties.nodeData = { ...formData }
      return Promise.resolve(values)
    }
    return Promise.resolve(errs)
  })
}

const setField = () => {
  model.properties.field_list = [{ label: '结果', value: 'result' }]
}

defineExpose({ validate, submit, setField })

onMounted(() => {
  if (model.properties.nodeData) {
    const data = model.properties.nodeData
    formData.mode = data.mode || 'script'
    formData.codeLocation = data.codeLocation || 'customize'
    formData.codeReference = data.codeReference ? JSON.parse(JSON.stringify(data.codeReference)) : []
    formData.functionName = data.functionName || ''
    formData.code = data.code || ''
    formData.allowIO = data.allowIO ?? false
    formData.parameters = data.parameters ? JSON.parse(JSON.stringify(data.parameters)) : []
  } else {
    model.properties.nodeData = {
      mode: 'script',
      codeLocation: 'customize',
      codeReference: [],
      functionName: '',
      code: '',
      allowIO: false,
      parameters: []
    }
  }
  setField()
})
</script>
<style lang="scss" scoped></style>
