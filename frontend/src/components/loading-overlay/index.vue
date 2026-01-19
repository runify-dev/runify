<template>
  <div
    v-if="visible"
    class="loading-overlay"
    :class="{
      'fixed-overlay': fullscreen,
      'relative-overlay': !fullscreen
    }"
    :style="{ zIndex }"
  >
    <div class="loading-container">
      <ProgressSpinner
        :style="{
          width: size,
          height: size
        }"
      />
      <div v-if="text" class="loading-text">
        {{ text }}
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import ProgressSpinner from 'primevue/progressspinner'

defineProps({
  visible: {
    type: Boolean,
    default: false
  },
  text: {
    type: String,
    default: ''
  },
  fullscreen: {
    type: Boolean,
    default: false
  },
  size: {
    type: String,
    default: '50px'
  },
  zIndex: {
    type: Number,
    default: 1000
  },
  background: {
    type: String,
    default: 'rgba(255, 255, 255, 0.8)'
  }
})
</script>

<style scoped>
/* 全屏遮罩 */
.fixed-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  background: var(--p-mask-background, rgba(0, 0, 0, 0.4));
}

/* 相对遮罩 */
.relative-overlay {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: var(--p-mask-background, rgba(0, 0, 0, 0.4));
  border-radius: inherit;
}

.loading-container {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 1rem;
}

.loading-text {
  color: var(--primary-color);
  font-size: 0.875rem;
  font-weight: 500;
}

/* 自定义进度条颜色 */
:deep(.p-progress-spinner-circle) {
  stroke: var(--primary-color) !important;
  stroke-width: 3;
}
</style>
