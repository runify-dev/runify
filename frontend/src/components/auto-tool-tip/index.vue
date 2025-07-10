<template>
  <el-tooltip v-bind="$attrs" :disabled="!(containerWeight > contentWeight)" effect="dark" placement="bottom"
    popper-class="auto-tooltip-popper">
    <div ref="tagLabel" :class="['auto-tool-tip', className]" :style="style">
      <slot></slot>
    </div>
  </el-tooltip>
</template>
<script setup lang="ts">
import { ref, onMounted, nextTick } from 'vue'
defineProps({ className: String, style: Object })
const tagLabel = ref()
const containerWeight = ref(0)
const contentWeight = ref(0)

onMounted(() => {
  nextTick(() => {
    containerWeight.value = tagLabel.value?.scrollWidth
    contentWeight.value = tagLabel.value?.clientWidth
  })
  window.addEventListener('resize', function () {
    containerWeight.value = tagLabel.value?.scrollWidth
    contentWeight.value = tagLabel.value?.clientWidth
  })
})
</script>
<style lang="scss" scoped>
.auto-tool-tip {
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}
</style>
