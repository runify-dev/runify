<template>
  <Form ref="formRef" @submit.stop class="flex flex-col gap-4" v-slot="$form">
    <FormField v-slot="$field" asChild name="field" initialValue="" :resolver="resolver.field">
      <IftaLabel
        ><label>{{ $t('dynamicsForm.constructor.field.label') }}</label>
        <InputText
          type="text"
          fluid
          :placeholder="$t('dynamicsForm.constructor.field.placeholder')"
      /></IftaLabel>

      <Message v-if="$field?.invalid" severity="error" size="small" variant="simple">{{
        $field.error?.message
      }}</Message>
    </FormField>
    <FormField v-slot="$field" asChild name="label" initialValue="" :resolver="resolver.label">
      <IftaLabel>
        <label>{{ $t('dynamicsForm.constructor.name.label') }}</label>
        <InputText type="text" fluid :placeholder="$t('dynamicsForm.constructor.name.placeholder')"
      /></IftaLabel>
      <Message v-if="$field?.invalid" severity="error" size="small" variant="simple">{{
        $field.error?.message
      }}</Message>
    </FormField>
    <FormField v-slot="$field" asChild name="tooltip" initialValue="" :resolver="resolver.tooltip">
      <IftaLabel>
        <label>{{ $t('dynamicsForm.constructor.tooltip.label') }}</label>
        <InputText type="text" fluid
      /></IftaLabel>

      <Message v-if="$field?.invalid" severity="error" size="small" variant="simple">{{
        $field.error?.message
      }}</Message>
    </FormField>
    <FormField
      v-slot="$field"
      asChild
      name="required"
      :initialValue="false"
      :resolver="resolver.required"
    >
      <label>{{ $t('dynamicsForm.constructor.required.label') }}</label>
      <ToggleSwitch />
      <Message v-if="$field?.invalid" severity="error" size="small" variant="simple">{{
        $field.error?.message
      }}</Message>
    </FormField>
    <FormField v-slot="$field" asChild name="type" initialValue="" :resolver="resolver.type">
      <label>{{ $t('dynamicsForm.constructor.input_type.label') }}</label>
      <Select
        :options="input_type_list"
        optionLabel="label"
        optionValue="value"
        :placeholder="$t('dynamicsForm.constructor.input_type.placeholder')"
        fluid
      >
      </Select>
      <Message v-if="$field?.invalid" severity="error" size="small" variant="simple">{{
        $field.error?.message
      }}</Message>
    </FormField>
    {{ $form.type?.value }}
    <component
      v-if="$form.type?.value"
      ref="componentFormRef"
      :setFieldValue="setFieldValue"
      :form="$form"
      :is="impl[$form.type.value]"
    ></component>
  </Form>
</template>
<script setup lang="ts">
import { onMounted, ref, nextTick, computed } from 'vue'
import _ from 'lodash'
import { input_type_list as input_type_list_data } from './data'
import { t } from '@/locales'
import { zodResolver } from '@primevue/forms/resolvers/zod'
import { z } from 'zod'
import type { VueModule } from '@/api/type/common'
import { FormField, type FormInstance } from '@primevue/forms'
const impl: Record<string, any> = Object.fromEntries(
  Object.entries(
    import.meta.glob<VueModule>('./impl/*.vue', {
      eager: true
    })
  ).map(([path, module]) => [path.replace(/^.*\/(.+)\.vue$/, '$1'), module.default])
)
const formRef = ref<FormInstance>()
const resolver = computed(() => {
  return {
    field: zodResolver(z.string().min(1, `字段必须在1-64之间`).max(64, `字段必须在1-64之间`)),
    label: zodResolver(
      z.string().min(1, `显示名称必须在1-64之间`).max(64, `显示名称必须在1-64之间`)
    ),
    tooltip: zodResolver(
      z.string().min(1, `参数提示说明必须在1-64之间`).max(64, `参数提示说明必须在1-64之间`)
    ),
    required: zodResolver(z.boolean({ error: '是否必填为必填参数' })),
    type: zodResolver(z.string().min(1, `组件类型必填`))
  }
})

const setFieldValue = (field: string, value: any) => {
  formRef.value?.setFieldValue(field, value)
}
const props = withDefaults(
  defineProps<{
    modelValue?: any
    input_type_list?: Array<{ label: string; value: string }>
  }>(),
  {
    input_type_list: () =>
      input_type_list_data.map((item) => ({ label: item.label, value: item.value }))
  }
)
const componentFormRef = ref<any>()

const getData = () => {
  const states = formRef.value?.states

  if (states) {
    return {
      label: { value: states.label?.value, tooltip: states.tooltip?.value, type: 'TooltipLabel' },
      required: states.required?.value,
      field: states.field?.value,
      type: states.type?.value,
      defaultValue: states.defaultValue?.value,
      showDefaultValue: states.showDefaultValue?.value,
      ...componentFormRef.value?.getData?.()
    }
  }
  return {}
}

const validate = () => {
  if (formRef.value) {
    return formRef.value?.validate()
  }
  return Promise.reject()
}

onMounted(() => {
  if (props.modelValue) {
    rander(props.modelValue)
  }
})
const rander = (data: any) => {
  setFieldValue('required', data.required ? data.required : false)
  setFieldValue('showDefaultValue', data.showDefaultValue)
  setFieldValue('type', data.type)
  setFieldValue('label', data.label.value)
  setFieldValue('tooltip', data.label.tooltip)
  setFieldValue('field', data.field)
  nextTick(() => {
    if (componentFormRef.value) {
      componentFormRef.value?.rander(data)
    }
  })
}

defineExpose({ getData, validate, rander })
</script>
<style lang="scss"></style>
