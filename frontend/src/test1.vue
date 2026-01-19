<template>
  <AppMenuContent>
    <template #menu> </template>
    <DynamicsFormPlus ref="dynamicsFormPlusRef" class="w-10/12"></DynamicsFormPlus>
  </AppMenuContent>
</template>
<script setup lang="ts">
import AppMenuContent from '@/layout-plus/app-menu-content/index.vue'
import { onMounted, ref } from 'vue'
import DynamicsFormPlus from '@/components/dynamics-form-plus/index.vue'
const dynamicsFormPlusRef = ref<InstanceType<typeof DynamicsFormPlus>>()
onMounted(() => {
  dynamicsFormPlusRef.value?.render([
    {
      field: 'name',
      type: 'TextInput',
      attrs: {},
      required: true,

      label: {
        type: 'BaseLabel',
        label: '用户名'
      },
      defaultValue: 'ss'
    },
    {
      field: 'password',
      type: 'PasswordInput',
      attrs: {},
      required: true,
      label: {
        type: 'BaseLabel',
        label: '密码'
      },
      defaultValue: 'ss'
    }
  ])
})
</script>
<style lang="scss" scoped></style>
