<template>
  <div class="flex-auto">
    <FormField v-slot="$field" asChild name="minlength" :initialValue="0">
      <label>密码长度最小</label>
      <InputNumber inputId="minlength" fluid />
      <Message v-if="$field?.invalid" severity="error" size="small" variant="simple">{{
        $field.error?.message
      }}</Message>
    </FormField>
  </div>
  <div class="flex-auto">
    <FormField v-slot="$field" asChild name="maxlength" :initialValue="64">
      <label>密码长度最大</label>
      <InputNumber inputId="maxlength" fluid />
      <Message v-if="$field?.invalid" severity="error" size="small" variant="simple">{{
        $field.error?.message
      }}</Message>
    </FormField>
  </div>
  <FormField v-slot="$field" asChild name="defaultValue" initialValue="">
    <label>默认值</label>
    <Password :feedback="false" toggleMask fluid />
    <Message v-if="$field?.invalid" severity="error" size="small" variant="simple">{{
      $field.error?.message
    }}</Message>
  </FormField>
</template>
<script setup lang="ts">
import Password from 'primevue/password'

const props = defineProps<{
  form: any
  setFieldValue: (field: string, value: any) => void
}>()

const getData = () => {
  return {
    input_type: 'PasswordInput',
    attrs: {
      minlength: props.form.minlength?.value ?? 0,
      maxlength: props.form.maxlength?.value ?? 64
    },
    defaultValue: props.form.defaultValue?.value,
    showDefaultValue: props.form.showDefaultValue?.value
  }
}

const rander = (form_data: any) => {
  const attrs = form_data.attrs || {}
  props.setFieldValue('minlength', attrs.minlength)
  props.setFieldValue('maxlength', attrs.maxlength)
  props.setFieldValue('defaultValue', form_data.defaultValue)
  props.setFieldValue('showDefaultValue', form_data.showDefaultValue)
}

defineExpose({ getData, rander })
</script>
<style lang="scss" scoped></style>
