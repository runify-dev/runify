<template>
  <div class="tool-call">
    <button class="tc-head" @click="$emit('toggle')">
      <svg class="tc-chevron" :class="{ open: expanded }" viewBox="0 0 16 16" fill="none">
        <path d="M6 4l4 4-4 4" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round"/>
      </svg>
      <span class="tc-icon" :class="{ spin: loading }">
        <Loading v-if="loading" :size="14" />
        <svg v-else viewBox="0 0 16 16" width="16" height="16" fill="none">
          <circle cx="7" cy="7" r="4.5" stroke="currentColor" stroke-width="1.3"/>
          <path d="M10.5 10.5L14 14" stroke="currentColor" stroke-width="1.3" stroke-linecap="round"/>
        </svg>
      </span>
      <span class="tc-name">{{ pattern || 'grep' }}</span>
      <span class="tc-lines" v-if="summary">{{ summary }}</span>
    </button>

    <Transition enter-active-class="tc-expand-enter" leave-active-class="tc-expand-leave">
      <div v-if="expanded && content.content" class="tc-body">
        <div class="rf-block">
          <pre class="rf-content">{{ content.content }}</pre>
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

const args = computed(() => {
  const raw = props.content.functionArguments
  if (!raw) return {} as any
  try { return JSON.parse(raw) } catch { return {} as any }
})

const pattern = computed(() => args.value.pattern || '')
const summary = computed(() => props.content.summary || '')
</script>

<style scoped>
.tool-call { width: 100%; }

.tc-head {
  display: inline-flex; align-items: center; gap: 6px;
  padding: 3px 0; border: none; background: none;
  cursor: pointer; font-family: inherit; color: var(--t3);
  transition: color var(--p-transition-duration);
  max-width: 100%; overflow: hidden;
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
}

.tc-lines {
  font-size: 11px; color: var(--t3);
  font-family: 'JetBrains Mono NL', monospace;
  flex-shrink: 0;
}

.tc-expand-enter { animation: tc-open 0.2s ease-out; overflow: hidden; }
.tc-expand-leave { animation: tc-close 0.15s ease-in forwards; overflow: hidden; }
@keyframes tc-open { from { opacity: 0; max-height: 0; } to { opacity: 1; max-height: 600px; } }
@keyframes tc-close { from { opacity: 1; max-height: 600px; } to { opacity: 0; max-height: 0; } }

.tc-body { padding: 4px 0; }

.rf-block {
  background: var(--bg2);
  border: 1px solid var(--bd);
  border-radius: 6px;
  padding: 8px 12px;
  max-height: 360px;
  overflow-y: auto;
}

.rf-content {
  margin: 0;
  font-family: 'JetBrains Mono NL', monospace;
  font-size: 11px;
  line-height: 1.5;
  color: var(--t1);
  white-space: pre-wrap;
  word-break: break-all;
}
</style>
