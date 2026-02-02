<template>
  <FormField v-slot="$field" :resolver="resolver" :name="field" class="flex flex-col gap-1">
    <FieldLabel
      :field="$field"
      :form="form"
      :formField="formField"
      :otherParams="otherParams"
      :formFieldList="formFieldList"
    >
    </FieldLabel>
    <FieldValue
      :field="$field"
      :form="form"
      :formField="formField"
      :otherParams="otherParams"
      :formFieldList="formFieldList"
    ></FieldValue>
    <Message v-if="$field?.invalid" severity="error" size="small" variant="simple">{{
      $field.error?.message
    }}</Message>
  </FormField>
</template>
<script setup lang="ts">
import { ref, computed } from 'vue'
import type { FormField } from '@/components/dynamics-form/type'
import FieldLabel from '@/components/dynamics-form-plus/field-label/index.vue'
import FieldValue from '@/components/dynamics-form-plus/field-value/index.vue'
import bus from '@/bus'
import { t } from '@/locales'
import { zodResolver } from '@primevue/forms/resolvers/zod'
import { z } from 'zod'
const props = defineProps<{
  // 表单Item
  formField: FormField
  // 其他参数
  otherParams: any
  // 所有字段
  formFieldList: Array<FormField>
  // Form v-slot="$form"
  form: any
}>()
const emit = defineEmits(['change'])

const componentFormRef = ref<any>()

const props_info = computed(() => {
  return props.formField?.propsInfo ? props.formField.propsInfo : {}
})

/**
 * 校验
 */
const resolver = computed(() => {
  return zodResolver(
    props_info.value.resolver
      ? new Function('z', `return ${props_info.value.resolver}`)(z)
      : props.formField.required
        ? z.any().refine(
            (val) => {
              console.log(val)
              return val !== undefined && val !== '' && val !== null
            },
            {
              message: props.formField.label.value + ' ' + '此项必填'
            }
          )
        : z.any()
  )
})

const validate = () => {
  if (componentFormRef.value) {
    return componentFormRef.value.validate()
  }
  return Promise.resolve()
}
const field = computed(() => {
  return props.formField.field
})
defineExpose({ validate })
</script>
<style lang="scss" scoped></style>
