<template>
  <node-view-wrapper
    class="block relative my-6 rounded-2xl"
    :class="selected ? 'ring-2 ring-[--p-primary-500]' : ''"
    contenteditable="false"
  >
    <div
      class="flex items-center gap-5 px-5 py-4 rounded-2xl border backdrop-blur-md"
      :class="playing ? 'border-[--p-primary-600]' : 'border-[--p-surface-700]'"
      style="
        font-family: 'Bricolage Grotesque', sans-serif;
        background: color-mix(in srgb, var(--p-surface-900) 90%, transparent);
        box-shadow: 0 12px 40px color-mix(in srgb, var(--p-surface-950) 50%, transparent);
      "
    >
      <!-- ── Upload state ── -->
      <template v-if="uploadProgress !== null">
        <div class="relative w-14 h-14 shrink-0 flex items-center justify-center">
          <svg class="-rotate-90 absolute inset-0 w-full h-full" viewBox="0 0 56 56">
            <circle
              cx="28"
              cy="28"
              r="23"
              fill="none"
              stroke="var(--p-surface-700)"
              stroke-width="3"
            />
            <circle
              cx="28"
              cy="28"
              r="23"
              fill="none"
              stroke="var(--p-primary-color)"
              stroke-width="3"
              stroke-linecap="round"
              :stroke-dasharray="`${uploadProgress * 1.445} 999`"
              style="filter: drop-shadow(0 0 4px var(--p-primary-color))"
            />
          </svg>
          <span
            class="relative text-[11px] font-bold tabular-nums"
            style="color: var(--p-primary-color); font-family: 'DM Mono', monospace"
          >
            {{ uploadProgress }}<em class="not-italic text-[8px] opacity-60">%</em>
          </span>
        </div>

        <div class="flex flex-col gap-1 min-w-0">
          <span
            class="text-sm font-bold truncate max-w-[200px]"
            style="color: var(--p-text-color)"
            >{{ node.attrs.title || '上传中…' }}</span
          >
          <span
            class="text-[10px] tracking-widest uppercase"
            style="color: var(--p-text-muted-color); font-family: 'DM Mono', monospace"
            >正在处理音频</span
          >
        </div>

        <div class="flex items-end gap-[2.5px] h-7 ml-auto">
          <span
            v-for="i in 10"
            :key="i"
            class="block w-[2.5px] min-h-[3px] rounded-sm"
            style="
              background: var(--p-primary-color);
              animation: uploadBar 1.1s ease-in-out infinite alternate;
            "
            :style="{ animationDelay: `${i * 0.09}s` }"
          />
        </div>
      </template>

      <!-- ── Player state ── -->
      <template v-else-if="src">
        <audio
          ref="audioRef"
          :src="src"
          preload="metadata"
          @timeupdate="onTimeUpdate"
          @loadedmetadata="onMetadata"
          @ended="onEnded"
          @play="playing = true"
          @pause="playing = false"
          @waiting="buffering = true"
          @canplay="buffering = false"
          @error="hasError = true"
        />

        <!-- Cover -->
        <div class="relative shrink-0">
          <div
            class="w-[76px] h-[76px] rounded-full relative flex items-center justify-center overflow-hidden border transition-colors duration-300"
            :class="[
              playing ? 'border-[--p-primary-500] spinning' : 'border-[--p-surface-600]',
              'bg-[--p-surface-800]'
            ]"
          >
            <img
              v-if="nodeCover"
              :src="nodeCover"
              alt="cover"
              class="absolute inset-0 w-full h-full object-cover rounded-full"
            />
            <template v-else>
              <svg
                viewBox="0 0 40 40"
                fill="none"
                class="w-9 h-9 opacity-40"
                style="color: var(--p-primary-400)"
              >
                <circle
                  cx="20"
                  cy="20"
                  r="8"
                  stroke="currentColor"
                  stroke-width="1.2"
                  stroke-dasharray="3.5 2.5"
                />
                <circle cx="20" cy="20" r="2.5" fill="currentColor" />
                <path
                  d="M20 3A17 17 0 0 1 37 20"
                  stroke="currentColor"
                  stroke-width="1.2"
                  stroke-linecap="round"
                />
                <path
                  d="M20 3A17 17 0 0 0 3 20"
                  stroke="currentColor"
                  stroke-width="1.2"
                  stroke-linecap="round"
                  opacity="0.3"
                />
              </svg>
            </template>
            <div
              class="absolute inset-0 rounded-full pointer-events-none"
              style="
                background: repeating-radial-gradient(
                  circle at center,
                  transparent 0,
                  transparent 7px,
                  color-mix(in srgb, var(--p-surface-600) 25%, transparent) 7px,
                  color-mix(in srgb, var(--p-surface-600) 25%, transparent) 8px
                );
              "
            />
            <div
              class="absolute w-2.5 h-2.5 rounded-full z-10 border bg-[--p-surface-900] border-[--p-surface-600]"
            />
          </div>

          <!-- EQ dots -->
          <div
            class="absolute -bottom-2.5 left-1/2 -translate-x-1/2 flex gap-[3px] transition-opacity duration-300"
            :class="playing && !buffering ? 'opacity-100' : 'opacity-0'"
          >
            <span
              v-for="i in 4"
              :key="i"
              class="block w-1 h-1 rounded-full"
              style="
                background: var(--p-primary-color);
                animation: eqDot 0.7s ease-in-out infinite alternate;
              "
              :style="{ animationDelay: `${i * 0.13}s` }"
            />
          </div>
        </div>

        <!-- Body -->
        <div class="flex-1 min-w-0 flex flex-col gap-2.5">
          <!-- Meta -->
          <div class="flex flex-col gap-0.5">
            <span
              v-if="!editingTitle"
              class="text-[15px] font-extrabold leading-snug tracking-tight truncate transition-colors"
              :class="editor.isEditable ? 'cursor-text rounded px-1 -mx-1 hover:bg-white/5' : ''"
              style="color: var(--p-text-color)"
              @click="editor.isEditable && startEditTitle()"
            >
              {{ nodeTitle || '未知曲目' }}
            </span>
            <input
              v-else
              ref="titleInputRef"
              v-model="titleDraft"
              placeholder="曲目名称"
              class="text-sm font-bold rounded-md px-2 py-1 outline-none border w-full max-w-[260px] transition-shadow bg-[--p-surface-700] border-[--p-primary-600] text-[--p-text-color] focus:ring-2 focus:ring-[--p-primary-500]/25"
              @blur="commitTitle"
              @keydown.enter="commitTitle"
              @keydown.esc="cancelTitle"
              @click.stop
            />

            <span
              v-if="!editingArtist"
              class="text-[10px] tracking-[0.1em] uppercase truncate transition-colors"
              :class="
                editor.isEditable ? 'cursor-text rounded px-0.5 -mx-0.5 hover:bg-white/5' : ''
              "
              style="color: var(--p-primary-400); font-family: 'DM Mono', monospace"
              @click="editor.isEditable && startEditArtist()"
            >
              {{ nodeArtist || '未知艺术家' }}
            </span>
            <input
              v-else
              ref="artistInputRef"
              v-model="artistDraft"
              placeholder="艺术家"
              class="text-[10px] tracking-widest uppercase rounded-md px-2 py-1 outline-none border w-full max-w-[260px] transition-shadow bg-[--p-surface-700] border-[--p-primary-600] focus:ring-2 focus:ring-[--p-primary-500]/25"
              style="color: var(--p-primary-400); font-family: 'DM Mono', monospace"
              @blur="commitArtist"
              @keydown.enter="commitArtist"
              @keydown.esc="cancelArtist"
              @click.stop
            />
          </div>

          <!-- Waveform -->
          <div
            ref="progressRef"
            class="relative h-9 cursor-pointer flex items-end"
            @mousedown="onProgressMousedown"
            @mousemove="onProgressHover"
            @mouseleave="hoverTime = null"
          >
            <div class="flex items-end gap-[1.5px] w-full h-8">
              <span
                v-for="(h, i) in waveform"
                :key="i"
                class="flex-1 rounded-[1.5px] min-h-[2px] transition-colors duration-75"
                :style="{
                  height: h + '%',
                  background:
                    i / waveform.length < playedRatio
                      ? 'var(--p-primary-color)'
                      : 'var(--p-surface-600)',
                  opacity: i / waveform.length < playedRatio ? '0.9' : '0.4'
                }"
              />
            </div>
            <!-- Playhead -->
            <div
              class="absolute bottom-0 h-full w-[1.5px] rounded-sm pointer-events-none -translate-x-1/2 transition-[left] duration-100"
              style="background: var(--p-primary-color); box-shadow: 0 0 6px var(--p-primary-color)"
              :style="{ left: playedPercent + '%' }"
            >
              <div
                class="absolute -top-1 left-1/2 -translate-x-1/2 w-2.5 h-2.5 rounded-full"
                style="
                  background: var(--p-primary-color);
                  box-shadow: 0 0 8px var(--p-primary-color);
                "
              />
            </div>
            <!-- Hover tooltip -->
            <div
              v-if="hoverTime !== null"
              class="absolute -top-7 pointer-events-none text-[9px] px-2 py-0.5 rounded -translate-x-1/2 whitespace-nowrap border"
              style="
                background: var(--p-surface-700);
                color: var(--p-primary-300);
                border-color: var(--p-primary-700);
                font-family: 'DM Mono', monospace;
              "
              :style="{ left: hoverX + 'px' }"
            >
              {{ formatTime(hoverTime) }}
            </div>
          </div>

          <!-- Time -->
          <div class="flex justify-between -mt-1">
            <span
              class="text-[9px] tabular-nums"
              style="color: var(--p-primary-400); font-family: 'DM Mono', monospace"
              >{{ formatTime(currentTime) }}</span
            >
            <span
              class="text-[9px] tabular-nums"
              style="color: var(--p-text-muted-color); font-family: 'DM Mono', monospace"
              >{{ formatTime(duration) }}</span
            >
          </div>

          <!-- Controls -->
          <div class="flex items-center gap-1.5">
            <!-- Volume -->
            <div class="flex items-center gap-1.5 mr-1">
              <button
                @click="toggleMute"
                class="w-[30px] h-[30px] rounded-lg flex items-center justify-center border cursor-pointer transition-all duration-150 bg-[--p-surface-800] border-[--p-surface-600] hover:bg-[--p-surface-700] hover:border-[--p-primary-700]"
                style="color: var(--p-text-muted-color)"
              >
                <svg viewBox="0 0 20 20" fill="currentColor" class="w-3.5 h-3.5">
                  <path d="M10 3L5 7H2v6h3l5 4V3z" />
                  <path
                    v-if="muted || volume === 0"
                    d="M14 8l4 4m0-4l-4 4"
                    stroke="currentColor"
                    stroke-width="1.5"
                    stroke-linecap="round"
                    fill="none"
                  />
                  <path
                    v-else-if="volume > 0.5"
                    d="M14.5 5.5a6 6 0 010 9M12.5 7.5a3.5 3.5 0 010 5"
                    stroke="currentColor"
                    stroke-width="1.2"
                    fill="none"
                    stroke-linecap="round"
                  />
                  <path
                    v-else
                    d="M12.5 7.5a3.5 3.5 0 010 5"
                    stroke="currentColor"
                    stroke-width="1.2"
                    fill="none"
                    stroke-linecap="round"
                  />
                </svg>
              </button>
              <input
                type="range"
                min="0"
                max="1"
                step="0.02"
                :value="muted ? 0 : volume"
                @input="onVolumeInput"
                class="w-12 h-0.5 appearance-none rounded cursor-pointer outline-none"
                style="background: var(--p-surface-600); accent-color: var(--p-primary-color)"
              />
            </div>

            <!-- Skip back -->
            <button
              @click="skipBy(-10)"
              class="w-[30px] h-[30px] rounded-lg flex items-center justify-center border cursor-pointer transition-all duration-150 bg-[--p-surface-800] border-[--p-surface-600] hover:bg-[--p-surface-700] hover:border-[--p-primary-700]"
              style="color: var(--p-text-muted-color)"
            >
              <svg viewBox="0 0 20 20" fill="currentColor" class="w-3.5 h-3.5">
                <path d="M10 4V2L6 5l4 3V6a6 6 0 110 8.5l-1.4 1.4A8 8 0 1010 4z" />
                <text
                  x="7"
                  y="13.5"
                  font-size="5.5"
                  fill="currentColor"
                  font-family="monospace"
                  font-weight="bold"
                >
                  10
                </text>
              </svg>
            </button>

            <!-- Play / Pause -->
            <button
              @click="togglePlay"
              class="w-[42px] h-[42px] rounded-full flex items-center justify-center border cursor-pointer transition-all duration-200 mx-0.5"
              :style="{
                background: `color-mix(in srgb, var(--p-primary-500) 14%, var(--p-surface-800))`,
                borderColor: playing ? 'var(--p-primary-400)' : 'var(--p-primary-600)',
                color: 'var(--p-primary-300)',
                boxShadow: playing
                  ? '0 0 16px color-mix(in srgb, var(--p-primary-500) 28%, transparent)'
                  : 'none'
              }"
            >
              <svg v-if="buffering" class="w-4 h-4 animate-spin" viewBox="0 0 20 20" fill="none">
                <circle
                  cx="10"
                  cy="10"
                  r="7"
                  stroke="currentColor"
                  stroke-width="2"
                  stroke-dasharray="22 10"
                />
              </svg>
              <svg
                v-else-if="!playing"
                viewBox="0 0 20 20"
                fill="currentColor"
                class="w-4 h-4 translate-x-px"
              >
                <path d="M7 5l9 5-9 5V5z" />
              </svg>
              <svg v-else viewBox="0 0 20 20" fill="currentColor" class="w-4 h-4">
                <rect x="5" y="4" width="3.5" height="12" rx="1.2" />
                <rect x="11.5" y="4" width="3.5" height="12" rx="1.2" />
              </svg>
            </button>

            <!-- Skip forward -->
            <button
              @click="skipBy(10)"
              class="w-[30px] h-[30px] rounded-lg flex items-center justify-center border cursor-pointer transition-all duration-150 bg-[--p-surface-800] border-[--p-surface-600] hover:bg-[--p-surface-700] hover:border-[--p-primary-700]"
              style="color: var(--p-text-muted-color)"
            >
              <svg viewBox="0 0 20 20" fill="currentColor" class="w-3.5 h-3.5">
                <path d="M10 4V2l4 3-4 3V6a6 6 0 100 8.5l1.4 1.4A8 8 0 1110 4z" />
                <text
                  x="7"
                  y="13.5"
                  font-size="5.5"
                  fill="currentColor"
                  font-family="monospace"
                  font-weight="bold"
                >
                  10
                </text>
              </svg>
            </button>

            <!-- Rate -->
            <button
              @click="cycleRate"
              class="h-[30px] px-2.5 rounded-lg flex items-center justify-center border cursor-pointer transition-all duration-150 text-[10px] font-bold tracking-wide bg-[--p-surface-800] border-[--p-surface-600] hover:bg-[--p-surface-700] hover:border-[--p-primary-700]"
              style="color: var(--p-text-muted-color); font-family: 'DM Mono', monospace"
            >
              {{ playbackRate }}x
            </button>
          </div>
        </div>
      </template>
    </div>
  </node-view-wrapper>
