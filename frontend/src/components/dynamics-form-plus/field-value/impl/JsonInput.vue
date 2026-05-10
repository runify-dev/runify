<template>
  <div class="flex flex-col gap-1">
    <div class="flex justify-end">
      <Button label="格式化" size="small" text severity="secondary" @click="format" />
    </div>
    <Textarea
      :modelValue="val"
      @update:modelValue="onUpdate"
      :placeholder="placeholder"
      rows="5"
      fluid
      class="font-mono"
    />
  </div>
</template>
<script setup lang="ts">
import { computed } from 'vue'
import Textarea from 'primevue/textarea'
import type { FormField } from '@/components/dynamics-form-plus/type'

const props = defineProps<{
  formField: FormField
  formFieldList: Array<FormField>
  formValue: Record<string, any>
}>()
const emit = defineEmits(['change'])

const field = computed(() => props.formField.field)
const val = computed(() => props.formValue[field.value])
const placeholder = computed(() => props.formField.attrs?.placeholder || '请输入 JSON')
const onUpdate = (v: any) => emit('change', field.value, v)

const format = () => {
  const raw = props.formValue[props.formField.field]
  if (!raw) return
  try {
    emit('change', field.value, JSON.stringify(JSON.parse(raw), null, 2))
  } catch {
    // invalid json
  }
}
</script>
<style lang="scss" scoped></style>
