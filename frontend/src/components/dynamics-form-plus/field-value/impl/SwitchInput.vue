<template>
  <ToggleSwitch :modelValue="val" @update:modelValue="onUpdate" />
</template>
<script setup lang="ts">
import { computed } from 'vue'
import ToggleSwitch from 'primevue/toggleswitch'
import type { FormField } from '@/components/dynamics-form-plus/type'

const props = defineProps<{
  formField: FormField
  formFieldList: Array<FormField>
  formValue: Record<string, any>
}>()
const emit = defineEmits(['change'])

const field = computed(() => props.formField.field)
const val = computed(() => props.formValue[field.value])
const onUpdate = (v: any) => emit('change', field.value, v)
</script>
<style lang="scss" scoped></style>
