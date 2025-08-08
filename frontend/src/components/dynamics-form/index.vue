<template>
  <el-form
    @submit.prevent
    class="demo-form-inline"
    ref="ruleFormRef"
    label-width="130px"
    label-suffix=":"
    v-loading="loading"
    v-bind="$attrs"
  >
    <slot></slot>
    <template v-for="item in formFieldList" :key="item.field">
      <FormItem
        ref="formFieldRef"
        :key="item.field"
        v-if="show(item)"
        @change="change(item, $event)"
        v-bind:modelValue="formValue[item.field]"
        :formfield="item"
        :view="view"
        :defaultItemWidth="defaultItemWidth"
        :form-value="formValue"
        :formfield-list="formFieldList"
        :parent_field="parent_field"
        :otherParams="otherParams"
      >
      </FormItem>
    </template>
    <slot name="after" v-bind="formValue"></slot>
  </el-form>
</template>
<script lang="ts" setup>
import type { Dict } from '@/api/type/common'
import FormItem from '@/components/dynamics-form/FormItem.vue'
import type { FormField } from '@/components/dynamics-form/type'
import { ref, watch } from 'vue'
import type { FormInstance } from 'element-plus'
import type Result from '@/request/Result'
import _ from 'lodash'

defineOptions({ name: 'dynamicsForm' })
withDefaults(
  defineProps<{
    // 是否只读
    view?: boolean
    // 默认每个宽度
    defaultItemWidth?: string
    // 其他参数
    otherParams: any

    parent_field?: string

    modelValue?: Dict<any>
  }>(),
  { view: false, defaultItemWidth: '75%' }
)
const emit = defineEmits(['update:modelValue'])
const formValue = ref<Dict<any>>({})

const loading = ref<boolean>(false)

const formFieldList = ref<Array<FormField>>([])

const ruleFormRef = ref<FormInstance>()

const formFieldRef = ref<Array<InstanceType<typeof FormItem>>>([])
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
        if (item.value_field && item.option_list && item.option_list.length > 0) {
          const value_field = item.value_field
          const find = item.option_list?.find((i) => {
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
        if (item.show_default_value === true && item.show_default_value) {
          return { [item.field]: item.default_value }
        }
      }
      return {}
    })
    .reduce((x, y) => ({ ...x, ...y }), {})
  formValue.value = _.cloneDeep(value)
  console.log('ss')
  emit('update:modelValue', formValue.value)
}
/**
 * 校验函数
 */
const validate = () => {
  return Promise.all([
    ...formFieldRef.value.map((item: any) => item.validate()),
    ruleFormRef.value ? ruleFormRef.value.validate() : Promise.resolve()
  ])
}

// 暴露获取当前表单数据函数
defineExpose({
  validate,
  render,
  ruleFormRef
})
</script>

<style lang="scss"></style>
