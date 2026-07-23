<template>
  <div class="processor-canvas h-full w-full">
    <Workflow ref="workflowRef" />
  </div>
</template>

<script setup lang="ts">
import { ref, provide } from 'vue'
import Workflow from '@/workflow/index.vue'
import { WorkflowType } from '@/workflow/common/data'

/**
 * 单张画布卡片的画布实例：per-card 注入（getDetails 指向本卡片的处理器实体），
 * 并把 WorkflowHost 需要的方法代理出去（供子 agent 驱动）。
 */
const props = defineProps<{
  /** 本卡片对应的处理器实体（子 agent 拉取后回填），画布内节点设置面板经 getDetails 读取 */
  getProcessor: () => any
}>()

provide('WorkflowType', WorkflowType.PROCESSOR)
provide('getDetails', () => props.getProcessor())
// 本页自带 agent，隐藏画布内置的 AI 生成面板
provide('hideAiGenerate', true)

const workflowRef = ref<InstanceType<typeof Workflow>>()

defineExpose({
  getLf: () => workflowRef.value?.getLf(),
  render: (data?: any) => workflowRef.value?.render(data),
  getGraphData: () => workflowRef.value?.getGraphData(),
  autoLayout: () => workflowRef.value?.autoLayout(),
  validateWorkflow: (options?: { silent?: boolean; skipNodeIds?: string[] }) =>
    workflowRef.value?.validateWorkflow(options) ?? Promise.resolve({ valid: true })
})
</script>

<style lang="scss" scoped>
// 画布组件根节点带视口高度类（layout-content-height），卡片内以容器为准
.processor-canvas :deep(.layout-content-height) {
  height: 100%;
}
</style>
