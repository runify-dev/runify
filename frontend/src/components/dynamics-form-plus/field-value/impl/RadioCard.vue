<template>
  <div class="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 gap-2">
    <div
      v-for="item in optionList"
      :key="item[valueField]"
      class="border rounded-lg p-3 cursor-pointer transition-all text-center text-sm"
      :class="
        val === item[valueField]
          ? 'border-primary-color text-primary-color'
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
