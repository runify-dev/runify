<template>
  <node-view-wrapper
    class="block my-5 rounded-xl overflow-hidden border-2 border-transparent transition-all duration-200 bg-surface-950 shadow-md"
    :class="{
      'border-primary shadow-[0_0_0_3px_var(--p-primary-color)]/25':
        selected
    }"
    contenteditable="false"
  >
    <div class="relative bg-surface-950" @click="onShellClick">
      <!-- 16:9 ratio box -->
      <div class="relative aspect-video bg-black cursor-pointer">
        <!-- Upload progress overlay -->
        <div
          v-if="uploadProgress !== null"
          class="absolute inset-0 bg-black/75 flex flex-col items-center justify-center gap-3.5 z-10"
        >
          <div class="relative w-18 h-18 flex items-center justify-center">
            <svg class="absolute inset-0 w-full h-full" viewBox="0 0 48 48" fill="none">
              <circle cx="24" cy="24" r="20" stroke="rgba(255,255,255,0.15)" stroke-width="4" />
              <circle
                cx="24"
                cy="24"
                r="20"
                stroke="var(--p-primary-color, #7db3e8)"
                stroke-width="4"
                stroke-linecap="round"
                :stroke-dasharray="`${uploadProgress * 1.257} 999`"
                transform="rotate(-90 24 24)"
                style="transition: stroke-dasharray 0.3s ease"
              />
            </svg>
            <span class="relative text-sm font-semibold text-white font-mono tracking-tight">
              {{ uploadProgress }}%
            </span>
          </div>
          <p class="text-xs text-white/55 m-0 max-w-[220px] text-center truncate">
            {{ node.attrs.title || '上传中…' }}
          </p>
        </div>

        <video
          v-else-if="src"
          ref="videoRef"
          class="w-full h-full object-contain block"
          :src="src"
          :poster="node.attrs.poster || undefined"
          preload="metadata"
          @timeupdate="onTimeUpdate"
          @loadedmetadata="onMetadata"
          @ended="onEnded"
          @play="playing = true"
          @pause="playing = false"
          @waiting="buffering = true"
          @canplay="buffering = false"
          @click.stop="togglePlay"
        />

        <!-- Buffering spinner -->
        <div
          v-if="buffering && uploadProgress === null"
          class="absolute inset-0 flex items-center justify-center bg-black/30"
        >
          <div
            class="w-9 h-9 border-3 border-white/20 border-t-white rounded-full animate-spin"
          ></div>
        </div>

        <!-- Big play button -->
        <transition name="fade">
          <div
            v-if="!playing && currentTime === 0 && !buffering && src && uploadProgress === null"
            class="absolute inset-0 flex items-center justify-center"
            @click.stop="togglePlay"
          >
            <svg
              class="w-18 h-18 transition-transform duration-150 hover:scale-[1.08]"
              viewBox="0 0 64 64"
              fill="none"
            >
              <circle cx="32" cy="32" r="32" fill="rgba(0,0,0,0.55)" />
              <path d="M26 20l20 12-20 12V20z" fill="white" />
            </svg>
          </div>
        </transition>
      </div>

      <!-- Controls bar -->
      <div
        v-if="uploadProgress === null && src"
        class="bg-gradient-to-t from-black/85 to-black/40 px-2.5 pt-1.5 pb-2"
        @click.stop
      >
        <!-- Progress bar -->
        <div
          class="relative h-[18px] flex items-center cursor-pointer mb-0.5 group"
          ref="progressRef"
          @mousedown="onProgressMousedown"
          @mousemove="onProgressHover"
          @mouseleave="hoverTime = null"
        >
          <div
            class="relative w-full h-1 group-hover:h-1.5 rounded-sm bg-white/20 overflow-visible transition-[height] duration-150"
          >
            <div
              class="absolute top-0 left-0 h-full rounded-sm bg-white/30 transition-[width] duration-300"
              :style="{ width: bufferedPercent + '%' }"
            ></div>
            <div
              class="absolute top-0 left-0 h-full rounded-sm flex items-center"
              :style="{ width: playedPercent + '%', background: 'var(--p-primary-color)' }"
            >
              <div
                class="absolute -right-[5px] w-3 h-3 rounded-full bg-white shadow-[0_1px_4px_rgba(0,0,0,0.4)] opacity-0 group-hover:opacity-100 transition-opacity duration-150"
              ></div>
            </div>
          </div>
          <!-- Time tooltip -->
          <div
            v-if="hoverTime !== null"
            class="absolute -top-7 -translate-x-1/2 bg-black/80 text-white text-[11px] px-1.5 py-0.5 rounded pointer-events-none whitespace-nowrap"
            :style="{ left: hoverX + 'px' }"
          >
            {{ formatTime(hoverTime) }}
          </div>
        </div>

        <!-- Bottom row -->
        <div class="flex items-center gap-1">
          <!-- Play / Pause -->
          <button
            class="inline-flex items-center justify-center w-7 h-7 border-0 bg-transparent text-white/85 cursor-pointer rounded-md flex-shrink-0 transition-all duration-100 hover:bg-white/12 hover:text-white"
            @click="togglePlay"
            :title="playing ? '暂停' : '播放'"
          >
            <svg v-if="!playing" class="w-4 h-4" viewBox="0 0 20 20" fill="currentColor">
              <path d="M6 4l10 6-10 6V4z" />
            </svg>
            <svg v-else class="w-4 h-4" viewBox="0 0 20 20" fill="currentColor">
              <rect x="5" y="4" width="3.5" height="12" rx="1" />
              <rect x="11.5" y="4" width="3.5" height="12" rx="1" />
            </svg>
          </button>

          <!-- Volume -->
          <div class="flex items-center gap-0.5">
            <button
              class="inline-flex items-center justify-center w-7 h-7 border-0 bg-transparent text-white/85 cursor-pointer rounded-md flex-shrink-0 transition-all duration-100 hover:bg-white/12 hover:text-white"
              @click="toggleMute"
              :title="muted ? '取消静音' : '静音'"
            >
              <svg
                v-if="!muted && volume > 0.5"
                class="w-4 h-4"
                viewBox="0 0 20 20"
                fill="currentColor"
              >
                <path d="M10 3L5 7H2v6h3l5 4V3z" fill="currentColor" />
                <path
                  d="M14.5 5.5a6 6 0 010 9M12.5 7.5a3.5 3.5 0 010 5"
                  stroke="currentColor"
                  stroke-width="1.2"
                  fill="none"
                  stroke-linecap="round"
                />
              </svg>
              <svg
                v-else-if="!muted && volume > 0"
                class="w-4 h-4"
                viewBox="0 0 20 20"
                fill="currentColor"
              >
                <path d="M10 3L5 7H2v6h3l5 4V3z" />
                <path
                  d="M12.5 7.5a3.5 3.5 0 010 5"
                  stroke="currentColor"
                  stroke-width="1.2"
                  fill="none"
                  stroke-linecap="round"
                />
              </svg>
              <svg v-else class="w-4 h-4" viewBox="0 0 20 20" fill="currentColor">
                <path d="M10 3L5 7H2v6h3l5 4V3z" />
                <path
                  d="M14 8l4 4m0-4l-4 4"
                  stroke="currentColor"
                  stroke-width="1.4"
                  stroke-linecap="round"
                  fill="none"
                />
              </svg>
            </button>
            <input
              class="w-15 h-1 appearance-none bg-white/25 rounded-sm outline-none cursor-pointer [&::-webkit-slider-thumb]:appearance-none [&::-webkit-slider-thumb]:w-2.5 [&::-webkit-slider-thumb]:h-2.5 [&::-webkit-slider-thumb]:rounded-full [&::-webkit-slider-thumb]:bg-white [&::-webkit-slider-thumb]:cursor-pointer"
              type="range"
              min="0"
              max="1"
              step="0.02"
              :value="muted ? 0 : volume"
              @input="onVolumeInput"
            />
          </div>

          <!-- Time -->
          <span class="text-[11.5px] text-white/75 font-mono whitespace-nowrap px-1">
            {{ formatTime(currentTime) }} / {{ formatTime(duration) }}
          </span>

          <div class="flex-1"></div>

          <!-- Title -->
          <div class="max-w-[180px] overflow-hidden">
            <span
              v-if="!editingTitle"
              class="text-[11.5px] text-white/55 cursor-text whitespace-nowrap overflow-hidden text-ellipsis block px-1.5 py-0.5 rounded transition-all duration-100 hover:bg-white/8 hover:text-white/80"
              @click.stop="startEditTitle"
              :title="nodeTitle || '点击添加标题'"
            >
              {{ nodeTitle || '添加标题' }}
            </span>
            <input
              v-else
              ref="titleInputRef"
              class="text-[11.5px] bg-white/10 border border-white/30 rounded text-white px-1.5 py-0.5 outline-none w-35 font-inherit"
              v-model="titleDraft"
              @blur="commitTitle"
              @keydown.enter="commitTitle"
              @keydown.esc="cancelTitle"
              @click.stop
            />
          </div>

          <!-- Playback rate -->
          <button
            class="inline-flex items-center justify-center h-7 px-1.5 border-0 bg-transparent text-white/85 cursor-pointer rounded-md flex-shrink-0 transition-all duration-100 hover:bg-white/12 hover:text-white text-[11.5px] font-mono font-semibold tracking-tight"
            @click.stop="cycleRate"
          >
            {{ playbackRate }}x
          </button>

          <!-- Fullscreen -->
          <button
            class="inline-flex items-center justify-center w-7 h-7 border-0 bg-transparent text-white/85 cursor-pointer rounded-md flex-shrink-0 transition-all duration-100 hover:bg-white/12 hover:text-white"
            @click="toggleFullscreen"
            title="全屏"
          >
            <svg
              class="w-4 h-4"
              viewBox="0 0 20 20"
              fill="none"
              stroke="currentColor"
              stroke-width="1.6"
              stroke-linecap="round"
            >
              <path d="M3 7V3h4M13 3h4v4M17 13v4h-4M7 17H3v-4" />
            </svg>
          </button>

          <!-- Replace video -->
          <button
            v-if="editor.isEditable"
            class="inline-flex items-center justify-center w-7 h-7 border-0 bg-transparent text-white/85 cursor-pointer rounded-md flex-shrink-0 transition-all duration-100 hover:bg-white/12 hover:text-white opacity-60 hover:opacity-100"
            @click.stop="replaceVideo"
            title="替换视频"
          >
            <svg
              class="w-4 h-4"
              viewBox="0 0 20 20"
              fill="none"
              stroke="currentColor"
              stroke-width="1.6"
              stroke-linecap="round"
              stroke-linejoin="round"
            >
              <path d="M4 4h5M4 4v5" />
              <path d="M16 16h-5M16 16v-5" />
              <path d="M4 4a8 8 0 0112.5 1.5M16 16A8 8 0 013.5 14.5" />
            </svg>
          </button>
        </div>
      </div>

      <!-- Upload placeholder controls -->
      <div v-else-if="uploadProgress !== null" class="bg-black/60 px-2.5 pt-1.5 pb-2" @click.stop>
        <div class="flex items-center gap-1">
          <span class="text-[11.5px] text-white/75 font-mono whitespace-nowrap px-1 opacity-50"
            >上传中…</span
          >
          <div class="flex-1"></div>
        </div>
      </div>
    </div>

    <!-- Fade transition styles (需保留少量 CSS) -->
  </node-view-wrapper>
