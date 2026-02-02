<template>
  <Dialog v-model:visible="visible" modal :style="{ width: '50rem' }">
    <Menu :workflow-type="WorkflowType.APPLICATION" @selected="selected"></Menu>
    <template #footer>
      <Button type="button" label="取消" severity="secondary" @click="close"></Button>
      <Button type="button" label="确定" @click="formRef?.submit"></Button
    ></template>
  </Dialog>
</template>
<script setup lang="ts">
import { ref } from 'vue'
import { type FormInstance } from '@primevue/forms'
import Menu from '../node-menu/index.vue'
import { WorkflowType } from '../data'
const visible = ref<boolean>(false)
const formRef = ref<FormInstance>()

const setting = ref<any>()
const selected = (node: any) => {
  setting.value.call(node, setting.value.anchorData).then(() => {
    close()
  })
}
const open = (_setting: any) => {
  visible.value = true
  setting.value = _setting
}
const close = () => {
  visible.value = false
  setting.value = undefined
}

defineExpose({
  open,
  close
})
</script>
<style lang="scss"></style>
