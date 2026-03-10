<template>
  <div v-if="visible" :class="overlayClass" :style="overlayStyle">
    <div class="loading-container">
      <ProgressSpinner :style="{ width: size, height: size }" />
      <div v-if="text" class="loading-text">{{ text }}</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, defineProps } from 'vue'
import ProgressSpinner from 'primevue/progressspinner'

export interface LoadingOverlayProps {
  visible?: boolean
  text?: string
  fullscreen?: boolean
  size?: string
  zIndex?: number
  background?: string
}

const props = withDefaults(defineProps<LoadingOverlayProps>(), {
  visible: false,
  text: '',
  fullscreen: false,
  size: '50px',
  zIndex: 2000,
  background: 'rgba(0, 0, 0, 0.5)'
})

const overlayClass = computed(() => ({
  'v-loading-overlay-fullscreen': props.fullscreen,
  'v-loading-overlay-relative': !props.fullscreen
}))

const overlayStyle = computed(() => ({
  zIndex: props.zIndex,
  background: props.background
}))
</script>

<style scoped>
.v-loading-overlay-fullscreen {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100dvh;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-direction: column;
  gap: 8px;
}

.v-loading-overlay-relative {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-direction: column;
  gap: 8px;
}

.loading-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.loading-text {
  color: white;
  font-size: 14px;
  font-weight: 500;
}

:deep(.p-progress-spinner-circle) {
  stroke: var(--primary-color) !important;
  stroke-width: 3;
}
</style>
