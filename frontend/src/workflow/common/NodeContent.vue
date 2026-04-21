<template>
  <slot name="content"> </slot>
  <Fieldset>
    <template #legend>
      <div class="flex items-center justify-between w-full gap-4">
        <span class="text-sm font-medium">节点输出</span>
        <div class="flex items-center gap-2">
          <span class="text-xs text-gray-500">异常捕捉</span>
          <InputSwitch v-model="errorCaptureEnabled" />
        </div>
      </div>
    </template>

    <template v-if="$slots.outputfield">
      <slot name="outputfield"></slot>
    </template>
    <template v-else>
      <TreeNode
        v-for="field in fieldList"
        :key="field.value"
        :node="field"
        :name="name"
        @copy="copy"
      />
    </template>
  </Fieldset>
</template>
<script setup lang="ts">
import { ref, onMounted, inject, provide, computed } from 'vue'
import type { LifeCycle } from '@/workflow/common/type'
import Clipboard from 'vue-clipboard3'
import TreeNode from '@/workflow/common/TreeNode.vue'
import bus from '@/bus'
const getModel = inject('getModel') as any
const model = getModel()
const errorCaptureEnabled = computed({
  get: () => {
    return model.properties.errorCaptureEnabled || false
  },
  set: (event) => {
    model.properties.errorCaptureEnabled = event
    model.refreshDegrees()
  }
})
provide('getOptions', () => {
  const getUpNode = (id: string, result: Array<any>) => {
    const upNodes = model.graphModel.getNodeIncomingNode(id)
    upNodes.forEach((node: any) => {
      result.push(node)
      getUpNode(node.id, result)
    })
    return result
  }
  const upNodes = getUpNode(model.id, [])
  return upNodes
    .map((node) => {
      return node.properties.field_list.map((item: any) => ({
        label: `@${node.properties.name}.${item.label}`,
        value: '$' + `{ ${node.properties.name}.${item.label} }`
      }))
    })
    .reduce((x: Array<any>, y: Array<any>) => [...x, ...y], [])
})

const getNodeFieldOptions = () => {
  const getUpNode = (id: string, result: Array<any>) => {
    const upNodes = model.graphModel.getNodeIncomingNode(id)
    upNodes.forEach((node: any) => {
      result.push(node)
      getUpNode(node.id, result)
    })
    return result
  }
  const upNodes = getUpNode(model.id, [])
  return upNodes.map((node) => {
    return {
      label: node.properties.name,
      value: node.id,
      disabled: true,
      children: node.properties.field_list.map((item: any) => ({
        label: item.label,
        value: item.value,
        children: item.children
      }))
    }
  })
}
const flattenVariables = (nodes: any[], parentLabel = '', parentValue = ''): any[] => {
  const result: any[] = []

  for (const node of nodes) {
    if (node.disabled) {
      // 分组节点，递归子级，传递当前 label 作为前缀
      result.push(...flattenVariables(node.children ?? [], node.label, node.label))
    } else {
      const value = parentValue ? `${parentValue}.${node.value}` : node.value
      const label = parentLabel ? `${parentLabel} / ${node.label}` : node.label
      result.push({ value, label, children: node.children })
    }
  }

  return result
}
provide('getNodeFieldOptions', getNodeFieldOptions)
provide('getTemplateVariables', () => {
  return flattenVariables(getNodeFieldOptions())
})

const props = defineProps<{
  validate: () => Promise<boolean>
  lifeCycle?: LifeCycle
}>()
const fieldList = computed(() => {
  const result = [...model.properties.field_list]
  if (errorCaptureEnabled.value) {
    result.push({
      label: '异常信息',
      value: 'exception'
    })
  }
  return result
})
const name = computed(() => {
  return model.properties.name
})
const copy = (text: string) => {
  const { toClipboard } = Clipboard()
  toClipboard(text)
    .then(() => {
      bus.emit('message:success', '复制成功')
    })
    .catch(() => {
      bus.emit('message:error', '复制失败')
    })
}

onMounted(() => {
  if (props.lifeCycle?.onMounted) {
    props.lifeCycle?.onMounted()
  }
})
</script>
<style lang="scss">
.run-drawer-header {
  margin-bottom: 0;
}
</style>
