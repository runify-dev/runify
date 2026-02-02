<template>
  <CascadeSelect
    @change="handleInnerChange"
    v-model="data"
    ref="cascadeRef"
    :options="_options"
    optionLabel="label"
    optionGroupLabel="label"
    optionGroupChildren="children"
    v-bind="$attrs"
    :formControl="undefined"
    name=""
  >
    <template #value="scope">
      {{
        scope.value && scope.value.fullPathString
          ? scope.value.fullPathString
          : scope.placeholder || '请选择'
      }}
    </template>
  </CascadeSelect>
</template>
<script setup lang="ts">
import { computed, provide } from 'vue'
import CascadeSelect from 'primevue/cascadeselect'
import enhanceTreeWithPaths, { findNodeByValuePath, type Options } from './index'
provide('$pcForm', undefined)
provide('$pcFormField', undefined)
const props = defineProps<{ options: Array<any>; modelValue?: any; config: Options }>()
const handleInnerChange = (event: any) => {
  console.log(event)
  // 1. 若事件有原始DOM事件，手动阻止冒泡
  if (event.originalEvent) {
    event.originalEvent.stopPropagation()
  }
  console.log(event.value?.fullValue)
  // 2. 只对外抛出fullValue
  emit('update:modelValue', event.value?.fullValue || [])
}
const _options = computed(() => {
  return enhanceTreeWithPaths(props.options, props.config)
})

const emit = defineEmits(['update:modelValue'])
const data = computed({
  get: () => {
    console.log(props.modelValue)
    const v = findNodeByValuePath(_options.value, props.modelValue, props.config)
    if (v) {
      return v
    }
    return []
  },
  set: (event: any) => {
    emit('update:modelValue', event.fullValue)
  }
})
</script>
<style lang="scss"></style>