</template>

<script setup lang="ts">
import { ref, computed, watch, nextTick, onMounted } from 'vue'
import { NodeViewWrapper } from '@tiptap/vue-3'
import type { NodeViewProps } from '@tiptap/vue-3'

const props = defineProps<NodeViewProps>()

const audioRef = ref<HTMLAudioElement | null>(null)
const progressRef = ref<HTMLElement | null>(null)
const titleInputRef = ref<HTMLInputElement | null>(null)
const artistInputRef = ref<HTMLInputElement | null>(null)

const playing = ref(false)
const buffering = ref(false)
const hasError = ref(false)
const currentTime = ref(0)
const duration = ref(0)
const volume = ref(0.8)
const muted = ref(false)
const hoverTime = ref<number | null>(null)
const hoverX = ref(0)
const editingTitle = ref(false)
const editingArtist = ref(false)
const titleDraft = ref('')
const artistDraft = ref('')
const playbackRate = ref(1)
const RATES = [0.5, 0.75, 1, 1.25, 1.5, 2]
const waveform = ref<number[]>([])

function generateWaveform(seed: string) {
  let h = 0
  for (let i = 0; i < seed.length; i++) h = (Math.imul(31, h) + seed.charCodeAt(i)) | 0
  const rng = () => {
    h ^= h << 13
    h ^= h >> 17
    h ^= h << 5
    return (h >>> 0) / 4294967296
  }
  return Array.from({ length: 56 }, () => 15 + rng() * 85)
}