</template>

<script setup lang="ts">
import { ref, computed, watch, nextTick } from 'vue'
import { NodeViewWrapper } from '@tiptap/vue-3'
import type { NodeViewProps } from '@tiptap/vue-3'

const props = defineProps<NodeViewProps>()

const videoRef = ref<HTMLVideoElement | null>(null)
const progressRef = ref<HTMLElement | null>(null)
const titleInputRef = ref<HTMLInputElement | null>(null)

const playing = ref(false)
const buffering = ref(false)
const currentTime = ref(0)
const duration = ref(0)
const bufferedPercent = ref(0)
const volume = ref(1)
const muted = ref(false)
const hoverTime = ref<number | null>(null)
const hoverX = ref(0)
const editingTitle = ref(false)
const titleDraft = ref('')
const playbackRate = ref(1)
const RATES = [0.5, 0.75, 1, 1.25, 1.5, 2]

const src = computed(() => props.node.attrs.src as string | null)
const nodeTitle = computed(() => props.node.attrs.title as string)
const uploadProgress = computed(() => props.node.attrs.uploadProgress as number | null)

const playedPercent = computed(() =>
  duration.value ? (currentTime.value / duration.value) * 100 : 0
)

watch(src, () => {
  playing.value = false
  currentTime.value = 0
  duration.value = 0
  bufferedPercent.value = 0
})

