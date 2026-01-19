<template>
  <Password
    type="text"
    placeholder="Password"
    autocomplete="off"
    :name="field"
    :feedback="false"
    toggleMask
    fluid
  />
</template>
<script setup lang="ts">
import { computed } from 'vue'
import Password from 'primevue/password'
import type { FormField } from '@/components/dynamics-form/type'
const props = defineProps<{
  // 表单Item
  formField: FormField
  // 其他参数
  otherParams: any
  // 所有字段
  formFieldList: Array<FormField>
  // FormField v-slot="$field"
  field: any
  // Form v-slot="$form"
  form: any
}>()
const field = computed(() => {
  return props.formField.field
})
</script>
<style lang="scss"></style>
