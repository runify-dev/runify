<template>
  <div class="flex items-center gap-4">
    <Slider
      :modelValue="val"
      @update:modelValue="onUpdate"
      :min="min"
      :max="max"
      :step="step"
      class="flex-auto"
    />
    <span class="text-sm text-muted-color w-12 text-right">{{ val ?? min }}</span>
  </div>
</template>
<script setup lang="ts">
import { computed } from 'vue'
import Slider from 'primevue/slider'
import type { FormField } from '@/components/dynamics-form-plus/type'

const props = defineProps<{
  formField: FormField
  formFieldList: Array<FormField>
  formValue: Record<string, any>
}>()
const emit = defineEmits(['change'])

const field = computed(() => props.formField.field)
const val = computed(() => props.formValue[field.value])
const attrs = computed(() => props.formField.attrs || {})
const min = computed(() => attrs.value.min ?? 0)
const max = computed(() => attrs.value.max ?? 100)
const step = computed(() => attrs.value.step ?? 1)
const onUpdate = (v: any) => emit('change', field.value, v)
</script>
<style lang="scss" scoped></style>
