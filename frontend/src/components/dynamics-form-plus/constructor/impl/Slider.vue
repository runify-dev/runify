<template>
  <div class="flex-auto">
    <FormField v-slot="$field" asChild name="min" :initialValue="0">
      <label>{{ t('dynamicsForm.impl.minValue') }}</label>
      <InputNumber inputId="min" fluid />
      <Message v-if="$field?.invalid" severity="error" size="small" variant="simple">{{
        $field.error?.message
      }}</Message>
    </FormField>
  </div>
  <div class="flex-auto">
    <FormField v-slot="$field" asChild name="max" :initialValue="100">
      <label>{{ t('dynamicsForm.impl.maxValue') }}</label>
      <InputNumber inputId="max" fluid />
      <Message v-if="$field?.invalid" severity="error" size="small" variant="simple">{{
        $field.error?.message
      }}</Message>
    </FormField>
  </div>
  <div class="flex-auto">
    <FormField v-slot="$field" asChild name="step" :initialValue="1">
      <label>{{ t('dynamicsForm.impl.step') }}</label>
      <InputNumber inputId="step" :min="1" fluid />
      <Message v-if="$field?.invalid" severity="error" size="small" variant="simple">{{
        $field.error?.message
      }}</Message>
    </FormField>
  </div>
  <FormField v-slot="$field" asChild name="defaultValue" :initialValue="0">
    <label>{{ t('dynamicsForm.impl.defaultValue') }}</label>
    <InputNumber fluid />
    <Message v-if="$field?.invalid" severity="error" size="small" variant="simple">{{
      $field.error?.message
    }}</Message>
  </FormField>
</template>
<script setup lang="ts">
import { t } from '@/locales'

const props = defineProps<{
  form: any
  setFieldValue: (field: string, value: any) => void
}>()

const getData = () => {
  return {
    input_type: 'Slider',
    attrs: {
      min: props.form.min?.value ?? 0,
      max: props.form.max?.value ?? 100,
      step: props.form.step?.value ?? 1
    },
    defaultValue: props.form.defaultValue?.value,
    showDefaultValue: props.form.showDefaultValue?.value
  }
}

const rander = (form_data: any) => {
  const attrs = form_data.attrs || {}
  props.setFieldValue('min', attrs.min)
  props.setFieldValue('max', attrs.max)
  props.setFieldValue('step', attrs.step)
  props.setFieldValue('defaultValue', form_data.defaultValue)
  props.setFieldValue('showDefaultValue', form_data.showDefaultValue)
}

defineExpose({ getData, rander })
</script>
<style lang="scss" scoped></style>
