<template>
  <div class="flex flex-col gap-4">
    <slot v-bind="formValue"></slot>
    <template v-for="item in formFieldList" :key="item.field">
      <FormFieldItem
        v-if="show(item)"
        :formField="item"
        :formFieldList="formFieldList"
        :formValue="formValue"
        @change="onChange"
      />
    </template>
    <slot name="after" v-bind="formValue"></slot>
  </div>
</template>
<script lang="ts" setup>
import FormFieldItem from './form-field-item/index.vue'
import type { FormField } from './type'
import { ref, nextTick } from 'vue'
import _ from 'lodash'

defineOptions({ name: 'dynamicsFormPlus' })

const props = withDefaults(
  defineProps<{
    modelValue?: Record<string, any>
  }>(),
  { modelValue: () => ({}) }
)

const emit = defineEmits(['update:modelValue'])

const formValue = ref<Record<string, any>>({ ...props.modelValue })
const formFieldList = ref<FormField[]>([])
const formFieldItemRefs = ref<InstanceType<typeof FormFieldItem>[]>([])

const show = (field: FormField) => {
  if (field.relationShowFieldDict) {
    const keys = Object.keys(field.relationShowFieldDict)
    for (const key of keys) {
      const v = _.get(formValue.value, key)
      if (v !== undefined && v !== null) {
        const values = field.relationShowFieldDict[key]
        if (values && values.length > 0) {
          return values.includes(v)
        }
        return true
      }
      return false
    }
  }
  return true
}

const setFieldValue = (field: string, value: any) => {
  formValue.value[field] = value
  emit('update:modelValue', { ...formValue.value })
}

const onChange = (field: string, value: any) => {
  setFieldValue(field, value)
}

const render = (fields: FormField[], data?: Record<string, any>) => {
  formFieldList.value = fields
  data = data || {}
  nextTick(() => {
    fields.forEach((item) => {
      const v = _.get(data, item.field)
      if (v !== undefined) {
        formValue.value[item.field] = v
      } else if (item.showDefaultValue && item.defaultValue !== undefined) {
        formValue.value[item.field] = item.defaultValue
      }
    })
    emit('update:modelValue', { ...formValue.value })
  })
}

const validate = (): { values: Record<string, any>; errors: Record<string, string> } => {
  const errors: Record<string, string> = {}
  formFieldItemRefs.value.forEach((item) => {
    const fieldErrors = item.validate()
    Object.assign(errors, fieldErrors)
  })
  return { values: { ...formValue.value }, errors }
}

defineExpose({ validate, render, formValue, setFieldValue })
</script>
<style lang="scss"></style>
