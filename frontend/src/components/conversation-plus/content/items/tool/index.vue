<template>
  <div class="tool-call">
    <button class="tc-head" @click="$emit('toggle')">
      <svg class="tc-chevron" :class="{ open: expanded }" viewBox="0 0 16 16" fill="none">
        <path d="M6 4l4 4-4 4" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round"/>
      </svg>
      <span class="tc-icon" :class="{ spin: loading }">
        <Loading v-if="loading" :size="14" />
        <svg v-else viewBox="0 0 16 16" fill="none">
          <path d="M11.5 2.5a1 1 0 0 1 1.4 0l.6.6a1 1 0 0 1 0 1.4l-7.1 7.1-2-2L11.5 2.5ZM4.2 10.2l2 2L3 15l-.7-.7 1.9-4.1Z" fill="currentColor"/>
        </svg>
      </span>
      <span class="tc-name">{{ content.toolName || 'Tool' }}</span>
    </button>

    <Transition
      enter-active-class="tc-expand-enter"
      leave-active-class="tc-expand-leave"
    >
      <div v-if="expanded" class="tc-body">
        <div v-if="argsDisplay" class="tc-block">
          <div class="tc-block-label">input</div>
          <pre class="tc-block-code">{{ argsDisplay }}</pre>
        </div>
        <div v-if="content.content" class="tc-block">
          <div class="tc-block-label">output</div>
          <pre class="tc-block-code">{{ content.content }}</pre>
        </div>
        <div v-if="loading && !content.content && !argsDisplay" class="tc-loading-dots">
          <span></span><span></span><span></span>
        </div>
      </div>
    </Transition>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import Loading from '@/components/conversation-plus/loading/index.vue'

const props = defineProps<{ content: any; loading: boolean; expanded: boolean }>()
defineEmits<{ toggle: [] }>()

const argsDisplay = computed(() => {
  const raw = props.content.functionArguments
  if (!raw) return ''
  try {
    return JSON.stringify(JSON.parse(raw), null, 2)
  } catch {
    return raw
  }
})
</script>

<style scoped>
.tool-call { width: 100%; }

.tc-head {
  display: inline-flex; align-items: center; gap: 6px;
  padding: 3px 0; border: none; background: none;
  cursor: pointer; font-family: inherit; color: var(--t3);
  transition: color var(--p-transition-duration); max-width: 100%; overflow: hidden;
}
.tc-head:hover { color: var(--t1); }

.tc-chevron { width: 14px; height: 14px; flex-shrink: 0; transition: transform var(--p-transition-duration); }
.tc-chevron.open { transform: rotate(90deg); }

.tc-icon { width: 16px; height: 16px; display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.tc-icon.spin { color: var(--t2); }

.tc-name {
  font-size: 13px; font-weight: 500;
  font-family: 'JetBrains Mono NL', monospace;
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
  min-width: 0; max-width: 400px;
}

.tc-expand-enter { animation: tc-open 0.2s ease-out; overflow: hidden; }
.tc-expand-leave { animation: tc-close 0.15s ease-in forwards; overflow: hidden; }
@keyframes tc-open { from { opacity: 0; max-height: 0; } to { opacity: 1; max-height: 600px; } }
@keyframes tc-close { from { opacity: 1; max-height: 600px; } to { opacity: 0; max-height: 0; } }

.tc-body { padding: 6px 0 4px 8px; display: flex; flex-direction: column; gap: 6px; }

.tc-block { display: flex; flex-direction: column; gap: 2px; }
.tc-block-label { font-size: 11px; font-weight: 500; color: var(--t3); padding-left: 2px; }
.tc-block-code {
  margin: 0; padding: 8px 10px; background: var(--hv); border-radius: 6px;
  font-size: 12px; line-height: 1.5; color: var(--t1);
  font-family: 'JetBrains Mono NL', monospace;
  white-space: pre-wrap; word-break: break-all; max-height: 240px; overflow-y: auto;
}

.tc-loading-dots { display: flex; gap: 4px; padding: 4px 2px; }
.tc-loading-dots span {
  width: 5px; height: 5px; border-radius: 50%; background: var(--t3);
  animation: dot-pulse 1.2s ease-in-out infinite;
}
.tc-loading-dots span:nth-child(2) { animation-delay: 0.2s; }
.tc-loading-dots span:nth-child(3) { animation-delay: 0.4s; }
@keyframes dot-pulse {
  0%, 80%, 100% { opacity: 0.3; transform: scale(0.8); }
  40% { opacity: 1; transform: scale(1); }
}
</style>
