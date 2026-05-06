<template>
  <div class="flex flex-wrap gap-2">
    <div
      v-for="item in optionList"
      :key="item[valueField]"
      class="border rounded-full px-4 py-1.5 cursor-pointer transition-all text-sm"
      :class="
        field?.value === item[valueField]
          ? 'border-primary-color text-primary-color bg-primary-50'
          : 'border-surface-border text-color hover:border-primary-color'
      "
      @click="select(item[valueField])"
    >
      {{ item[labelField] }}
    </div>
  </div>
</template>
<script setup lang="ts">
import { computed } from 'vue'
import type { FormField } from '@/components/dynamics-form-plus/type'

const props = defineProps<{
  formField: FormField
  otherParams: any
  formFieldList: Array<FormField>
  field: any
  form: any
}>()

const labelField = computed(() => props.formField.labelField || 'label')
const valueField = computed(() => props.formField.valueField || 'value')
const optionList = computed(() => props.formField.optionList || [])

const select = (value: any) => {
  props.form?.setFieldValue(props.formField.field, value)
}
</script>
<style lang="scss" scoped></style>
