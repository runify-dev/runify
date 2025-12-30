<template>
  <el-form
    ref="ruleFormRef"
    style="max-width: 600px"
    :model="instance"
    :rules="rules"
    label-width="auto"
    label-position="top"
    require-asterisk-position="right"
  >
    <el-form-item label="请求方式" prop="method">
      <el-segmented
        v-model="instance.method"
        :options="methodOptions"
        :props="{
          label: 'label',
          value: 'value'
        }"
      />
    </el-form-item>
    <el-form-item label="请求地址" prop="path">
      <el-input v-model="instance.path" placeholder="请输入请求地址" />
    </el-form-item>
    <el-form-item prop="parameters">
      <Parameters ref="parametersRef" :parameters="instance.parameters"></Parameters>
    </el-form-item>
  </el-form>
</template>
<script setup lang="ts">
import { onMounted, ref, inject } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import Parameters from './parameter/index.vue'
import processorAPI from '@/api/processor'
import type { BaseNodeModel } from '@logicflow/core'
import { nextTick } from 'process'
const parametersRef = ref<InstanceType<typeof Parameters>>()
const getModel = inject('getModel') as () => BaseNodeModel
const model = getModel()
const props = defineProps<{
  processor: any
}>()
const defaultValue = {
  method: 'GET',
  path: '',
  parameters: []
}
const instance = ref<any>({ ...defaultValue })
const methodOptions = ref<Array<any>>([
  {
    label: 'GET',
    value: 'GET'
  },
  {
    label: 'POST',
    value: 'POST'
  },
  {
    label: 'PUT',
    value: 'PUT'
  },
  {
    label: 'DELETE',
    value: 'DELETE'
  }
])
const rules = ref<FormRules<any>>({
  path: [{ required: true, message: '请输入请求地址', trigger: 'blur' }],
  method: [{ required: true, message: '请选择请求方式', trigger: 'blur' }]
})

const ruleFormRef = ref<FormInstance>()
const validate = () => {
  return ruleFormRef.value?.validate()
}
const submit = () => {
  return validate()
    ?.then(() => {
      return processorAPI.editProcessor(props.processor.projectId, props.processor.id, {
        meta: instance.value
      })
    })
    .then(() => {
      model.properties.nodeData = {
        meta: instance.value,
        protocol: props.processor.protocol
      }
    })
}
onMounted(() => {
  instance.value = { ...defaultValue, ...props.processor.meta }
  nextTick(() => {
    parametersRef.value?.updateFieldList()
  })
})
defineExpose({ validate, submit })
</script>
<style lang="scss"></style>
