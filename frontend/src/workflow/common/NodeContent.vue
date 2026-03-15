<template>
  <slot name="content"> </slot>
  <Fieldset legend="节点输出">
    <template v-if="$slots.outputfield">
      <slot name="outputfield"></slot>
    </template>
    <template v-else>
      <div
        class="flex p-4 mt-2 rounded-xs h-5 bg-gray-100 font-normal text-[14px] items-center justify-between"
        v-for="field in fieldList"
        :key="field.value"
      >
        <span>{{ `${field.label}\{\{ ${field.value} \}\}` }} </span>
        <div
          @click="copy(`{{${name}.${field.value}}}`)"
          class="w-5 h-5 hover:bg-gray-100 hover:text-gray-900 hover:cursor-pointer flex rounded-xs justify-center items-center"
        >
          <Button
            v-tooltip="'复制'"
            icon="pi pi-copy"
            variant="text"
            aria-label="Filter"
            severity="secondary"
            size="small"
          ></Button>
        </div>
      </div>
    </template>
  </Fieldset>
</template>
<script setup lang="ts">
import { ref, onMounted, inject, provide, computed } from 'vue'
import type { LifeCycle } from '@/workflow/common/type'
import Clipboard from 'vue-clipboard3'
import { ElMessage } from 'element-plus'
const getModel = inject('getModel') as any
const model = getModel()
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
      const name = parentValue ? `${parentValue}.${node.value}` : node.value
      const label = parentLabel ? `${parentLabel} / ${node.label}` : node.label
      result.push({ name, label })
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
  return model.properties.field_list
})
const name = ref<string>('')
const copy = (text: string) => {
  const { toClipboard } = Clipboard()
  toClipboard(text)
    .then(() => {
      ElMessage.success({ message: '复制成功' })
    })
    .catch(() => {
      ElMessage.error({ message: '复制失败' })
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
