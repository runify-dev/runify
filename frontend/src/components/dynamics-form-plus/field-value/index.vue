<template>
  <component
    :is="impl[type]"
    :formField="formField"
    :otherParams="otherParams"
    :formFieldList="formFieldList"
    :field="field"
    :form="form"
  ></component>
</template>
<script setup lang="ts">
import type { FormField } from '@/components/dynamics-form/type'
import type { VueModule } from '@/api/type/common'
import { computed } from 'vue'
const impl: Record<string, any> = Object.fromEntries(
  Object.entries(
    import.meta.glob<VueModule>('./impl/**/*.vue', {
      eager: true
    })
  ).map(([path, module]) => [path.replace(/^.*\/(.+)\.vue$/, '$1'), module.default])
)
const props = defineProps<{
  // 表单Item
  formField: FormField
  // 其他参数
  otherParams: any
  // 所有字段
  formFieldList: Array<FormField>
  // FormField v-slot="$field"
  field: any
  // Form v-slot="$form"
  form: any
}>()
const type: any = computed(() => {
  return props.formField.type
})
</script>
<style lang="scss"></style>
