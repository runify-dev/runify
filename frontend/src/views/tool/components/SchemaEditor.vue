<template>
  <div>
    <div v-for="(field, idx) in fields" :key="idx" class="flex items-center gap-2 mb-2">
      <InputText v-model="field.field" placeholder="字段名" class="w-32" />
      <InputText v-model="labelProxy[idx]" placeholder="显示名" class="flex-1" @update:model-value="setLabel(idx, $event)" />
      <Select v-model="field.type" :options="typeOptions" option-label="label" option-value="value" class="w-40" />
      <div v-if="!simple" class="flex items-center gap-1">
        <Checkbox v-model="field.required" :binary="true" :inputId="`req-${idx}`" />
        <label :for="`req-${idx}`" class="text-xs">必填</label>
      </div>
      <div v-if="secret" class="flex items-center gap-1">
        <Checkbox v-model="field.secret" :binary="true" :inputId="`sec-${idx}`" />
        <label :for="`sec-${idx}`" class="text-xs">密钥</label>
      </div>
      <Button icon="pi pi-trash" severity="danger" variant="text" size="small" @click="remove(idx)" />
    </div>
    <Button icon="pi pi-plus" label="添加字段" size="small" variant="outlined" @click="add" />
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'

const props = withDefaults(
  defineProps<{ modelValue: any[]; secret?: boolean; simple?: boolean }>(),
  { secret: false, simple: false }
)
const emit = defineEmits(['update:modelValue'])

const inputTypes = [
  { label: '文本', value: 'TextInput' },
  { label: '密码', value: 'PasswordInput' },
  { label: '数字', value: 'Slider' },
  { label: '开关', value: 'SwitchInput' },
  { label: '单选', value: 'SingleSelect' },
  { label: '多选', value: 'MultiSelect' },
  { label: 'JSON', value: 'JsonInput' }
]
const outputTypes = [
  { label: 'string', value: 'string' },
  { label: 'number', value: 'number' },
  { label: 'boolean', value: 'boolean' },
  { label: 'object', value: 'object' },
  { label: 'array', value: 'array' }
]
const typeOptions = computed(() => (props.simple ? outputTypes : inputTypes))

const fields = ref<any[]>([])
const labelProxy = ref<string[]>([])

const readLabel = (f: any) => {
  if (f.label && typeof f.label === 'object') return f.label.value || ''
  return f.label || ''
}
const setLabel = (idx: number, val: string | undefined) => {
  const f = fields.value[idx]
  val = val ?? ''
  if (props.simple) {
    f.label = val
  } else {
    f.label = { value: val, tooltip: val, type: 'TooltipLabel' }
  }
  sync()
}

watch(
  () => props.modelValue,
  (v) => {
    fields.value = (v || []).map((f: any) => ({ ...f }))
    labelProxy.value = fields.value.map(readLabel)
  },
  { immediate: true, deep: false }
)

watch(fields, sync, { deep: true })

function sync() {
  emit('update:modelValue', fields.value.map((f) => ({ ...f })))
}

function add() {
  fields.value.push(
    props.simple
      ? { field: '', label: '', type: 'string' }
      : { field: '', label: { value: '', tooltip: '', type: 'TooltipLabel' }, type: 'TextInput', required: false }
  )
  labelProxy.value.push('')
  sync()
}

function remove(idx: number) {
  fields.value.splice(idx, 1)
  labelProxy.value.splice(idx, 1)
  sync()
}
</script>

<style lang="scss" scoped></style>
