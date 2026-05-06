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
  padding: 4rem 0 0 0;
  margin-left: 0 !important;
  transition: margin-left var(--layout-section-transition-duration);
  background-color: var(--surface-ground);
}

.layout-main {
  flex: 1;
  background-color: var(--app-content-bg);
  border-radius: var(--content-border-radius);
  padding: 1.5rem;
}

@media (min-width: 992px) {
  .layout-main-container {
    padding: 6rem 2rem 2rem 2rem;
  }
}
</style>
