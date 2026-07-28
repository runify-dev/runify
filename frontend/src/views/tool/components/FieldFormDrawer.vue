<template>
  <Drawer
    v-model:visible="drawer"
    :header="header"
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
    <DynamicsFormConstructor ref="constructorRef"></DynamicsFormConstructor>
    <template #footer>
      <Button text @click="close">{{ t('common.cancel') }}</Button>
      <Button @click="submit">{{ edit ? t('common.save') : t('common.add') }}</Button>
    </template>
  </Drawer>
</template>

<script setup lang="ts">
import { nextTick, ref } from 'vue'
import DynamicsFormConstructor from '@/components/dynamics-form-plus/constructor/index.vue'
import { t } from '@/locales'

const props = defineProps<{ header: string; addParams: (data: any, index?: number) => boolean }>()

const drawer = ref(false)
const edit = ref(false)
const currentIndex = ref<number>()
const constructorRef = ref<InstanceType<typeof DynamicsFormConstructor>>()

const close = () => {
  drawer.value = false
  edit.value = false
  currentIndex.value = undefined
}

const open = (data?: any, index?: number) => {
  drawer.value = true
  edit.value = !!data
  currentIndex.value = index
  if (data) {
    nextTick(() => constructorRef.value?.rander(data))
  }
}

const submit = () => {
  constructorRef.value?.validate().then(({ errors }: any) => {
    if (Object.keys(errors).length === 0) {
      const ok = props.addParams(constructorRef.value?.getData(), currentIndex.value)
      if (ok) close()
    }
  })
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
