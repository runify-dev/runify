<template>
  <div class="layout-main-container">
    <div class="layout-main">
      <slot></slot>
    </div>
  </div>
</template>
<script setup lang="ts"></script>
<style lang="scss" scoped>
.layout-main-container {
  display: flex;
  flex-direction: column;
  min-height: 100dvh;
  justify-content: space-between;
  padding: 6rem 2rem 0 2rem;
  margin-left: 0 !important;
  transition: margin-left var(--layout-section-transition-duration);
}

.layout-main {
  flex: 1 1 auto;
  padding-bottom: 2rem;
}
</style>
