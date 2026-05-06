<template>
  <Select
    :name="field"
    :options="optionList"
    :optionLabel="labelField"
    :optionValue="valueField"
    :placeholder="placeholder"
    fluid
  />
</template>
<script setup lang="ts">
import { computed } from 'vue'
import Select from 'primevue/select'
import type { FormField } from '@/components/dynamics-form-plus/type'

const props = defineProps<{
  formField: FormField
  otherParams: any
  formFieldList: Array<FormField>
  field: any
  form: any
}>()

const field = computed(() => props.formField.field)
const labelField = computed(() => props.formField.labelField || 'label')
const valueField = computed(() => props.formField.valueField || 'value')
const optionList = computed(() => props.formField.optionList || [])
const placeholder = computed(() => props.formField.attrs?.placeholder || '请选择')
</script>
<style lang="scss" scoped></style>
