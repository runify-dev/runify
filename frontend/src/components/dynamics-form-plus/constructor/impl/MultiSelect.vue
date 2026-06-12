<template>
  <FormField v-slot="$field" asChild name="labelField" initialValue="label">
    <label>{{ t('dynamicsForm.impl.labelField') }}</label>
    <InputText fluid placeholder="label" />
    <Message v-if="$field?.invalid" severity="error" size="small" variant="simple">{{
      $field.error?.message
    }}</Message>
  </FormField>
  <FormField v-slot="$field" asChild name="valueField" initialValue="value">
    <label>{{ t('dynamicsForm.impl.valueField') }}</label>
    <InputText fluid placeholder="value" />
    <Message v-if="$field?.invalid" severity="error" size="small" variant="simple">{{
      $field.error?.message
    }}</Message>
  </FormField>
  <FormField v-slot="$field" asChild name="optionList" initialValue="">
    <label>{{ t('dynamicsForm.impl.optionData') }}</label>
    <Textarea rows="4" fluid class="font-mono" :placeholder="t('dynamicsForm.impl.optionExample')" />
    <Message v-if="$field?.invalid" severity="error" size="small" variant="simple">{{
      $field.error?.message
    }}</Message>
  </FormField>
</template>
<script setup lang="ts">
import { t } from '@/locales'
import Textarea from 'primevue/textarea'

const props = defineProps<{
  form: any
  setFieldValue: (field: string, value: any) => void
}>()

const parseOptionList = (raw: string) => {
  if (!raw) return []
  try {
    return JSON.parse(raw)
  } catch {
    return []
  }
}

const getData = () => {
  return {
    input_type: 'MultiSelect',
    labelField: props.form.labelField?.value || 'label',
    valueField: props.form.valueField?.value || 'value',
    optionList: parseOptionList(props.form.optionList?.value),
    defaultValue: props.form.defaultValue?.value,
    showDefaultValue: props.form.showDefaultValue?.value
  }
}

const rander = (form_data: any) => {
  props.setFieldValue('labelField', form_data.labelField || 'label')
  props.setFieldValue('valueField', form_data.valueField || 'value')
  props.setFieldValue('optionList', JSON.stringify(form_data.optionList || [], null, 2))
  props.setFieldValue('defaultValue', form_data.defaultValue)
  props.setFieldValue('showDefaultValue', form_data.showDefaultValue)
}

defineExpose({ getData, rander })
</script>
<style lang="scss" scoped></style>
