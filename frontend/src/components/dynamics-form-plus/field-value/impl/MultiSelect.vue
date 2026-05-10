<template>
  <MultiSelect
    :modelValue="val"
    @update:modelValue="onUpdate"
    :options="optionList"
    :optionLabel="labelField"
    :optionValue="valueField"
    :placeholder="placeholder"
    :maxSelectedLabels="3"
    fluid
  />
</template>
<script setup lang="ts">
import { computed } from 'vue'
import MultiSelect from 'primevue/multiselect'
import type { FormField } from '@/components/dynamics-form-plus/type'

const props = defineProps<{
  formField: FormField
  formFieldList: Array<FormField>
  formValue: Record<string, any>
}>()
const emit = defineEmits(['change'])

const field = computed(() => props.formField.field)
const val = computed(() => props.formValue[field.value])
const labelField = computed(() => props.formField.labelField || 'label')
const valueField = computed(() => props.formField.valueField || 'value')
const optionList = computed(() => props.formField.optionList || [])
const placeholder = computed(() => props.formField.attrs?.placeholder || '请选择')
const onUpdate = (v: any) => emit('change', field.value, v)
</script>
<style lang="scss" scoped></style>
