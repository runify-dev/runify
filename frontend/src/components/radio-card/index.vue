<template>
  <div class="grid gap-2" :class="gridClass">
    <div
      v-for="item in optionList"
      :key="item[valueField]"
      class="border rounded-lg px-4 py-2 min-h-10 cursor-pointer transition-all flex items-center justify-center"
      :class="[
        disabled
          ? 'opacity-50 cursor-not-allowed border-surface-border bg-surface-50'
          : modelValue === item[valueField]
            ? 'border-primary-color text-primary-color bg-primary-50 dark:bg-primary-50/10 shadow-sm'
            : 'border-surface-border text-color hover:border-primary-color/50 hover:bg-surface-50'
      ]"
      @click="!disabled && select(item[valueField])"
    >
      <slot v-bind="item">
        {{ item.label }}
      </slot>
    </div>
  </div>
</template>
<script lang="ts" setup>
defineProps<{
  modelValue?: any
  optionList: Array<any>
  valueField: string
  disabled?: boolean
  gridClass?: string
}>()
const emit = defineEmits(['update:modelValue', 'change'])
const select = (value: any) => {
  emit('update:modelValue', value)
}
</script>
<style lang="scss" scoped></style>
