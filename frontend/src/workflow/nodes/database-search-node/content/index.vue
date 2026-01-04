<template>
  <el-form
    ref="formRef"
    label-position="top"
    :model="form"
    label-width="auto"
    style="max-width: 600px"
    require-asterisk-position="right"
  >
    <el-form-item
      :rules="[{ required: true, message: 'SQL', trigger: 'blur' }]"
      label="内容(SQL)"
      prop="template"
    >
      <el-cascader
        placeholder="请选择参数"
        :options="options"
        v-model="form.pool"
        :show-all-levels="false"
        class="w-full"
      />
    </el-form-item>
    <el-form-item
      :rules="[{ required: true, message: 'SQL', trigger: 'blur' }]"
      label="内容(SQL)"
      prop="template"
    >
      <CodeEditor v-model="form.template" title="SQL" lang="SQL"></CodeEditor>
    </el-form-item>
    <el-form-item label="" prop="parameters">
      <Parameters ref="parametersRef" v-model:parameters="form.parameters"></Parameters>
    </el-form-item>
  </el-form>
</template>
<script setup lang="ts">
import { ref, inject, onMounted } from 'vue'
import Parameters from '@/workflow/nodes/database-search-node/components/parameters/index.vue'
import CodeEditor from '@/components/code-editor/index.vue'
import type { BaseNodeModel } from '@logicflow/core'
import type { FormInstance } from 'element-plus'
const getModel = inject('getModel') as () => BaseNodeModel
const formRef = ref<FormInstance>()
const model = getModel()
const getNodeFieldOptions = inject('getNodeFieldOptions') as any
const options = getNodeFieldOptions()
const form = ref<any>({
  pool: [],
  template: '',
  parameters: []
})

const validate = () => {
  return formRef.value ? formRef.value.validate() : Promise.resolve(false)
}
const submit = () => {
  model.properties.nodeData = form.value
  return Promise.resolve(true)
}
const setField = () => {
  model.properties.field_list = [
    {
      label: '结果',
      value: 'result'
    }
  ]
}
defineExpose({ validate, submit, setField })
onMounted(() => {
  if (model.properties.nodeData) {
    form.value = JSON.parse(JSON.stringify(model.properties.nodeData))
  } else {
    model.properties.nodeData = form.value
  }
  setField()
})
</script>
<style lang="scss" scoped></style>
