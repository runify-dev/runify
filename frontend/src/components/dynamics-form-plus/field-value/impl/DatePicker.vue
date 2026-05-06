<template>
  <DatePicker
    :name="field"
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

const props = defineProps<{
  formField: FormField
  otherParams: any
  formFieldList: Array<FormField>
  field: any
  form: any
}>()

const field = computed(() => props.formField.field)
const attrs = computed(() => props.formField.attrs || {})
const placeholder = computed(() => attrs.value.placeholder || '请选择日期')
const dateFormat = computed(() => attrs.value.dateFormat || 'yy-mm-dd')
const showTime = computed(() => attrs.value.showTime ?? false)
const hourFormat = computed(() => attrs.value.hourFormat || '24')
</script>
<style lang="scss" scoped></style>
