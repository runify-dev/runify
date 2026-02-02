<template>
  <Drawer
    v-model:visible="drawer"
    header="模型参数"
    position="right"
    :pt="{
      root: {
        style: {
          '--drawer-width-mobile': '80%',
          '--drawer-width-tablet': '50%',
          '--drawer-width-desktop': '500px'
        },
        class: 'responsive-drawer'
      }
    }"
  >
    <DynamicsFormConstructor ref="DynamicsFormConstructorRef"></DynamicsFormConstructor>
    <template #footer>
      <Button @click="close">{{ $t('common.cancel') }}</Button>
      <Button type="primary" @click="submit()">{{
        edit ? $t('common.save') : $t('common.add')
      }}</Button>
    </template>
  </Drawer>
</template>
<script setup lang="ts">
import { nextTick, ref } from 'vue'
import DynamicsFormConstructor from '@/components/dynamics-form-plus/constructor/index.vue'
import type { style } from '@logicflow/extension/lib/bpmn-elements/presets/icons'
const props = defineProps<{
  addParams: (data: any, index?: number) => boolean
}>()
const drawer = ref<boolean>()
const DynamicsFormConstructorRef = ref<InstanceType<typeof DynamicsFormConstructor>>()
const currentIndex = ref<number>()
const close = () => {
  drawer.value = false
}
const edit = ref<boolean>(false)
const open = (data?: any, index?: number) => {
  drawer.value = true
  if (data) {
    nextTick(() => {
      DynamicsFormConstructorRef.value?.rander(data)
    })
    edit.value = true
    currentIndex.value = index
  }
}

const submit = () => {
  if (DynamicsFormConstructorRef.value) {
    DynamicsFormConstructorRef.value?.validate().then(({ errors }) => {
      if (Object.keys(errors).length === 0) {
        const ok = props.addParams(DynamicsFormConstructorRef.value?.getData(), currentIndex.value)
        if (ok) {
          drawer.value = false
          edit.value = false
          currentIndex.value = undefined
        }
      }
    })
  }
}
defineExpose({ open, close })
</script>
<style lang="scss">
.responsive-drawer {
  width: var(--drawer-width-mobile) !important;
}

@media (min-width: 768px) {
  .responsive-drawer {
    width: var(--drawer-width-tablet) !important;
  }
}

@media (min-width: 1024px) {
  .responsive-drawer {
    width: var(--drawer-width-desktop) !important;
  }
}
</style>
