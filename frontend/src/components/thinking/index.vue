<template>
  <div class="thinking">
    <!-- Header -->
    <button type="button" class="thinking-header" @click="isExpanded = !isExpanded">
      <div class="thinking-header-left">
        <span class="thinking-icon" :class="{ 'is-loading': loading }">
          <Loading v-if="loading" :size="16" />
          <svg v-else viewBox="0 0 14 14" fill="none" class="thinking-icon-svg">
            <path
              d="M7 2C5.5 2 4 3 4 4.5c0 .8.3 1.4.8 1.9C4 6.8 3.5 7.7 3.5 8.5 3.5 10.4 5 12 7 12s3.5-1.6 3.5-3.5c0-.8-.5-1.7-1.3-2.1.5-.5.8-1.1.8-1.9C10 3 8.5 2 7 2z"
              stroke="currentColor"
              stroke-width="1.2"
              stroke-linejoin="round"
            />
            <path
              d="M7 6v2M6 9h2"
              stroke="currentColor"
              stroke-width="1.2"
              stroke-linecap="round"
            />
          </svg>
        </span>

        <span class="thinking-title">
          {{ loading ? 'Thinking...' : 'Thought process' }}
        </span>

        <span v-if="!loading && tokenCount" class="thinking-tokens">
          {{ tokenCount }} tokens
        </span>
      </div>

      <svg
        viewBox="0 0 12 12"
        fill="none"
        class="thinking-arrow"
        :class="{ 'is-expanded': isExpanded }"
      >
        <path
          d="M2.5 4.5L6 8l3.5-3.5"
          stroke="currentColor"
          stroke-width="1.4"
          stroke-linecap="round"
          stroke-linejoin="round"
        />
      </svg>
    </button>

    <!-- Body -->
    <Transition
      enter-active-class="transition-all duration-200 ease-out overflow-hidden"
      leave-active-class="transition-all duration-150 ease-in overflow-hidden"
      enter-from-class="opacity-0 max-h-0!"
      enter-to-class="opacity-100 max-h-[800px]!"
      leave-from-class="opacity-100 max-h-[800px]!"
      leave-to-class="opacity-0 max-h-0!"
    >
      <div v-if="isExpanded" class="thinking-body">
        <!-- skeleton: loading but no text yet -->
        <div v-if="loading && !thinking" class="thinking-skeleton">
          <div class="skeleton-line" style="width: 82%" />
          <div class="skeleton-line" style="width: 65%" />
          <div class="skeleton-line" style="width: 74%" />
          <div class="skeleton-line" style="width: 50%" />
        </div>

        <!-- text: streaming or done -->
        <div v-else class="thinking-content">
          <p class="thinking-text">
            {{ thinking }}
            <span v-if="loading" class="thinking-cursor" />
          </p>
          <button
            v-if="!loading && thinking"
            class="copy-btn"
            :class="{ 'is-copied': copied }"
            @click="copyThinking"
          >
            <svg v-if="copied" viewBox="0 0 14 14" fill="none" class="copy-icon">
              <path
                d="M2 7l3.5 3.5 6.5-7"
                stroke="currentColor"
                stroke-width="1.6"
                stroke-linecap="round"
                stroke-linejoin="round"
              />
            </svg>
            <svg v-else viewBox="0 0 14 14" fill="none" class="copy-icon">
              <rect
                x="4"
                y="4"
                width="8"
                height="8"
                rx="1.5"
                stroke="currentColor"
                stroke-width="1.3"
              />
              <path
                d="M2 10V2h8"
                stroke="currentColor"
                stroke-width="1.3"
                stroke-linecap="round"
                stroke-linejoin="round"
              />
            </svg>
          </button>
        </div>
      </div>
    </Transition>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import Loading from '@/components/conversation-plus/loading/index.vue'

const props = defineProps<{
  thinking?: string
  loading?: boolean
  tokenCount?: number
}>()

const isExpanded = ref(!!props.loading)
const copied = ref(false)

async function copyThinking() {
  if (!props.thinking) return
  await navigator.clipboard.writeText(props.thinking).catch(() => {})
  copied.value = true
  setTimeout(() => {
    copied.value = false
  }, 2000)
}
</script>

<style scoped>
.thinking {
  border-radius: 8px;
  border: 1px solid var(--bd);
  background: var(--bg);
  overflow: hidden;
  width: 100%;
  font-size: 14px;
}

/* ── Header ──────────────────────────────────────────────────────────── */
.thinking-header {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding: 8px 12px;
  border: none;
  background: transparent;
  cursor: pointer;
  text-align: left;
  font-family: inherit;
  transition: background-color 0.15s;
}

.thinking-header:hover {
  background: var(--hv);
}

.thinking-header-left {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

/* ── Icon ────────────────────────────────────────────────────────────── */
.thinking-icon {
  width: 20px;
  height: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  color: var(--t2);
}

.thinking-icon-svg {
  width: 14px;
  height: 14px;
}

/* ── Title ───────────────────────────────────────────────────────────── */
.thinking-title {
  font-size: 12.5px;
  font-weight: 500;
  color: var(--t1);
}

.thinking-tokens {
  font-size: 11px;
  color: var(--t3);
}

/* ── Arrow ───────────────────────────────────────────────────────────── */
.thinking-arrow {
  width: 12px;
  height: 12px;
  color: var(--t3);
  transition: transform 0.15s;
  flex-shrink: 0;
}

.thinking-arrow.is-expanded {
  transform: rotate(180deg);
}

/* ── Body ────────────────────────────────────────────────────────────── */
.thinking-body {
  border-top: 1px solid var(--bd);
  padding: 8px 12px 12px;
}

/* ── Skeleton ────────────────────────────────────────────────────────── */
.thinking-skeleton {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 4px 0;
}

.skeleton-line {
  height: 8px;
  border-radius: 4px;
  background: var(--ac);
  animation: pulse 2s cubic-bezier(0.4, 0, 0.6, 1) infinite;
}

@keyframes pulse {
  0%, 100% {
    opacity: 0.4;
  }
  50% {
    opacity: 0.7;
  }
}

/* ── Content ─────────────────────────────────────────────────────────── */
.thinking-content {
  position: relative;
}

.thinking-content:hover .copy-btn {
  opacity: 1;
}

.thinking-text {
  font-size: 12.5px;
  line-height: 1.6;
  color: var(--t2);
  white-space: pre-wrap;
  padding-right: 28px;
  margin: 0;
}

/* ── Cursor ──────────────────────────────────────────────────────────── */
.thinking-cursor {
  display: inline-block;
  width: 2px;
  height: 13px;
  background: var(--t3);
  vertical-align: middle;
  margin-left: 2px;
  animation: blink 0.9s step-start infinite;
}

@keyframes blink {
  0%, 100% {
    opacity: 1;
  }
  50% {
    opacity: 0;
  }
}

/* ── Copy Button ─────────────────────────────────────────────────────── */
.copy-btn {
  position: absolute;
  top: 0;
  right: 0;
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: none;
  border-radius: 4px;
  background: transparent;
  color: var(--t3);
  cursor: pointer;
  opacity: 0;
  transition: opacity 0.15s, background-color 0.15s, color 0.15s;
}

.copy-btn:hover {
  background: var(--hv);
  color: var(--t2);
}

.copy-btn.is-copied {
  color: var(--t2);
  opacity: 1;
}

.copy-icon {
  width: 14px;
  height: 14px;
}
</style>
