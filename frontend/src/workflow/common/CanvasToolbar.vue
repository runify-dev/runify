<template>
  <div class="canvas-toolbar">
    <button class="toolbar-btn" title="放大" @click="$emit('zoom-in')">
      <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/><line x1="11" y1="8" x2="11" y2="14"/><line x1="8" y1="11" x2="14" y2="11"/></svg>
    </button>
    <div class="toolbar-zoom">{{ zoomPercent }}%</div>
    <button v-if="showZoomOut" class="toolbar-btn" title="缩小" @click="$emit('zoom-out')">
      <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/><line x1="8" y1="11" x2="14" y2="11"/></svg>
    </button>
    <div class="toolbar-divider"></div>
    <button class="toolbar-btn" title="适应屏幕" @click="$emit('fit-view')">
      <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M8 3H5a2 2 0 0 0-2 2v3m18 0V5a2 2 0 0 0-2-2h-3m0 18h3a2 2 0 0 0 2-2v-3M3 16v3a2 2 0 0 0 2 2h3"/></svg>
    </button>
    <button class="toolbar-btn" title="自动布局" @click="$emit('auto-layout')">
      <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="3" width="7" height="7" rx="1"/><rect x="14" y="3" width="7" height="7" rx="1"/><rect x="3" y="14" width="7" height="7" rx="1"/><rect x="14" y="14" width="7" height="7" rx="1"/></svg>
    </button>
    <div class="toolbar-divider"></div>
    <button
      class="toolbar-btn"
      :class="{ 'toolbar-btn-active': mode === 'drag' }"
      title="拖拽模式"
      @click="$emit('set-mode', 'drag')"
    >
      <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M5 9l4-4 4 4"/><path d="M5 15l4 4 4-4"/><path d="M15 5l4 4-4 4"/><path d="M15 15l4-4-4-4"/></svg>
    </button>
    <button
      class="toolbar-btn"
      :class="{ 'toolbar-btn-active': mode === 'select' }"
      title="框选模式（空格）"
      @click="$emit('set-mode', 'select')"
    >
      <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="3" width="18" height="18" rx="2" ry="2" stroke-dasharray="4 2"/></svg>
    </button>
    <div class="toolbar-divider"></div>
    <button
      class="toolbar-btn"
      :title="fullscreen ? '退出全屏' : '全屏'"
      @click="$emit('toggle-fullscreen')"
    >
      <svg v-if="!fullscreen" xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="15 3 21 3 21 9"/><polyline points="9 21 3 21 3 15"/><line x1="21" y1="3" x2="14" y2="10"/><line x1="3" y1="21" x2="10" y2="14"/></svg>
      <svg v-else xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="4 14 10 14 10 20"/><polyline points="20 10 14 10 14 4"/><line x1="14" y1="10" x2="21" y2="3"/><line x1="3" y1="21" x2="10" y2="14"/></svg>
    </button>
  </div>
</template>
<script setup lang="ts">
withDefaults(defineProps<{
  zoomPercent: number
  mode: 'drag' | 'select'
  showZoomOut?: boolean
  fullscreen?: boolean
}>(), {
  showZoomOut: true,
  fullscreen: false
})

defineEmits<{
  'zoom-in': []
  'zoom-out': []
  'fit-view': []
  'auto-layout': []
  'set-mode': [mode: 'drag' | 'select']
  'toggle-fullscreen': []
}>()
</script>
<style scoped>
.canvas-toolbar {
  display: flex;
  align-items: center;
  gap: 2px;
  background: var(--p-content-background);
  border: 1px solid var(--p-content-border-color);
  border-radius: 8px;
  padding: 4px;
  box-shadow: var(--p-shadow-1);
  z-index: 10;
  user-select: none;
}
.toolbar-btn {
  width: 32px;
  height: 32px;
  border: none;
  border-radius: 6px;
  background: transparent;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--p-text-muted-color);
  transition: background 0.15s, color 0.15s;
}
.toolbar-btn:hover {
  background: var(--p-content-background);
  color: var(--p-primary-color);
}
.toolbar-btn-active {
  background: var(--p-content-background);
  color: var(--p-primary-color);
}
.toolbar-zoom {
  min-width: 40px;
  text-align: center;
  font-size: 12px;
  color: var(--p-text-muted-color);
  font-weight: 500;
}
.toolbar-divider {
  width: 1px;
  height: 20px;
  background: var(--p-content-border-color);
  margin: 0 2px;
}
</style>
