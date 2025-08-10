<template>
  <el-drawer
    destroy-on-close
    v-model="drawer"
    title="模型参数"
    direction="rtl"
    :before-close="close"
  >
    <DynamicsFormConstructor ref="DynamicsFormConstructorRef"></DynamicsFormConstructor>
    <template #footer>
      <div style="flex: auto">
        <el-button @click="close">{{ $t('common.cancel') }}</el-button>
        <el-button type="primary" @click="submit()">{{
          edit ? $t('common.save') : $t('common.add')
        }}</el-button>
      </div>
    </template>
  </el-drawer>
</template>
<script setup lang="ts">
import { nextTick, ref } from 'vue'
import DynamicsFormConstructor from '@/components/dynamics-form/constructor/index.vue'
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
  DynamicsFormConstructorRef.value?.validate().then((valid) => {
    if (valid) {
      const ok = props.addParams(DynamicsFormConstructorRef.value?.getData(), currentIndex.value)
      if (ok) {
        drawer.value = false
        edit.value = false
        currentIndex.value = undefined
      }
    }
  })
}
defineExpose({ open, close })
</script>
<style lang="scss"></style>