function togglePlay() {
  const v = videoRef.value
  if (!v) return
  if (v.paused) v.play()
  else v.pause()
}

function onTimeUpdate() {
  const v = videoRef.value
  if (!v) return
  currentTime.value = v.currentTime
  if (v.buffered.length) {
    bufferedPercent.value = (v.buffered.end(v.buffered.length - 1) / v.duration) * 100
  }
}

function onMetadata() {
  const v = videoRef.value
  if (!v) return
  duration.value = v.duration
}

function onEnded() {
  playing.value = false
}

function toggleMute() {
  const v = videoRef.value
  if (!v) return
  muted.value = !muted.value
  v.muted = muted.value
}

function onVolumeInput(e: Event) {
  const val = parseFloat((e.target as HTMLInputElement).value)
  volume.value = val
  if (videoRef.value) {
    videoRef.value.volume = val
    videoRef.value.muted = val === 0
    muted.value = val === 0
  }
}

function seekTo(clientX: number) {
  const bar = progressRef.value
  const v = videoRef.value
  if (!bar || !v || !duration.value) return
  const rect = bar.getBoundingClientRect()
  const ratio = Math.max(0, Math.min(1, (clientX - rect.left) / rect.width))
  v.currentTime = ratio * duration.value
}

function onProgressMousedown(e: MouseEvent) {
  seekTo(e.clientX)
  const onMove = (ev: MouseEvent) => seekTo(ev.clientX)
  const onUp = () => {
    window.removeEventListener('mousemove', onMove)
    window.removeEventListener('mouseup', onUp)
  }
  window.addEventListener('mousemove', onMove)
  window.addEventListener('mouseup', onUp)
}

