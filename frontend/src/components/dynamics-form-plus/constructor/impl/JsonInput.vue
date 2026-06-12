<template>
  <FormField v-slot="$field" asChild name="defaultValue" initialValue="">
    <label>{{ t('dynamicsForm.impl.defaultValue') }}</label>
    <Textarea rows="4" fluid class="font-mono" placeholder="{}" />
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

const getData = () => {
  const raw = props.form.defaultValue?.value
  let defaultValue = raw
  if (raw && typeof raw === 'string') {
    try {
      defaultValue = JSON.parse(raw)
    } catch {
      // keep as string
    }
  }
  return {
    input_type: 'JsonInput',
    defaultValue,
    showDefaultValue: props.form.showDefaultValue?.value,
    props_info: {
      resolver: `z.any().refine((v) => { if(!v) return true; if(typeof v === 'object') return true; try { JSON.parse(v); return true } catch { return false } }, { message: '${t('dynamicsForm.impl.jsonPlaceholder')}' })`
    }
  }
}

const rander = (form_data: any) => {
  const dv = form_data.defaultValue
  props.setFieldValue('defaultValue', typeof dv === 'object' && dv !== null ? JSON.stringify(dv, null, 2) : dv ?? '')
  props.setFieldValue('showDefaultValue', form_data.showDefaultValue)
}

defineExpose({ getData, rander })
</script>
<style lang="scss" scoped></style>
