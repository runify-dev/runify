<template>
  <div></div>
</template>
<script setup lang="ts">
import { onMounted, inject } from 'vue'
import type { BaseNodeModel } from '@logicflow/core'
const getModel = inject('getModel') as () => BaseNodeModel
const model = getModel()
onMounted(() => {
  model.properties.field_list = [
    {
      label: '问题',
      value: 'question'
    }
  ]
})
const validate = () => {
  return Promise.resolve({ errors: {}, values: {} })
}
const submit = () => {
  return Promise.resolve({ errors: {}, values: {} })
}
defineExpose({
  validate: validate,
  submit: submit
})
</script>
<style lang="scss" scoped></style>
