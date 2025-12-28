<template>
  <SimpleNodeContainer :model="model" :validate="validate" :submit="submit">
    <el-form
      ref="formRef"
      label-position="top"
      :model="form"
      label-width="auto"
      style="max-width: 600px"
      require-asterisk-position="right"
    >
      <el-form-item
        :rules="[{ required: true, message: '函数名称', trigger: 'blur' }]"
        label="函数名称"
        prop="functionName"
      >
        <el-input v-model="form.functionName" />
      </el-form-item>
      <el-form-item label="" prop="parameters">
        <Parameters ref="parametersRef" v-model:parameters="form.parameters"></Parameters>
      </el-form-item>
      <el-form-item
        :rules="[{ required: true, message: '工具内容不能为空', trigger: 'blur' }]"
        label="内容(javascript)"
        prop="code"
      >
        <CodeEditor v-model="form.code" title="工具"></CodeEditor>
      </el-form-item>
    </el-form>
  </SimpleNodeContainer>
</template>
<script setup lang="ts">
import SimpleNodeContainer from '@/workflow/common/SimpleNodeContainer.vue'
import type { BaseNodeModel } from '@logicflow/core'
import { inject, ref, onMounted } from 'vue'
import CodeEditor from '@/components/code-editor/index.vue'
import Parameters from '@/workflow/nodes/tool-node/components/parameters/index.vue'
const getModel = inject('getModel') as () => BaseNodeModel
const formRef = ref()
const model = getModel()
const form = ref({
  code: '',
  functionName: '',
  parameters: []
})
const parametersRef = ref()
const validate = () => {
  return Promise.all([formRef.value.validate(), parametersRef.value.validate()])
}
const submit = () => {
  model.properties.nodeData = form.value
  return Promise.resolve(true)
}
onMounted(() => {
  if (model.properties.nodeData) {
    form.value = model.properties.nodeData
  } else {
    model.properties.nodeData = form.value
  }
})
</script>
<style lang="scss" scoped></style>
