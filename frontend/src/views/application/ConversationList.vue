<template>
  <!-- 左侧抽屉 -->
  <div
    ref="drawer"
    @touchstart="handleTouchStart"
    @touchmove="handleTouchMove"
    @touchend="handleTouchEnd"
  >
    <!-- 抽屉内容 -->
    <div class="p-4">
      <h2 class="text-lg font-bold mb-4">抽屉菜单</h2>
      <ul>
        <li class="py-2 px-4 hover:bg-gray-200 rounded cursor-pointer">菜单项 1</li>
        <li class="py-2 px-4 hover:bg-gray-200 rounded cursor-pointer">菜单项 2</li>
        <li class="py-2 px-4 hover:bg-gray-200 rounded cursor-pointer">菜单项 3</li>
        <li class="py-2 px-4 hover:bg-gray-200 rounded cursor-pointer">菜单项 4</li>
      </ul>
    </div>
  </div>
</template>
<script setup lang="ts">
import { computed } from 'vue'
const props = defineProps<{
  drawerOpen: boolean
  handleTouchStart: any
  handleTouchMove: any
  handleTouchEnd: any
}>()

const emit = defineEmits(['update:drawerOpen'])
const _drawerOpen = computed({
  get: () => {
    return props.drawerOpen
  },
  set: (e) => {
    emit('update:drawerOpen', e)
  }
})
const open = () => {
  _drawerOpen.value = true
}

const close = () => {
  _drawerOpen.value = false
}
defineExpose({ open, close })
</script>
<style lang="scss"></style>
