<template>
  <el-drawer header-class="run-drawer-header" v-model="drawer" direction="rtl" append-to-body>
    <template #header>
      <h4 class="font-medium">{{ name }}</h4>
    </template>
    <template #default>
      <NodeContent :validate="validate" :life-cycle="lifeCycle">
        <template v-if="$slots.content" #content>
          <slot name="content"> </slot>
        </template>
      </NodeContent>
    </template>
    <template #footer>
      <div style="flex: auto">
        <el-button @click="close">取消</el-button>
        <el-button type="primary" @click="confirm">确认</el-button>
      </div>
    </template>
  </el-drawer>
</template>
<script setup lang="ts">
import { ref } from 'vue'
import type { BaseNodeModel } from '@logicflow/core'
import type { LifeCycle } from '@/workflow/common/type'
import NodeContent from '@/workflow/common/NodeContent.vue'
const props = defineProps<{
  validate: () => Promise<boolean>
  lifeCycle?: LifeCycle
}>()
const drawer = ref<boolean>(false)
const name = ref<string>('')

const confirm = () => {
  props.validate().then(() => {
    close()
  })
}
const close = () => {
  drawer.value = false
}

const open = (model: BaseNodeModel) => {
  name.value = model.properties.name
  drawer.value = true
  return Promise.resolve('ok')
}
defineExpose({ open, close })
</script>
<style lang="scss">
.run-drawer-header {
  margin-bottom: 0;
}
</style>
