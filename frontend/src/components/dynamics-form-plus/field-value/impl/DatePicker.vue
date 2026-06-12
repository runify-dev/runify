<template>
  <DatePicker
    :modelValue="val"
    @update:modelValue="onUpdate"
    :placeholder="placeholder"
    :dateFormat="dateFormat"
    :showTime="showTime"
    :hourFormat="hourFormat"
    fluid
  />
</template>
<script setup lang="ts">
import { computed } from 'vue'
import DatePicker from 'primevue/datepicker'
import type { FormField } from '@/components/dynamics-form-plus/type'
import { t } from '@/locales'

const props = defineProps<{
  formField: FormField
  formFieldList: Array<FormField>
  formValue: Record<string, any>
}>()
const emit = defineEmits(['change'])

const field = computed(() => props.formField.field)
const val = computed(() => props.formValue[field.value])
const attrs = computed(() => props.formField.attrs || {})
const placeholder = computed(() => attrs.value.placeholder || t('dynamicsForm.default.placeholder'))
const dateFormat = computed(() => attrs.value.dateFormat || 'yy-mm-dd')
const showTime = computed(() => attrs.value.showTime ?? false)
const hourFormat = computed(() => attrs.value.hourFormat || '24')
const onUpdate = (v: any) => emit('change', field.value, v)
</script>
<style lang="scss" scoped></style>
