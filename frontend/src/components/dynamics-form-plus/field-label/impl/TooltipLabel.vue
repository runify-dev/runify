<template>
  <label class="inline-flex items-center gap-1">
    {{ label }}
    <span v-if="required" class="text-red-500">*</span>
    <i
      v-if="tooltip"
      v-tooltip="tooltip"
      class="pi pi-info-circle text-muted-color text-xs cursor-help"
    />
  </label>
</template>
<script setup lang="ts">
import { computed } from 'vue'
import type { FormField } from '@/components/dynamics-form-plus/type'

const props = defineProps<{
  formField: FormField
  otherParams: any
  formFieldList: Array<FormField>
  field: any
  form: any
}>()

const label = computed(() => props.formField.label?.value ?? props.formField.label ?? '')
const tooltip = computed(() => props.formField.label?.tooltip ?? '')
const required = computed(() => props.formField.required)
</script>
<style lang="scss" scoped></style>
