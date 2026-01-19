<template>
  <Form ref="formRef" @submit.stop class="flex flex-col gap-4" v-slot="$form">
    <slot></slot>
    <template v-for="item in formFieldList" :key="item.field">
      <FormFieldItem
        ref="formFieldRef"
        :key="item.field"
        v-if="show(item)"
        :formField="item"
        :form-field-list="formFieldList"
        :otherParams="otherParams"
        :form="$form"
      >
      </FormFieldItem>
    </template>
    <slot name="after" v-bind="formValue"></slot>
  </Form>
</template>
<script lang="ts" setup>
import type { Dict } from '@/api/type/common'
import FormFieldItem from './form-field-item/index.vue'
import type { FormField } from '@/components/dynamics-form/type'
import { ref } from 'vue'
import { Form, type FormInstance } from '@primevue/forms'
import type Result from '@/request/Result'
import _ from 'lodash'

defineOptions({ name: 'dynamicsFormPlus' })
withDefaults(
  defineProps<{
    // 其他参数
    otherParams?: any
  }>(),
  { otherParams: () => ({}) }
)
const emit = defineEmits(['update:modelValue'])
const formValue = ref<Dict<any>>({})

const loading = ref<boolean>(false)

const formFieldList = ref<Array<FormField>>([])

const formRef = ref<FormInstance>()

const formFieldRef = ref<Array<InstanceType<typeof FormFieldItem>>>([])
/**
 * 当前 field是否展示
 * @param field
 */
const show = (field: FormField) => {
  if (field.relation_show_field_dict) {
    const keys = Object.keys(field.relation_show_field_dict)
    for (const index in keys) {
      const key = keys[index]
      const v = _.get(formValue.value, key)
      if (v && v !== undefined && v !== null) {
        const values = field.relation_show_field_dict[key]
        if (values && values.length > 0) {
          return values.includes(v)
        } else {
          return true
        }
      } else {
        return false
      }
    }
  }
  return true
}

/**
 * 表单字段修改
 * @param field
 * @param value
 */
const change = (field: FormField, value: any) => {
  formValue.value[field.field] = value
  emit('update:modelValue', formValue.value)
}

const render = (
  render_data: string | Array<FormField> | Promise<Result<Array<FormField>>>,
  data?: Dict<any>
) => {
  if (render_data instanceof Array) {
    formFieldList.value = render_data
  }
  data = data ? data : {}
  const value = formFieldList.value
    .map((item) => {
      if (data[item.field] !== undefined) {
        if (item.valueField && item.optionList && item.optionList.length > 0) {
          const value_field = item.valueField
          const find = item.optionList?.find((i) => {
            if (typeof data[item.field] === 'string') {
              return i[value_field] === data[item.field]
            } else {
              return data[item.field].indexOf([value_field]) === -1
            }
          })
          if (find) {
            return { [item.field]: data[item.field] }
          }
        } else {
          return { [item.field]: data[item.field] }
        }
      } else {
        if (item.showDefaultValue === true && item.showDefaultValue) {
          return { [item.field]: item.defaultValue }
        }
      }
      return {}
    })
    .reduce((x, y) => ({ ...x, ...y }), {})
  formRef.value?.setValues(value)
}
/**
 * 校验函数
 */
const validate = () => {
  return Promise.all([
    ...formFieldRef.value.map((item: any) => item.validate()),
    formRef.value ? formRef.value.validate() : Promise.resolve()
  ])
}

// 暴露获取当前表单数据函数
defineExpose({
  validate,
  render,
  formRef
})
</script>

<style lang="scss"></style>
