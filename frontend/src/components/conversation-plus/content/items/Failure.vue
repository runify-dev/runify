<template>
  <div class="failure-item">
    <span class="fail-icon">
      <svg width="16" height="16" viewBox="0 0 16 16" fill="none">
        <circle cx="8" cy="8" r="6.5" stroke="currentColor" stroke-width="1.4"/>
        <path d="M8 5v3.5M8 10.5h.006" stroke="currentColor" stroke-width="1.4" stroke-linecap="round"/>
      </svg>
    </span>
    <span class="fail-text">{{ content.content }}</span>
  </div>
</template>

<script setup lang="ts">
defineProps<{ content: any }>()
</script>

<style scoped>
.failure-item {
  display: inline-flex;
  align-items: flex-start;
  gap: 6px;
  padding: 8px 12px;
  border-radius: 8px;
  background: var(--danger-bg);
  border: 1px solid var(--danger-text);
  max-width: 100%;
}

.fail-icon {
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: var(--danger-bg);
  color: var(--danger-text);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.fail-text {
  font-size: 13px;
  line-height: 1.6;
  color: var(--danger-text);
  white-space: pre-wrap;
  word-break: break-word;
}
</style>
