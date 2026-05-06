<template>
  <ToggleSwitch :name="field" />
</template>
<script setup lang="ts">
import { computed } from 'vue'
import ToggleSwitch from 'primevue/toggleswitch'
import type { FormField } from '@/components/dynamics-form-plus/type'

const props = defineProps<{
  formField: FormField
  otherParams: any
  formFieldList: Array<FormField>
  field: any
  form: any
}>()

const field = computed(() => props.formField.field)
</script>
<style lang="scss" scoped></style>
