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
import { ref, onMounted } from 'vue'
import type { BaseNodeModel } from '@logicflow/core'
import type { Field, LifeCycle } from '@/workflow/common/type'
import AppIcon from '@/components/icons/AppIcon.vue'
import Clipboard from 'vue-clipboard3'
import { ElMessage } from 'element-plus'
const props = defineProps<{
  validate: () => Promise<boolean>
  lifeCycle?: LifeCycle
}>()
const drawer = ref<boolean>(false)
const fieldList = ref<Array<Field>>([])
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

const close = () => {
  drawer.value = false
}

const open = (model: BaseNodeModel) => {
  name.value = model.properties.name
  fieldList.value = model.properties.field_list
  drawer.value = true
  return Promise.resolve('ok')
}
onMounted(() => {
  if (props.lifeCycle?.onMounted) {
    props.lifeCycle?.onMounted()
  }
})
defineExpose({ open, close })
</script>
<style lang="scss">
.run-drawer-header {
  margin-bottom: 0;
}
</style>
