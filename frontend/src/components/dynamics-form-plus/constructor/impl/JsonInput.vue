<template>
  <FormField v-slot="$field" asChild name="defaultValue" initialValue="">
    <label>默认值</label>
    <Textarea rows="4" fluid class="font-mono" placeholder="{}" />
    <Message v-if="$field?.invalid" severity="error" size="small" variant="simple">{{
      $field.error?.message
    }}</Message>
  </FormField>
</template>
<script setup lang="ts">
import Textarea from 'primevue/textarea'

const props = defineProps<{
  form: any
  setFieldValue: (field: string, value: any) => void
}>()

const getData = () => {
  return {
    input_type: 'JsonInput',
    defaultValue: props.form.defaultValue?.value,
    showDefaultValue: props.form.showDefaultValue?.value,
    props_info: {
      resolver: `z.string().refine((v) => { try { if(v) JSON.parse(v); return true } catch { return false } }, { message: '请输入合法的 JSON 格式' })`
    }
  }
}

const rander = (form_data: any) => {
  props.setFieldValue('defaultValue', form_data.defaultValue)
  props.setFieldValue('showDefaultValue', form_data.showDefaultValue)
}

defineExpose({ getData, rander })
</script>
<style lang="scss" scoped></style>
