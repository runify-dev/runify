<template>
  <div class="flex flex-col gap-1">
    <div class="flex justify-end">
      <Button label="格式化" size="small" text severity="secondary" @click="format" />
    </div>
    <Textarea :name="field" :placeholder="placeholder" rows="5" fluid class="font-mono" />
  </div>
</template>
<script setup lang="ts">
import { computed } from 'vue'
import Textarea from 'primevue/textarea'
import type { FormField } from '@/components/dynamics-form-plus/type'

const props = defineProps<{
  formField: FormField
  otherParams: any
  formFieldList: Array<FormField>
  field: any
  form: any
}>()

const field = computed(() => props.formField.field)
const placeholder = computed(() => props.formField.attrs?.placeholder || '请输入 JSON')

const format = () => {
  const val = props.form?.values?.[props.formField.field]
  if (!val) return
  try {
    const formatted = JSON.stringify(JSON.parse(val), null, 2)
    props.form?.setFieldValue(props.formField.field, formatted)
  } catch {
    // invalid json, do nothing
  }
}
</script>
<style lang="scss" scoped></style>
