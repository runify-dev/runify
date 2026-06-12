<template>
  <div class="flex flex-col gap-1">
    <FieldLabel
      :formField="formField"
      :formFieldList="formFieldList"
      :formValue="formValue"
    />
    <FieldValue
      :formField="formField"
      :formFieldList="formFieldList"
      :formValue="formValue"
      @change="onFieldChange"
    />
    <Message v-if="errorMsg" severity="error" size="small" variant="simple">
      {{ errorMsg }}
    </Message>
  </div>
</template>
<script setup lang="ts">
import { ref } from 'vue'
import { t } from '@/locales'
import type { FormField } from '@/components/dynamics-form-plus/type'
import FieldLabel from '@/components/dynamics-form-plus/field-label/index.vue'
import FieldValue from '@/components/dynamics-form-plus/field-value/index.vue'
import { z } from 'zod'

const props = defineProps<{
  formField: FormField
  formFieldList: Array<FormField>
  formValue: Record<string, any>
}>()

const emit = defineEmits(['change'])

const errorMsg = ref<string>('')

const buildSchema = () => {
  const info = props.formField.propsInfo
  let schema: any

  if (info?.resolver) {
    try {
      const code = Array.isArray(info.resolver) ? info.resolver.join('\n') : info.resolver
      schema = new Function('z', `return ${code}`)(z)
    } catch {
      // fallback
    }
  }

  if (!schema) {
    schema = props.formField.required
      ? z
          .any()
          .refine((val) => val !== undefined && val !== '' && val !== null, {
            message: (props.formField.label?.value || props.formField.label || props.formField.field) + ' ' + t('dynamicsForm.impl.requiredField')
          })
      : z.any()
  }

  return schema
}

const validateField = () => {
  const schema = buildSchema()
  const val = props.formValue[props.formField.field]
  const result = schema.safeParse(val)
  if (result.success) {
    errorMsg.value = ''
    return true
  }
  errorMsg.value = result.error.issues?.[0]?.message || t('dynamicsForm.impl.validationFailed')
  return false
}

const onFieldChange = (field: string, value: any) => {
  emit('change', field, value)
  validateField()
}

const validate = (): Record<string, string> => {
  validateField()
  if (errorMsg.value) {
    return { [props.formField.field]: errorMsg.value }
  }
  return {}
}

defineExpose({ validate })
</script>
<style lang="scss" scoped></style>
