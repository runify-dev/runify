import { inject, onMounted, onBeforeUnmount, type Ref } from 'vue'
import type { BaseNodeModel } from '@logicflow/core'
import type SimpleNodeContainer from './SimpleNodeContainer.vue'

export function useNodeValidator(
  model: BaseNodeModel,
  containerRef: Ref<InstanceType<typeof SimpleNodeContainer> | undefined>
) {
  const onOpenSettings = (nodeId: string) => {
    if (nodeId === model.id) {
      containerRef.value?.openContent()
    }
  }

  onMounted(() => {
    model.graphModel.eventCenter.on('runify:node:open-settings', onOpenSettings)
  })

  onBeforeUnmount(() => {
    model.graphModel.eventCenter.off('runify:node:open-settings', onOpenSettings)
  })
}
