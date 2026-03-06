<template>
  <node-view-wrapper class="video-block-wrapper" :class="{ selected }" contenteditable="false">
    <div class="video-player-shell" @click="onShellClick">
      <div class="video-ratio-box">
        <!-- Upload progress overlay -->
        <div v-if="uploadProgress !== null" class="video-upload-overlay">
          <div class="video-upload-icon">
            <svg viewBox="0 0 48 48" fill="none">
              <circle cx="24" cy="24" r="20" stroke="rgba(255,255,255,0.15)" stroke-width="4" />
              <circle
                cx="24"
                cy="24"
                r="20"
                stroke="#7db3e8"
                stroke-width="4"
                stroke-linecap="round"
                :stroke-dasharray="`${uploadProgress * 1.257} 999`"
                transform="rotate(-90 24 24)"
                style="transition: stroke-dasharray 0.3s ease"
              />
            </svg>
            <span class="video-upload-percent">{{ uploadProgress }}%</span>
          </div>
          <p class="video-upload-label">{{ node.attrs.title || '上传中…' }}</p>
        </div>

        <video
          v-else-if="src"
          ref="videoRef"
          class="video-el"
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
        <div v-if="buffering && uploadProgress === null" class="video-spinner-overlay">
          <div class="video-spinner"></div>
        </div>

        <!-- Big play button -->
        <transition name="fade">
          <div
            v-if="!playing && currentTime === 0 && !buffering && src && uploadProgress === null"
            class="video-big-play"
            @click.stop="togglePlay"
          >
            <svg viewBox="0 0 64 64" fill="none">
              <circle cx="32" cy="32" r="32" fill="rgba(0,0,0,0.55)" />
              <path d="M26 20l20 12-20 12V20z" fill="white" />
            </svg>
          </div>
        </transition>
      </div>

      <!-- Controls bar (hidden during upload) -->
      <div v-if="uploadProgress === null && src" class="video-controls" @click.stop>
        <!-- Progress bar -->
        <div
          class="video-progress"
          ref="progressRef"
          @mousedown="onProgressMousedown"
          @mousemove="onProgressHover"
          @mouseleave="hoverTime = null"
        >
          <div class="video-progress-bg">
            <div class="video-progress-buffer" :style="{ width: bufferedPercent + '%' }"></div>
            <div class="video-progress-played" :style="{ width: playedPercent + '%' }">
              <div class="video-progress-thumb"></div>
            </div>
          </div>
          <div
            v-if="hoverTime !== null"
            class="video-time-tooltip"
            :style="{ left: hoverX + 'px' }"
          >
            {{ formatTime(hoverTime) }}
          </div>
        </div>

        <!-- Bottom row -->
        <div class="video-controls-row">
          <!-- Play / Pause -->
          <button class="video-ctrl-btn" @click="togglePlay" :title="playing ? '暂停' : '播放'">
            <svg v-if="!playing" viewBox="0 0 20 20" fill="currentColor">
              <path d="M6 4l10 6-10 6V4z" />
            </svg>
            <svg v-else viewBox="0 0 20 20" fill="currentColor">
              <rect x="5" y="4" width="3.5" height="12" rx="1" />
              <rect x="11.5" y="4" width="3.5" height="12" rx="1" />
            </svg>
          </button>

          <!-- Volume -->
          <div class="video-volume-wrap">
            <button class="video-ctrl-btn" @click="toggleMute" :title="muted ? '取消静音' : '静音'">
              <svg v-if="!muted && volume > 0.5" viewBox="0 0 20 20" fill="currentColor">
                <path d="M10 3L5 7H2v6h3l5 4V3z" fill="currentColor" />
                <path
                  d="M14.5 5.5a6 6 0 010 9M12.5 7.5a3.5 3.5 0 010 5"
                  stroke="currentColor"
                  stroke-width="1.2"
                  fill="none"
                  stroke-linecap="round"
                />
              </svg>
              <svg v-else-if="!muted && volume > 0" viewBox="0 0 20 20" fill="currentColor">
                <path d="M10 3L5 7H2v6h3l5 4V3z" />
                <path
                  d="M12.5 7.5a3.5 3.5 0 010 5"
                  stroke="currentColor"
                  stroke-width="1.2"
                  fill="none"
                  stroke-linecap="round"
                />
              </svg>
              <svg v-else viewBox="0 0 20 20" fill="currentColor">
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
              class="video-volume-slider"
              type="range"
              min="0"
              max="1"
              step="0.02"
              :value="muted ? 0 : volume"
              @input="onVolumeInput"
            />
          </div>

          <!-- Time -->
          <span class="video-time">{{ formatTime(currentTime) }} / {{ formatTime(duration) }}</span>

          <div style="flex: 1"></div>

          <!-- Title -->
          <div class="video-title-wrap">
            <span
              v-if="!editingTitle"
              class="video-title"
              @click.stop="startEditTitle"
              :title="nodeTitle || '点击添加标题'"
              >{{ nodeTitle || '添加标题' }}</span
            >
            <input
              v-else
              ref="titleInputRef"
              class="video-title-input"
              v-model="titleDraft"
              @blur="commitTitle"
              @keydown.enter="commitTitle"
              @keydown.esc="cancelTitle"
              @click.stop
            />
          </div>

          <!-- Playback rate -->
          <div class="video-rate-wrap">
            <button class="video-ctrl-btn video-rate-btn" @click.stop="cycleRate">
              {{ playbackRate }}x
            </button>
          </div>

          <!-- Fullscreen -->
          <button class="video-ctrl-btn" @click="toggleFullscreen" title="全屏">
            <svg
              viewBox="0 0 20 20"
              fill="none"
              stroke="currentColor"
              stroke-width="1.6"
              stroke-linecap="round"
            >
              <path d="M3 7V3h4M13 3h4v4M17 13v4h-4M7 17H3v-4" />
            </svg>
          </button>

          <!-- Replace video (only in edit mode) -->
          <button
            v-if="editor.isEditable"
            class="video-ctrl-btn video-replace-btn"
            @click.stop="replaceVideo"
            title="替换视频"
          >
            <svg
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

      <!-- Upload placeholder controls bar -->
      <div
        v-else-if="uploadProgress !== null"
        class="video-controls video-controls-uploading"
        @click.stop
      >
        <div class="video-controls-row">
          <span class="video-time" style="opacity: 0.5">上传中…</span>
          <div style="flex: 1"></div>
        </div>
      </div>
    </div>
  </node-view-wrapper>
