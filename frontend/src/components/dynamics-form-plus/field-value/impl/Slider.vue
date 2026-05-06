<template>
  <div class="flex items-center gap-4">
    <Slider :name="field" :min="min" :max="max" :step="step" class="flex-auto" />
    <span class="text-sm text-muted-color w-12 text-right">{{ modelValue }}</span>
  </div>
</template>
<script setup lang="ts">
import { computed } from 'vue'
import Slider from 'primevue/slider'
import type { FormField } from '@/components/dynamics-form-plus/type'

const props = defineProps<{
  formField: FormField
  otherParams: any
  formFieldList: Array<FormField>
  field: any
  form: any
}>()

const field = computed(() => props.formField.field)
const attrs = computed(() => props.formField.attrs || {})
const min = computed(() => attrs.value.min ?? 0)
const max = computed(() => attrs.value.max ?? 100)
const step = computed(() => attrs.value.step ?? 1)
const modelValue = computed(() => props.field?.value ?? props.formField.defaultValue ?? min.value)
</script>
<style lang="scss" scoped></style>
