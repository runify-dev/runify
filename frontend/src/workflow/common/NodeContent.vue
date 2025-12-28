<template>
  <h5
    v-if="$slots.content"
    class="relative before:block before:absolute before:translate-x-[-50%] before:translate-y-[-50%] before:top-1/2 before:left-[2px] before:w-[2px] before:h-[80%] before:bg-[var(--el-color-primary)] pl-3 font-medium"
  >
    基本设置
  </h5>
  <slot name="content"> </slot>
  <h5
    class="relative before:block before:absolute before:translate-x-[-50%] before:translate-y-[-50%] before:top-1/2 before:left-[2px] before:w-[2px] before:h-[80%] before:bg-[var(--el-color-primary)] pl-3 font-medium"
  >
    节点输出
  </h5>
  <div
    class="flex p-4 rounded-xs h-[20px] bg-gray-100 font-normal text-[14px] items-center justify-between"
    v-for="field in fieldList"
    :key="field.value"
  >
    <span>{{ `${field.label}\{\{ ${field.value} \}\}` }} </span>
    <div
      @click="copy(`{{${name}.${field.value}}}`)"
      class="w-5 h-5 hover:bg-gray-100 hover:text-gray-900 hover:cursor-pointer flex rounded-xs justify-center items-center"
    >
      <app-icon name="CopyDocument"></app-icon>
    </div>
  </div>
</template>
<script setup lang="ts">
import { ref, onMounted, inject, provide, computed } from 'vue'
import type { Field, LifeCycle } from '@/workflow/common/type'
import AppIcon from '@/components/icons/AppIcon.vue'
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
provide('getNodeFieldOptions', () => {
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
      children: node.properties.field_list.map((item: any) => ({
        label: item.label,
        value: item.value
      }))
    }
  })
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
