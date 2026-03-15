<template>
  <SimpleNodeContainer :model="model" :submit="submit" :validate="validate">
    <Content ref="contentRef" :WorkflowType="WorkflowType" :details="details"></Content>
  </SimpleNodeContainer>
</template>
<script setup lang="ts">
import MdInput from '@/components/md/MDInput.vue'
import SimpleNodeContainer from '@/workflow/common/SimpleNodeContainer.vue'
import type { BaseNodeModel } from '@logicflow/core'
import type { FormInstance } from 'element-plus'
import { TreeCommonAPI } from '@/api/tree'
import Content from './content/index.vue'
const WorkflowType = inject('WorkflowType')
const details = (inject('getDetails') as any)()
import { inject, onMounted, ref } from 'vue'
const getModel = inject('getModel') as () => BaseNodeModel
const model = getModel()

const contentRef = ref()
const validate = () => {
  return contentRef.value ? contentRef.value.validate() : Promise.resolve(true)
}
const submit = () => {
  return contentRef.value ? contentRef.value.submit() : Promise.resolve(true)
}
</script>
<style lang="scss" scoped>
:deep(.el-form-item__content) {
  width: 100%;
}
</style>
