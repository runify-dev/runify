<template>
  <MenuContent
    v-if="visible"
    :append-node="(node) => appendNode(node, anchorDataValue)"
    :workflow-type="workflowType"
    :close="close"
  >
  </MenuContent>
</template>
<script setup lang="ts">
import { ref } from 'vue'
import MenuContent from './MenuContent.vue'
import type { WorkflowType } from './data'
defineProps<{
  appendNode: (node: any, anchorData: any) => void
  workflowType: WorkflowType
}>()
const anchorDataValue = ref()
const visible = ref<boolean>()
const open = (anchorData: any) => {
  visible.value = !visible.value
  anchorDataValue.value = anchorData
}
const close = () => {
  visible.value = false
}

defineExpose({ open, close })
</script>
<style lang="scss" scoped></style>
