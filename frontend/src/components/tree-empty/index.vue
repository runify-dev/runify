<template>
  <div class="tree-empty-state">
    <svg width="48" height="48" viewBox="0 0 48 48" fill="none" xmlns="http://www.w3.org/2000/svg">
      <rect
        x="6"
        y="8"
        width="14"
        height="10"
        rx="2"
        stroke="currentColor"
        stroke-width="1.5"
        fill="none"
      />
      <rect
        x="28"
        y="22"
        width="14"
        height="10"
        rx="2"
        stroke="currentColor"
        stroke-width="1.5"
        fill="none"
        stroke-dasharray="3 2"
      />
      <rect
        x="28"
        y="36"
        width="14"
        height="10"
        rx="2"
        stroke="currentColor"
        stroke-width="1.5"
        fill="none"
        stroke-dasharray="3 2"
      />
      <line x1="20" y1="13" x2="24" y2="13" stroke="currentColor" stroke-width="1.5" />
      <line
        x1="24"
        y1="13"
        x2="24"
        y2="41"
        stroke="currentColor"
        stroke-width="1.5"
        stroke-dasharray="3 2"
      />
      <line
        x1="24"
        y1="27"
        x2="28"
        y2="27"
        stroke="currentColor"
        stroke-width="1.5"
        stroke-dasharray="3 2"
      />
      <line
        x1="24"
        y1="41"
        x2="28"
        y2="41"
        stroke="currentColor"
        stroke-width="1.5"
        stroke-dasharray="3 2"
      />
    </svg>
    <p class="empty-title">{{ t('common.noData') }}</p>
    <p class="empty-desc">{{ t('common.treeEmptyDesc') }}</p>
  </div>
</template>
<script setup lang="ts">
import { t } from '@/locales'
</script>
<style lang="scss"></style>
