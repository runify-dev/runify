<template>
  <div class="flex flex-wrap gap-2">
    <div
      v-for="item in optionList"
      :key="item[valueField]"
      class="border rounded-full px-4 py-1.5 cursor-pointer transition-all text-sm"
      :class="
        val === item[valueField]
          ? 'border-primary-color text-primary-color bg-primary-50'
          : 'border-surface-border text-color hover:border-primary-color'
      "
      @click="onUpdate(item[valueField])"
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
  formFieldList: Array<FormField>
  formValue: Record<string, any>
}>()
const emit = defineEmits(['change'])

const field = computed(() => props.formField.field)
const val = computed(() => props.formValue[field.value])
const labelField = computed(() => props.formField.labelField || 'label')
const valueField = computed(() => props.formField.valueField || 'value')
const optionList = computed(() => props.formField.optionList || [])
const onUpdate = (v: any) => emit('change', field.value, v)
</script>
<style lang="scss" scoped></style>
