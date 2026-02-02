<template>
  <label for="username"> {{ label }} <span v-if="required" class="text-red-500">*</span> </label>
</template>
<script setup lang="ts">
import type { FormField } from '@/components/dynamics-form/type'
import { computed } from 'vue'
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
const label = computed(() => {
  return props.formField.label.value
})
const required = computed(() => {
  return props.formField.required
})
</script>
<style lang="scss"></style>