function onProgressHover(e: MouseEvent) {
  const bar = progressRef.value
  if (!bar || !duration.value) return
  const rect = bar.getBoundingClientRect()
  const ratio = Math.max(0, Math.min(1, (e.clientX - rect.left) / rect.width))
  hoverTime.value = ratio * duration.value
  hoverX.value = e.clientX - rect.left
}

function cycleRate() {
  const idx = RATES.indexOf(playbackRate.value)
  playbackRate.value = RATES[(idx + 1) % RATES.length]
  if (videoRef.value) videoRef.value.playbackRate = playbackRate.value
}

function startEditTitle() {
  if (!props.editor.isEditable) return
  titleDraft.value = nodeTitle.value
  editingTitle.value = true
  nextTick(() => titleInputRef.value?.focus())
}

function commitTitle() {
  props.updateAttributes({ title: titleDraft.value.trim() })
  editingTitle.value = false
}

function cancelTitle() {
  editingTitle.value = false
}

function replaceVideo() {
  props.updateAttributes({ src: null, currentTime: 0, uploadProgress: null })
}

function toggleFullscreen() {
  const v = videoRef.value
  if (!v) return
  if (document.fullscreenElement) document.exitFullscreen()
  else v.requestFullscreen()
}

function onShellClick() {}

function formatTime(s: number): string {
  if (!s || isNaN(s)) return '0:00'
  const m = Math.floor(s / 60)
  const sec = Math.floor(s % 60)
    .toString()
    .padStart(2, '0')
  return `${m}:${sec}`
}
</script>

<style scoped>
/* fade transition — Tailwind 无法覆盖 Vue transition hooks */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
