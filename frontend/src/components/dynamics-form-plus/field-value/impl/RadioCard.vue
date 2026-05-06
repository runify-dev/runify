<template>
  <div class="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 gap-2">
    <div
      v-for="item in optionList"
      :key="item[valueField]"
      class="border rounded-lg p-3 cursor-pointer transition-all text-center text-sm"
      :class="
        field?.value === item[valueField]
          ? 'border-primary-color text-primary-color'
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
