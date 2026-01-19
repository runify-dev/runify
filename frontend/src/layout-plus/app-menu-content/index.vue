<template>
  <div class="layout-main-container">
    <div class="layout-main">
      <AppSidebar> <slot name="menu"></slot></AppSidebar>
      <div class="w-full overflow-hidden">
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
  min-height: 100vh;
  justify-content: space-between;
  padding: 6rem 2rem 0 2rem;
  transition: margin-left var(--layout-section-transition-duration);
}

.layout-main {
  flex: 1 1 auto;
  padding-bottom: 2rem;
}
</style>
