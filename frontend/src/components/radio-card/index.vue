<template>
  <div class="radio_content" :style="radioContentStyle">
    <div class="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-3 mt-5">
      <Card
        v-for="item in optionList"
        :key="item.value"
        v-bind="$attrs"
        shadow="never"
        :class="[
          inputDisabled ? 'is-disabled' : '',
          modelValue == item[valueField] ? 'active' : ''
        ]"
        @click="inputDisabled ? () => {} : selected(item[valueField])"
      >
        <template #content>
          <slot v-bind="item">
            {{ item.label }}
          </slot>
        </template>
      </Card>
    </div>
  </div>
</template>
<script lang="ts" setup>
import { computed, ref, inject, nextTick } from 'vue'
import { useFormDisabled, formItemContextKey } from 'element-plus'

const inputDisabled = useFormDisabled()

defineProps<{
  // 选中的值
  modelValue?: any
  optionList: Array<any>
  valueField: string
}>()
const elFormItem = inject(formItemContextKey, void 0)
const selected = (activeValue: string | number) => {
  emit('update:modelValue', activeValue)
  nextTick(() => {
    if (elFormItem?.validate) {
      elFormItem.validate('change')
    }
  })
}
const emit = defineEmits(['update:modelValue', 'change'])
const width = ref<number>()
const radioContentStyle = computed(() => {
  if (width.value) {
    if (width.value < 350) {
      return { '--maxkb-radio-card-width': '316px' }
    } else if (width.value > 770) {
      return { '--maxkb-radio-card-width': '378px' }
    } else {
      return { '--maxkb-radio-card-width': '100%' }
    }
  }
  return {}
})
</script>
<style lang="scss" scoped>
.radio_content {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-start;
  width: 100%;

  .is-disabled {
    border: 1px solid var(--el-card-border-color);
    background-color: var(--el-fill-color-light);
    color: var(--el-text-color-placeholder);
    cursor: not-allowed;
    &:hover {
      cursor: not-allowed;
    }
  }
  .active {
    border: 1px solid var(--el-color-primary);
    color: var(--el-color-primary);
  }
  .item {
    line-height: 22px;
    cursor: pointer;
    display: flex;
    justify-content: center;
    align-items: center;
    width: var(--maxkb-radio-card-width, 100%);
    margin: 4px;
  }
}
</style>
