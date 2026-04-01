<template>
  <div class="rounded-lg border-[0.5px] border-slate-200 bg-white text-sm overflow-hidden w-full">
    <!-- Header -->
    <button
      type="button"
      class="w-full flex items-center justify-between gap-2 px-3 py-2 hover:bg-slate-50/70 transition-colors text-left cursor-pointer"
      @click="isExpanded = !isExpanded"
    >
      <div class="flex items-center gap-2 min-w-0">
        <span
          class="shrink-0 size-5 rounded-md flex items-center justify-center"
          :class="loading ? 'bg-slate-100 text-slate-400' : 'bg-violet-50 text-violet-500'"
        >
          <span v-if="loading" class="flex items-center gap-[2px]">
            <span
              class="size-1 rounded-full bg-slate-400 animate-bounce"
              style="animation-delay: 0ms"
            ></span>
            <span
              class="size-1 rounded-full bg-slate-400 animate-bounce"
              style="animation-delay: 150ms"
            ></span>
            <span
              class="size-1 rounded-full bg-slate-400 animate-bounce"
              style="animation-delay: 300ms"
            ></span>
          </span>
          <svg v-else viewBox="0 0 14 14" fill="none" class="size-3.5">
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

        <span class="text-[12.5px] font-medium text-slate-600">
          {{ loading ? 'Thinking...' : 'Thought process' }}
        </span>

        <span v-if="!loading && tokenCount" class="text-[11px] text-slate-300">
          {{ tokenCount }} tokens
        </span>
      </div>

      <svg
        viewBox="0 0 12 12"
        fill="none"
        class="size-3 text-slate-400 transition-transform duration-150 shrink-0"
        :class="{ 'rotate-180': isExpanded }"
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
      <div v-if="isExpanded" class="border-t border-slate-100 px-3 pt-2 pb-3">
        <!-- skeleton: loading but no text yet -->
        <div v-if="loading && !thinking" class="flex flex-col gap-2 py-1">
          <div class="h-2 rounded-full bg-slate-100 animate-pulse" style="width: 82%"></div>
          <div class="h-2 rounded-full bg-slate-100 animate-pulse" style="width: 65%"></div>
          <div class="h-2 rounded-full bg-slate-100 animate-pulse" style="width: 74%"></div>
          <div class="h-2 rounded-full bg-slate-100 animate-pulse" style="width: 50%"></div>
        </div>

        <!-- text: streaming or done -->
        <div v-else class="relative group">
          <p class="text-[12.5px] leading-relaxed text-slate-500 whitespace-pre-wrap pr-7">
            {{ thinking }}
            <span
              v-if="loading"
              class="inline-block w-[2px] h-[13px] bg-slate-400 align-middle ml-0.5 cursor-blink"
            />
          </p>
          <button
            v-if="!loading && thinking"
            class="absolute top-0 right-0 size-6 flex items-center justify-center rounded transition-colors cursor-pointer opacity-0 group-hover:opacity-100"
            :class="
              copied
                ? 'text-emerald-500 bg-emerald-50'
                : 'text-slate-400 hover:text-slate-600 hover:bg-slate-100'
            "
            @click="copyThinking"
          >
            <svg v-if="copied" viewBox="0 0 14 14" fill="none" class="size-3.5">
              <path
                d="M2 7l3.5 3.5 6.5-7"
                stroke="currentColor"
                stroke-width="1.6"
                stroke-linecap="round"
                stroke-linejoin="round"
              />
            </svg>
            <svg v-else viewBox="0 0 14 14" fill="none" class="size-3.5">
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

const props = defineProps<{
  thinking?: string
  loading?: boolean
  tokenCount?: number
  defaultExpanded?: boolean
}>()

const isExpanded = ref(props.defaultExpanded ?? false)
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
@keyframes blink {
  0%,
  100% {
    opacity: 1;
  }
  50% {
    opacity: 0;
  }
}
.cursor-blink {
  animation: blink 0.9s step-start infinite;
}
</style>
