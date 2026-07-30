<template>
  <Drawer
    v-model:visible="drawer"
    :header="name"
    direction="rtl"
    append-to-body
    :modal="false"
    :dismissable="false"
    class="node-setting-drawer"
    :style="{ width: '30rem'  }"
    :pt="{ root: { class: 'max-sm:!w-full' } }"
  >
    <template #default>
      <NodeContent v-if="drawer" :validate="validate">
        <template v-if="$slots.content" #content>
          <slot name="content"> </slot>
        </template>
      </NodeContent>
    </template>
    <template #footer>
      <div class="flex justify-end gap-2">
        <Button label="取消" severity="secondary" @click="close" />
        <Button label="确认" @click="confirm" />
      </div>
    </template>
  </Drawer>
</template>
<script setup lang="ts">
import { ref, inject } from 'vue'
import Drawer from 'primevue/drawer'
import type { BaseNodeModel } from '@logicflow/core'
import NodeContent from '@/workflow/common/NodeContent.vue'
import { collectErrorMessages } from '@/workflow/common/validator-utils'
import bus from '@/bus'
const getModel = inject('getModel') as () => BaseNodeModel
const model = getModel()
const props = defineProps<{
  validate: () => Promise<any>
  submit: () => Promise<any>
}>()
const drawer = ref<boolean>(false)
const name = ref<string>('')

// 取第一条报错信息(各校验器的报错文案本身已包含字段名,如「请输入data」,故不再拼接内部字段路径)
const firstError = (errors: any): string =>
  collectErrorMessages(errors)[0] ?? '节点配置校验未通过，请检查必填项'

const confirm = () => {
  props
    .validate()
    .then(({ errors }) => {
      if (Object.keys(errors).length == 0) {
        props
          .submit()
          .then(() => {
            model.graphModel.eventCenter.emit('runify:node:data-saved', model.id)
            close()
          })
          .catch((err) => bus.emit('message:error', firstError(err)))
      } else {
        // 校验不过给出提示,避免「点确认没反应」
        bus.emit('message:error', firstError(errors))
      }
    })
    .catch((err) => bus.emit('message:error', firstError(err)))
}
const close = () => {
  drawer.value = false
}

const open = (model: BaseNodeModel) => {
  name.value = model.properties.name
  drawer.value = true
}
defineExpose({ open, close })
</script>
<style lang="scss">
.run-drawer-header {
  margin-bottom: 0;
}
.lf-node-selected {
  .node-container {
    outline-color: var(--p-primary-color);
  }
}
.node-setting-drawer.p-drawer {
  .p-drawer-content {
    overflow-x: hidden !important;
    overflow-y: auto;
  }
}
</style>
