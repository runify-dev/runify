<template>
  <div class="layout-main-container">
    <div class="layout-main">
      <AppSidebar> <slot name="menu"></slot></AppSidebar>
      <div class="w-full flex-1 min-h-0">
        <slot></slot>
      </div>
    </div>
  </div>
</template>
<script setup lang="ts">
import AppSidebar from '@/layout-plus/app-sidebar/index.vue'
</script>
<style lang="scss" scoped>
.layout-main-container {
  display: flex;
  flex-direction: column;
  min-height: 100dvh;
  padding-top: 4rem;
  transition: margin-left var(--layout-section-transition-duration);
  background-color: var(--surface-ground);
}

.layout-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  background-color: var(--app-content-bg);
  border-radius: var(--content-border-radius);
  padding: 1.5rem;
  height: calc(100dvh - 4rem);
  overflow: auto;
}

@media (min-width: 992px) {
  .layout-main-container {
    padding-top: 6rem;
    padding-bottom: 2rem;
  }
  .layout-main {
    height: calc(100dvh - 8rem);
  }
}
</style>
