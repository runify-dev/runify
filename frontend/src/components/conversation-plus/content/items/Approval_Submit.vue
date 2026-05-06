<template>
  <div class="approval-wrapper">
    <div class="approval-card">
      <div v-if="content.content" class="as-content" :class="{ clamp: needClamp && !expanded }">
        {{ content.content }}
      </div>
      <button v-if="needClamp" class="as-toggle" @click="expanded = !expanded">
        {{ expanded ? '收起' : '展开' }}
      </button>
      <div class="as-action">
        <span class="as-icon" :class="isApprove ? 'approve' : 'reject'">
          <svg v-if="isApprove" width="14" height="14" viewBox="0 0 14 14" fill="none">
            <path d="M3 7.5l2.5 2.5L11 4" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
          <svg v-else width="14" height="14" viewBox="0 0 14 14" fill="none">
            <path d="M4 4l6 6M10 4l-6 6" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/>
          </svg>
        </span>
        <span class="as-text">{{ isApprove ? '通过' : '拒绝' }}</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'

const props = defineProps<{ content: any }>()
const isApprove = computed(() => props.content.result === 'approve')

const CLAMP_LINES = 3
const needClamp = computed(() => {
  const text = props.content.content || ''
  return text.length > CLAMP_LINES * 40 || text.split('\n').length > CLAMP_LINES
})

const expanded = ref(false)
</script>

<style scoped>
.approval-wrapper {
  display: flex;
  justify-content: flex-end;
  width: 100%;
  padding: 8px 0;
}

.approval-card {
  max-width: 420px;
  padding: 10px 14px;
  background: var(--ab);
  border-radius: 12px;
  border-bottom-right-radius: 3px;
  font-size: 13px;
  color: var(--t1);
}

.as-content {
  font-size: 13px;
  line-height: 1.6;
  color: var(--t2);
  margin-bottom: 8px;
  white-space: pre-wrap;
  word-break: break-word;
}

.as-content.clamp {
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.as-toggle {
  display: inline-block;
  border: none;
  background: none;
  padding: 0;
  margin-bottom: 8px;
  font-size: 12px;
  color: var(--t3);
  cursor: pointer;
  font-family: inherit;
}

.as-toggle:hover {
  color: var(--t1);
}

.as-action {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.as-icon {
  width: 20px;
  height: 20px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.as-icon.approve {
  background: rgba(34, 197, 94, 0.15);
  color: #22c55e;
}

.as-icon.reject {
  background: rgba(239, 68, 68, 0.15);
  color: #ef4444;
}

.as-text {
  font-weight: 500;
}
</style>