</template>

<script setup lang="ts">
import { ref, computed, watch, nextTick } from 'vue'
import { NodeViewWrapper } from '@tiptap/vue-3'
import type { NodeViewProps } from '@tiptap/vue-3'

const props = defineProps<NodeViewProps>()

// ── Refs ──
const videoRef = ref<HTMLVideoElement | null>(null)
const progressRef = ref<HTMLElement | null>(null)
const titleInputRef = ref<HTMLInputElement | null>(null)

// ── State ──
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

// ── Attrs ──
const src = computed(() => props.node.attrs.src as string | null)
const nodeTitle = computed(() => props.node.attrs.title as string)
const uploadProgress = computed(() => props.node.attrs.uploadProgress as number | null)

// ── Derived ──
const playedPercent = computed(() =>
  duration.value ? (currentTime.value / duration.value) * 100 : 0
)

// Reset player state when src changes (e.g. after upload completes)
watch(src, () => {
  playing.value = false
  currentTime.value = 0
  duration.value = 0
  bufferedPercent.value = 0
})

// ── Playback ──
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

// ── Volume ──
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

// ── Progress ──
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

// ── Playback rate ──
function cycleRate() {
  const idx = RATES.indexOf(playbackRate.value)
  playbackRate.value = RATES[(idx + 1) % RATES.length]
  if (videoRef.value) videoRef.value.playbackRate = playbackRate.value
}

// ── Title ──
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

// ── Replace ──
function replaceVideo() {
  props.updateAttributes({ src: null, currentTime: 0, uploadProgress: null })
}

// ── Fullscreen ──
function toggleFullscreen() {
  const v = videoRef.value
  if (!v) return
  if (document.fullscreenElement) document.exitFullscreen()
  else v.requestFullscreen()
}

// ── Shell click ──
function onShellClick() {}

// ── Helpers ──
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
.video-block-wrapper {
  display: block;
  margin: 20px 0;
  border-radius: 12px;
  overflow: hidden;
  border: 2px solid transparent;
  transition:
    border-color 0.2s,
    box-shadow 0.2s;
  background: #0d0d0d;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.12);
}
.video-block-wrapper.selected {
  border-color: #7db3e8;
  box-shadow: 0 0 0 3px rgba(125, 179, 232, 0.25);
}

/* ── Player ── */
.video-player-shell {
  position: relative;
  background: #0d0d0d;
}

.video-ratio-box {
  position: relative;
  aspect-ratio: 16 / 9;
  background: #000;
  cursor: pointer;
}
.video-el {
  width: 100%;
  height: 100%;
  object-fit: contain;
  display: block;
}

/* ── Upload overlay ── */
.video-upload-overlay {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.75);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 14px;
  z-index: 10;
}

.video-upload-icon {
  position: relative;
  width: 72px;
  height: 72px;
  display: flex;
  align-items: center;
  justify-content: center;
}
.video-upload-icon svg {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
}
.video-upload-percent {
  position: relative;
  font-size: 14px;
  font-weight: 600;
  color: #fff;
  font-family: 'JetBrains Mono', monospace;
  letter-spacing: -0.5px;
}
.video-upload-label {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.55);
  margin: 0;
  max-width: 220px;
  text-align: center;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* Spinner */
