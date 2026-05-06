<template>
  <div>
    <SelectButton
      v-model="data.location"
      :options="locationOptions"
      option-label="label"
      option-value="value"
    />

    <Cascader
      v-if="data.location === 'reference'"
      :config="{ labelKey: 'label', valueKey: 'value' }"
      :options="options"
      v-model="data.reference"
      optionLabel="label"
      :optionGroupChildren="['children']"
      class="w-full"
      placeholder="请选择引用参数"
    />
    <TemplateEditor
      v-else
      :model-value="data.value || ''"
      @update:model-value="onValueChange"
      :variables="variables"
      title="响应内容"
      style="height: 200px"
    />
  </div>
</template>

<script setup lang="ts">
import { inject, provide } from 'vue'
import { computed } from 'vue'
import Cascader from '@/components/cascader/index.vue'
import TemplateEditor from '@/components/template-editor/index.vue'
provide('$pcForm', undefined)
provide('$pcFormField', undefined)
const props = defineProps<{
  modelValue: any
}>()
const locationOptions = [
  { label: '引用', value: 'reference' },
  { label: '自定义', value: 'customize' }
]

const getNodeFieldOptions = inject('getNodeFieldOptions') as any
const getTemplateVariables = inject('getTemplateVariables') as any
const options = getNodeFieldOptions()
const variables = getTemplateVariables()
const emit = defineEmits(['update:modelValue'])
const data = computed({
  get: () => {
    if (!props.modelValue) {
      emit('update:modelValue', { location: '', value: '' })
    }
    return props.modelValue
  },
  set: (e) => {
    emit('update:modelValue', e)
  }
})

function onValueChange(value: string) {
  emit('update:modelValue', { ...data.value, value })
}
</script>
<style lang="scss" scoped></style>
