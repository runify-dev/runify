<template>
  <FormField v-slot="$field" asChild name="defaultValue" :initialValue="false">
    <label>{{ t('dynamicsForm.impl.defaultValue') }}</label>
    <ToggleSwitch />
    <Message v-if="$field?.invalid" severity="error" size="small" variant="simple">{{
      $field.error?.message
    }}</Message>
  </FormField>
</template>
<script setup lang="ts">
import { t } from '@/locales'
import ToggleSwitch from 'primevue/toggleswitch'

const props = defineProps<{
  form: any
  setFieldValue: (field: string, value: any) => void
}>()

const getData = () => {
  return {
    input_type: 'SwitchInput',
    defaultValue: props.form.defaultValue?.value ?? false,
    showDefaultValue: props.form.showDefaultValue?.value
  }
}

const rander = (form_data: any) => {
  props.setFieldValue('defaultValue', form_data.defaultValue ?? false)
  props.setFieldValue('showDefaultValue', form_data.showDefaultValue)
}

defineExpose({ getData, rander })
</script>
<style lang="scss" scoped></style>