.video-spinner-overlay {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.3);
}
.video-spinner {
  width: 36px;
  height: 36px;
  border: 3px solid rgba(255, 255, 255, 0.2);
  border-top-color: #fff;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}
@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

/* Big play button */
.video-big-play {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  pointer-events: all;
}
.video-big-play svg {
  width: 72px;
  height: 72px;
  transition: transform 0.15s;
}
.video-big-play:hover svg {
  transform: scale(1.08);
}

/* Fade transition */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

/* ── Controls ── */
.video-controls {
  background: linear-gradient(to top, rgba(0, 0, 0, 0.85) 0%, rgba(0, 0, 0, 0.4) 100%);
  padding: 6px 10px 8px;
}
.video-controls-uploading {
  background: rgba(0, 0, 0, 0.6);
  padding: 6px 10px 8px;
}

/* Progress */
.video-progress {
  position: relative;
  height: 18px;
  display: flex;
  align-items: center;
  cursor: pointer;
  margin-bottom: 2px;
}
.video-progress-bg {
  position: relative;
  width: 100%;
  height: 4px;
  border-radius: 2px;
  background: rgba(255, 255, 255, 0.2);
  overflow: visible;
  transition: height 0.15s;
}
.video-progress:hover .video-progress-bg {
  height: 6px;
}
.video-progress-buffer {
  position: absolute;
  top: 0;
  left: 0;
  height: 100%;
  border-radius: 2px;
  background: rgba(255, 255, 255, 0.3);
  transition: width 0.3s;
}
.video-progress-played {
  position: absolute;
  top: 0;
  left: 0;
  height: 100%;
  border-radius: 2px;
  background: #7db3e8;
  display: flex;
  align-items: center;
}
.video-progress-thumb {
  position: absolute;
  right: -5px;
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background: #fff;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.4);
  opacity: 0;
  transition: opacity 0.15s;
}
.video-progress:hover .video-progress-thumb {
  opacity: 1;
}

/* Time tooltip */
.video-time-tooltip {
  position: absolute;
  top: -28px;
  transform: translateX(-50%);
  background: rgba(0, 0, 0, 0.8);
  color: #fff;
  font-size: 11px;
  padding: 2px 6px;
  border-radius: 4px;
  white-space: nowrap;
  pointer-events: none;
}

/* Controls row */
.video-controls-row {
  display: flex;
  align-items: center;
  gap: 4px;
}
.video-ctrl-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border: none;
  background: transparent;
  color: rgba(255, 255, 255, 0.85);
  cursor: pointer;
  border-radius: 6px;
  flex-shrink: 0;
  transition:
    background 0.12s,
    color 0.12s;
}
.video-ctrl-btn:hover {
  background: rgba(255, 255, 255, 0.12);
  color: #fff;
}
.video-ctrl-btn svg {
  width: 16px;
  height: 16px;
}

/* Volume */
.video-volume-wrap {
  display: flex;
  align-items: center;
  gap: 2px;
}
.video-volume-slider {
  width: 60px;
  height: 4px;
  appearance: none;
  background: rgba(255, 255, 255, 0.25);
  border-radius: 2px;
  outline: none;
  cursor: pointer;
}
.video-volume-slider::-webkit-slider-thumb {
  appearance: none;
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: #fff;
  cursor: pointer;
}

/* Time */
.video-time {
  font-size: 11.5px;
  color: rgba(255, 255, 255, 0.75);
  font-family: 'JetBrains Mono', monospace;
  white-space: nowrap;
  padding: 0 4px;
}

/* Title */
.video-title-wrap {
  max-width: 180px;
  overflow: hidden;
}
.video-title {
  font-size: 11.5px;
  color: rgba(255, 255, 255, 0.55);
  cursor: text;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  display: block;
  padding: 0 6px;
  border-radius: 4px;
  transition:
    background 0.12s,
    color 0.12s;
}
.video-title:hover {
  background: rgba(255, 255, 255, 0.08);
  color: rgba(255, 255, 255, 0.8);
}
.video-title-input {
  font-size: 11.5px;
  background: rgba(255, 255, 255, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.3);
  border-radius: 4px;
  color: #fff;
  padding: 2px 6px;
  outline: none;
  width: 140px;
  font-family: inherit;
}

/* Rate */
.video-rate-btn {
  width: auto !important;
  padding: 0 6px;
  font-size: 11.5px;
  font-family: 'JetBrains Mono', monospace;
  font-weight: 600;
  letter-spacing: -0.3px;
}

/* Replace btn */
.video-replace-btn {
  opacity: 0.6;
}
.video-replace-btn:hover {
  opacity: 1;
}
</style>
