<template>{{ formField.label.value }}</template>
<script setup lang="ts">
import type { FormField } from '@/components/dynamics-form/type'
defineProps<{
  // 表单Item
  formField: FormField
}>()
</script>
<style lang="scss" scoped></style>
