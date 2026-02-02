<template>
  <component :is="kw[workflowType]"></component>
</template>
<script setup lang="ts">
import applicationMenu from '@/workflow/common/node-menu/impl/application-menu/index.vue'
import processorMenu from '@/workflow/common/node-menu/impl/processor-menu/index.vue'
import { WorkflowType } from '../data'
const kw = {
  [WorkflowType.APPLICATION]: applicationMenu,
  [WorkflowType.PROCESSOR]: processorMenu
}
defineProps<{
  workflowType: WorkflowType
}>()
</script>
<style lang="scss" scoped></style>
