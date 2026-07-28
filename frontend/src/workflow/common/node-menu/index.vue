<template>
  <component :is="kw[workflowType]"></component>
</template>
<script setup lang="ts">
import applicationMenu from '@/workflow/common/node-menu/impl/application-menu/index.vue'
import processorMenu from '@/workflow/common/node-menu/impl/processor-menu/index.vue'
import applicationLoopMenu from '@/workflow/common/node-menu/impl/application-loop-menu/index.vue'
import processorLoopMenu from '@/workflow/common/node-menu/impl/processor-loop-menu/index.vue'
import { WorkflowType } from '../data'
const kw = {
  [WorkflowType.APPLICATION]: applicationMenu,
  [WorkflowType.PROCESSOR]: processorMenu,
  [WorkflowType.APPLICATION_LOOP]: applicationLoopMenu,
  [WorkflowType.PROCESSOR_LOOP]: processorLoopMenu,
  // 工具工作流暂复用 processor 节点集
  [WorkflowType.TOOL]: processorMenu,
  [WorkflowType.TOOL_LOOP]: processorLoopMenu
}
defineProps<{
  workflowType: WorkflowType
}>()
</script>
<style lang="scss" scoped></style>
