<template>
  <CascadeSelect
    @change="handleInnerChange"
    v-model="data"
    ref="cascadeRef"
    :options="_options"
    optionLabel="label"
    optionGroupLabel="label"
    optionGroupChildren="children"
    showClear
    v-bind="$attrs"
    :formControl="undefined"
    name=""
  >
    <template #value="scope">
      {{
        scope.value && scope.value.fullPathString
          ? scope.value.fullPathString
          : scope.placeholder || t('common.pleaseSelect')
      }}
    </template>
  </CascadeSelect>
</template>
<script setup lang="ts">
import { computed, provide } from 'vue'
import CascadeSelect from 'primevue/cascadeselect'
import { t } from '@/locales'
import enhanceTreeWithPaths, { findNodeByValuePath, type Options } from './index'
provide('$pcForm', undefined)
provide('$pcFormField', undefined)
const props = defineProps<{ options: Array<any>; modelValue?: any; config: Options }>()
const handleInnerChange = (event: any) => {
  // 1. 若事件有原始DOM事件，手动阻止冒泡
  if (event.originalEvent) {
    event.originalEvent.stopPropagation()
  }
  // 2. 只对外抛出fullValue（清空时 value 为 null，回退为空数组）
  emit('update:modelValue', event.value?.fullValue || [])
}
const _options = computed(() => {
  return enhanceTreeWithPaths(props.options, props.config)
})

const emit = defineEmits(['update:modelValue'])
const data = computed({
  get: () => {
    const v = findNodeByValuePath(_options.value, props.modelValue, props.config)
    if (v) {
      return v
    }
    // 返回 null（而非空数组），未选中时 d_value 为 null，清空按钮才会隐藏
    return null
  },
  set: (event: any) => {
    // 清空时 event 为 null，回退为空数组
    emit('update:modelValue', event?.fullValue || [])
  }
})
</script>
<style lang="scss"></style>
