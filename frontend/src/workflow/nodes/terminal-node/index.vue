<template>
  <SimpleNodeContainer ref="containerRef" :model="model" :validate="validate" :submit="submit">
    <Content ref="contentRef" :WorkflowType="WorkflowType" :details="details" />
  </SimpleNodeContainer>
</template>

<script setup lang="ts">
import SimpleNodeContainer from '@/workflow/common/SimpleNodeContainer.vue'
import type { BaseNodeModel } from '@logicflow/core'
import Content from './content/index.vue'
import { init } from './content'
import { inject, ref, onMounted } from 'vue'
import { useNodeValidator } from '@/workflow/common/useNodeValidator'

const WorkflowType = inject('WorkflowType')
const details = (inject('getDetails') as any)()
const getModel = inject('getModel') as () => BaseNodeModel
const model = getModel()

const containerRef = ref()
const contentRef = ref()

const validate = () => {
  return contentRef.value ? contentRef.value.validate() : Promise.resolve(true)
}

const submit = () => {
  return contentRef.value ? contentRef.value.submit() : Promise.resolve(true)
}

useNodeValidator(model, containerRef)

onMounted(() => {
  init({ model, workflowType: WorkflowType as string, details })
})
</script>

<style lang="scss" scoped></style>
