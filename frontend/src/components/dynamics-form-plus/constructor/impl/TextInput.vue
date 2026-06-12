<template>
  <div class="flex-auto">
    <FormField v-slot="$field" asChild name="minlength" :initialValue="0">
      <label>{{ t('dynamicsForm.impl.textMinLength') }}</label>
      <InputNumber inputId="minlength" fluid />
      <Message v-if="$field?.invalid" severity="error" size="small" variant="simple">{{
        $field.error?.message
      }}</Message>
    </FormField>
  </div>
  <div class="flex-auto">
    <FormField v-slot="$field" asChild name="maxlength" :initialValue="10">
      <label>{{ t('dynamicsForm.impl.textMaxLength') }}</label>
      <InputNumber inputId="maxlength" fluid />
      <Message v-if="$field?.invalid" severity="error" size="small" variant="simple">{{
        $field.error?.message
      }}</Message>
    </FormField>
  </div>
  <FormField v-slot="$field" asChild name="defaultValue" initialValue="" :resolver="resolver">
    <label
      >{{ t('dynamicsForm.impl.defaultValue') }}
      <FormField v-slot="$field" asChild name="showDefaultValue"> <Checkbox binary /> </FormField
    ></label>
    <InputText type="text" />
    <Message v-if="$field?.invalid" severity="error" size="small" variant="simple">
      {{ $field.error?.message }}
    </Message>
  </FormField>
</template>
<script setup lang="ts">
import { computed } from 'vue'
import { t } from '@/locales'
import { zodResolver } from '@primevue/forms/resolvers/zod'
import { z } from 'zod'

const props = defineProps<{
  form: any
  setFieldValue: (field: string, value: any) => void
}>()
const minlength = computed(() => {
  return props.form.minlength ? props.form.minlength.value : 0
})
const maxlength = computed(() => {
  return props.form.maxlength ? props.form.maxlength.value : 10
})
const label = computed(() => {
  return props.form.label ? props.form.label.value : ''
})
const resolver = computed(() => {
  return zodResolver(
    z
      .string()
      .min(minlength.value, `${label.value}${t('dynamicsForm.impl.mustBeGreaterThan')}${minlength.value}`)
      .max(maxlength.value, `${label.value}${t('dynamicsForm.impl.mustBeLessThan')}${maxlength.value}`)
  )
})

const getData = () => {
  return {
    input_type: 'TextInput',
    attrs: {
      maxlength: props.form.maxlength.value,
      minlength: props.form.minlength.value
    },
    defaultValue: props.form.defaultValue.value,
    showDefaultValue: props.form.showDefaultValue?.value,
    props_info: {
      resolver: props.form.required.value
        ? [
            `z
              .string()
              .minLength(props.form.minlength.value, {
                message: \`${props.form.label.value}${t('dynamicsForm.impl.mustBeGreaterThan')}\${props.form.minlength.value}\`
              })
              .maxLength(props.form.maxlength.value, {
                message: \`${props.form.label.value}${t('dynamicsForm.impl.mustBeLessThan')}\${props.form.maxlength.value}\`
              })`
          ]
        : [
            `z
              .string()
              .minLength(props.form.minlength.value, {
                message: \`${props.form.label.value}${t('dynamicsForm.impl.mustBeGreaterThan')}\${props.form.minlength.value}\`
              })
              .maxLength(props.form.maxlength.value, {
                message: \`${props.form.label.value}${t('dynamicsForm.impl.mustBeLessThan')}\${props.form.maxlength.value}\`
              })`
          ]
    }
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
<style lang="scss" scoped>
.defaultValueItem {
  position: relative;
  .defaultValueCheckbox {
    position: absolute;
    right: 0;
    top: -35px;
  }
}
</style>
