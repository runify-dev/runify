<template>
  <Drawer
    v-model:visible="drawer"
    :header="name"
    direction="rtl"
    append-to-body
    :style="{ width: '25rem' }"
  >
    <template #default>
      <NodeContent v-if="drawer" :validate="validate" :life-cycle="lifeCycle">
        <template v-if="$slots.content" #content>
          <slot name="content"> </slot>
        </template>
      </NodeContent>
    </template>
    <template #footer>
      <div style="flex: auto">
        <Button @click="close">取消</Button>
        <Button @click="confirm">确认</Button>
      </div>
    </template>
  </Drawer>
</template>
<script setup lang="ts">
import { ref } from 'vue'
import Drawer from 'primevue/drawer'
import type { BaseNodeModel } from '@logicflow/core'
import type { LifeCycle } from '@/workflow/common/type'
import NodeContent from '@/workflow/common/NodeContent.vue'
const props = defineProps<{
  validate: () => Promise<any>
  submit: () => Promise<any>
  lifeCycle?: LifeCycle
}>()
const drawer = ref<boolean>(false)
const name = ref<string>('')

const confirm = () => {
  props.validate().then(() => {
    props.submit().then(() => {
      close()
    })
  })
}
const close = () => {
  drawer.value = false
}

const open = (model: BaseNodeModel) => {
  name.value = model.properties.name
  drawer.value = true
  console.log('ss')
}
defineExpose({ open, close })
</script>
<style lang="scss">
.run-drawer-header {
  margin-bottom: 0;
}
.lf-node-selected {
  .node-container {
    outline-color: var(--el-color-primary);
  }
}
</style>
