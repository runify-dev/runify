<template>
  <label>
    {{ label }}
    <span v-if="required" class="text-red-500">*</span>
  </label>
</template>
<script setup lang="ts">
import type { FormField } from '@/components/dynamics-form-plus/type'
import { computed } from 'vue'
const props = defineProps<{
  formField: FormField
  formFieldList: Array<FormField>
  formValue: Record<string, any>
}>()
const label = computed(() => props.formField.label?.value ?? props.formField.label ?? '')
const required = computed(() => props.formField.required)
</script>
<style lang="scss" scoped></style>
