<template>
  <FormField v-slot="$field" asChild name="dateFormat" initialValue="yy-mm-dd">
    <label>日期格式</label>
    <InputText fluid placeholder="yy-mm-dd" />
    <Message v-if="$field?.invalid" severity="error" size="small" variant="simple">{{
      $field.error?.message
    }}</Message>
  </FormField>
  <FormField v-slot="$field" asChild name="showTime" :initialValue="false">
    <label>显示时间</label>
    <ToggleSwitch />
    <Message v-if="$field?.invalid" severity="error" size="small" variant="simple">{{
      $field.error?.message
    }}</Message>
  </FormField>
  <FormField v-slot="$field" asChild name="placeholder" initialValue="">
    <label>占位提示</label>
    <InputText fluid placeholder="请选择日期" />
    <Message v-if="$field?.invalid" severity="error" size="small" variant="simple">{{
      $field.error?.message
    }}</Message>
  </FormField>
</template>
<script setup lang="ts">
import ToggleSwitch from 'primevue/toggleswitch'

const props = defineProps<{
  form: any
  setFieldValue: (field: string, value: any) => void
}>()

const getData = () => {
  return {
    input_type: 'DatePicker',
    attrs: {
      dateFormat: props.form.dateFormat?.value || 'yy-mm-dd',
      showTime: props.form.showTime?.value ?? false,
      placeholder: props.form.placeholder?.value || '请选择日期'
    },
    defaultValue: props.form.defaultValue?.value,
    showDefaultValue: props.form.showDefaultValue?.value
  }
}

const rander = (form_data: any) => {
  const attrs = form_data.attrs || {}
  props.setFieldValue('dateFormat', attrs.dateFormat || 'yy-mm-dd')
  props.setFieldValue('showTime', attrs.showTime ?? false)
  props.setFieldValue('placeholder', attrs.placeholder || '')
  props.setFieldValue('defaultValue', form_data.defaultValue)
  props.setFieldValue('showDefaultValue', form_data.showDefaultValue)
}

defineExpose({ getData, rander })
</script>
<style lang="scss" scoped></style>
