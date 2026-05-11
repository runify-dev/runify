<template>
  <div class="flex flex-col gap-1">
    <div class="flex justify-end">
      <Button label="格式化" size="small" text severity="secondary" @click="format" />
    </div>
    <Textarea
      v-model="text"
      :placeholder="placeholder"
      rows="5"
      fluid
      class="font-mono"
      @blur="syncToValue"
    />
  </div>
</template>
<script setup lang="ts">
import { ref, watch, computed } from 'vue'
import Textarea from 'primevue/textarea'
import type { FormField } from '@/components/dynamics-form-plus/type'

const props = defineProps<{
  formField: FormField
  formFieldList: Array<FormField>
  formValue: Record<string, any>
}>()
const emit = defineEmits(['change'])

const field = computed(() => props.formField.field)
const placeholder = computed(() => props.formField.attrs?.placeholder || '请输入 JSON')

const text = ref('')

// formValue 变化时同步到 text（对象→字符串）
watch(
  () => props.formValue[field.value],
  (val) => {
    if (val === undefined || val === null) {
      text.value = ''
    } else if (typeof val === 'object') {
      text.value = JSON.stringify(val, null, 2)
    } else {
      text.value = String(val)
    }
  },
  { immediate: true }
)

// 失焦时把 text 解析为对象存入 formValue
const syncToValue = () => {
  const raw = text.value.trim()
  if (!raw) {
    emit('change', field.value, undefined)
    return
  }
  try {
    emit('change', field.value, JSON.parse(raw))
  } catch {
    // 无效 JSON，保留字符串让校验层报错
    emit('change', field.value, raw)
  }
}

const format = () => {
  const raw = text.value.trim()
  if (!raw) return
  try {
    text.value = JSON.stringify(JSON.parse(raw), null, 2)
    emit('change', field.value, JSON.parse(text.value))
  } catch {
    // invalid json
  }
}
</script>
<style lang="scss" scoped></style>