const src = computed(() => props.node.attrs.src as string | null)
const nodeTitle = computed(() => props.node.attrs.title as string)
const nodeArtist = computed(() => props.node.attrs.artist as string)
const nodeCover = computed(() => props.node.attrs.cover as string)
const uploadProgress = computed(() => props.node.attrs.uploadProgress as number | null)
const playedPercent = computed(() =>
  duration.value ? (currentTime.value / duration.value) * 100 : 0
)
const playedRatio = computed(() => (duration.value ? currentTime.value / duration.value : 0))

watch(
  src,
  (val) => {
    playing.value = false
    currentTime.value = 0
    duration.value = 0
    if (val) waveform.value = generateWaveform(val)
  },
  { immediate: true }
)
onMounted(() => {
  if (src.value) waveform.value = generateWaveform(src.value)
})

function togglePlay() {
  const a = audioRef.value
  if (!a) return
  if (a.paused) {
    a.volume = volume.value
    a.muted = muted.value
    a.play()
  } else a.pause()
}
function skipBy(sec: number) {
  const a = audioRef.value
  if (!a) return
  a.currentTime = Math.max(0, Math.min(duration.value, a.currentTime + sec))
}
function onTimeUpdate() {
  if (audioRef.value) currentTime.value = audioRef.value.currentTime
}
function onMetadata() {
  if (audioRef.value) duration.value = audioRef.value.duration
}
function onEnded() {
  playing.value = false
  currentTime.value = 0
}
function toggleMute() {
  const a = audioRef.value
  if (!a) return
  muted.value = !muted.value
  a.muted = muted.value
}
function onVolumeInput(e: Event) {
  const val = parseFloat((e.target as HTMLInputElement).value)
  volume.value = val
  if (audioRef.value) {
    audioRef.value.volume = val
    audioRef.value.muted = val === 0
    muted.value = val === 0
  }
}
function seekTo(clientX: number) {
  const bar = progressRef.value,
    a = audioRef.value
  if (!bar || !a || !duration.value) return
  const rect = bar.getBoundingClientRect()
  a.currentTime = Math.max(0, Math.min(1, (clientX - rect.left) / rect.width)) * duration.value
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
  hoverTime.value = Math.max(0, Math.min(1, (e.clientX - rect.left) / rect.width)) * duration.value
  hoverX.value = e.clientX - rect.left
}
function cycleRate() {
  const idx = RATES.indexOf(playbackRate.value)
  playbackRate.value = RATES[(idx + 1) % RATES.length]
  if (audioRef.value) audioRef.value.playbackRate = playbackRate.value
}
function startEditTitle() {
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
function startEditArtist() {
  artistDraft.value = nodeArtist.value
  editingArtist.value = true
  nextTick(() => artistInputRef.value?.focus())
}
function commitArtist() {
  props.updateAttributes({ artist: artistDraft.value.trim() })
  editingArtist.value = false
}
function cancelArtist() {
  editingArtist.value = false
}
function formatTime(s: number): string {
  if (!s || isNaN(s)) return '0:00'
  return `${Math.floor(s / 60)}:${Math.floor(s % 60)
    .toString()
    .padStart(2, '0')}`
}
</script>

<style>
@import url('https://fonts.googleapis.com/css2?family=DM+Mono:wght@400;500&family=Bricolage+Grotesque:wght@400;600;800&display=swap');

/* Only keyframes — all layout/color is Tailwind */
.spinning {
  animation: vinylSpin 4s linear infinite;
}
@keyframes vinylSpin {
  to {
    transform: rotate(360deg);
  }
}
@keyframes uploadBar {
  from {
    height: 3px;
    opacity: 0.35;
  }
  to {
    height: 22px;
    opacity: 1;
  }
}
@keyframes eqDot {
  from {
    transform: scale(0.4);
    opacity: 0.4;
  }
  to {
    transform: scale(1.6);
    opacity: 1;
  }
}
</style>
