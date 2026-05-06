<template>
  <div ref="rootRef">
    <!-- Trigger: tiptap toolbar style, not a PrimeVue Button -->
    <button
      type="button"
      data-style="ghost"
      class="tiptap-button"
      :class="{ 'is-active': popoverVisible }"
      title="插入音频"
      @click="togglePopover"
    >
      <span class="pi pi-volume-up" />
    </button>

    <!-- PrimeVue Popover (OverlayPanel v4) -->
    <Popover ref="popoverRef" @show="popoverVisible = true" @hide="onPopoverHide">
      <div class="flex w-72 flex-col overflow-hidden rounded-xl">
        <!-- Header -->
        <div class="flex items-center gap-2 border-b border-white/6 px-4 py-2.5">
          <span class="h-1.5 w-1.5 rounded-full bg-cyan-500" style="box-shadow: 0 0 6px var(--p-cyan-500)" />
          <span class="font-mono text-[10px] uppercase tracking-widest text-white/35">
            插入音频
          </span>
        </div>

        <!-- PrimeVue Tabs -->
        <Tabs v-model:value="activeTab">
          <TabList class="border-b border-white/6 !bg-transparent px-0">
            <Tab
              v-for="tab in tabs"
              :key="tab.id"
              :value="tab.id"
              class="flex flex-1 items-center justify-center gap-1.5 !py-2.5 text-xs font-medium"
            >
              <component :is="tab.icon" class="h-3.5 w-3.5" />
              {{ tab.label }}
            </Tab>
          </TabList>

          <!-- ── Upload ── -->
          <TabPanel value="upload" class="p-4">
            <label
              class="flex cursor-pointer flex-col items-center gap-3 rounded-lg border border-dashed border-white/12 px-4 py-5 transition-all duration-150 hover:border-cyan-500/30 hover:bg-cyan-500/4"
              :class="dragOver ? '!border-cyan-500/50 !bg-cyan-500/6' : ''"
              @dragover.prevent="dragOver = true"
              @dragleave="dragOver = false"
              @drop.prevent="onDrop"
            >
              <div
                class="flex h-10 w-10 items-center justify-center rounded-full border border-white/10 bg-white/5"
              >
                <svg
                  class="h-5 w-5 text-white/40"
                  viewBox="0 0 20 20"
                  fill="none"
                  stroke="currentColor"
                  stroke-width="1.5"
                  stroke-linecap="round"
                >
                  <path d="M10 13V4M7 7l3-3 3 3M3 16h14" />
                </svg>
              </div>
              <div class="text-center">
                <div class="text-xs font-medium">点击选择或拖拽音频文件</div>
                <div class="mt-0.5 font-mono text-[10px]">MP3 · WAV · OGG · FLAC</div>
              </div>
              <input
                ref="fileInputRef"
                type="file"
                accept="audio/*"
                multiple
                class="hidden"
                @change="onFileChange"
              />
            </label>
          </TabPanel>

          <!-- ── URL ── -->
          <TabPanel value="url" class="p-4">
            <div class="flex flex-col gap-2.5">
              <InputText
                v-model="urlDraft"
                placeholder="https://example.com/audio.mp3"
                size="small"
                fluid
                @keydown.enter="insertFromUrl"
                @keydown.esc="closePopover"
              />
              <button
                type="button"
                class="tiptap-button w-full justify-center !h-8 text-xs"
                :disabled="!urlDraft"
                @click="insertFromUrl"
              >
                插入链接
              </button>
            </div>
          </TabPanel>

          <!-- ── Record ── -->
          <TabPanel value="record" class="p-4">
            <!-- Mic denied -->
            <div
              v-if="micDenied"
              class="flex flex-col items-center gap-2 rounded-lg border border-red-500/20 bg-red-500/6 px-4 py-4 text-center"
            >
              <i class="pi pi-microphone-slash text-xl text-red-400" />
              <p class="text-xs text-red-400/80">麦克风权限被拒绝，请在浏览器设置中开启</p>
            </div>

            <template v-else>
              <!-- Idle -->
              <div v-if="recordState === 'idle'" class="flex flex-col items-center gap-3 py-2">
                <button
                  type="button"
                  class="tiptap-button !h-14 !w-14 !rounded-full flex items-center justify-center"
                  title="开始录音"
                  @click="startRecording"
                >
                  <i class="pi pi-microphone text-xl" />
                </button>
                <p class="text-xs text-white/30">点击开始录音</p>
              </div>

              <!-- Recording -->
              <div
                v-else-if="recordState === 'recording'"
                class="flex flex-col items-center gap-3 py-2"
              >
                <!-- Live waveform -->
                <div class="flex h-10 w-full items-end justify-center gap-0.5 px-2">
                  <span
                    v-for="(h, i) in waveformBars"
                    :key="i"
                    class="w-1 rounded-sm transition-all duration-75"
                    :style="{
                      height: `${h}%`,
                      background: 'linear-gradient(to top,var(--p-cyan-500),var(--p-cyan-300))'
                    }"
                  />
                </div>
                <!-- Timer -->
                <span
                  class="font-mono text-xl font-bold tabular-nums text-white/85"
                  style="text-shadow: 0 0 20px var(--p-cyan-500)"
                >
                  {{ formatDuration(recordedSeconds) }}
                </span>
                <!-- Live indicator -->
                <div class="flex items-center gap-1.5">
                  <span
                    class="h-2 w-2 animate-pulse rounded-full bg-red-400"
                    style="box-shadow: 0 0 8px var(--p-red-400)"
                  />
                  <span class="font-mono text-[10px] uppercase tracking-widest text-red-400/70">
                    录音中
                  </span>
                </div>
                <!-- Stop -->
                <button
                  type="button"
                  class="tiptap-button flex items-center gap-2 !px-4"
                  @click="stopRecording"
                >
                  <span class="h-2.5 w-2.5 rounded-sm bg-current" />
                  停止录音
                </button>
              </div>

              <!-- Preview -->
              <div v-else-if="recordState === 'preview'" class="flex flex-col gap-3">
                <!-- Mini player -->
                <div
                  class="flex items-center gap-3 rounded-lg border border-white/8 bg-white/4 px-3 py-2.5"
                >
                  <button
                    type="button"
                    class="tiptap-button !h-8 !w-8 !rounded-full flex items-center justify-center"
                    @click="togglePreviewPlay"
                  >
                    <i :class="previewPlaying ? 'pi pi-pause' : 'pi pi-play'" class="text-sm" />
                  </button>
                  <div class="flex min-w-0 flex-1 flex-col gap-0.5">
                    <span class="font-mono text-xs text-white/60">录音片段</span>
                    <span class="font-mono text-[10px] text-white/30">
                      {{ formatDuration(recordedSeconds) }}
                    </span>
                  </div>
                  <button
                    type="button"
                    class="tiptap-button !h-6 !w-6 text-xs"
                    title="丢弃"
                    @click="discardRecording"
                  >
                    <i class="pi pi-times" />
                  </button>
                </div>

                <audio
                  ref="previewAudioRef"
                  :src="recordedBlobUrl ?? undefined"
                  class="hidden"
                  @ended="previewPlaying = false"
                />

                <!-- Insert -->
                <button
                  type="button"
                  class="tiptap-button w-full justify-center !h-8 text-xs is-active"
                  @click="insertRecording"
                >
                  <i class="pi pi-check mr-1" />
                  插入录音
                </button>
              </div>
            </template>
          </TabPanel>
        </Tabs>
      </div>
    </Popover>
  </div>
</template>

<script setup lang="ts">
import { ref, onBeforeUnmount, defineComponent, h } from 'vue'
import type { Editor } from '@tiptap/core'
import Popover from 'primevue/popover'
import InputText from 'primevue/inputtext'
import Tabs from 'primevue/tabs'
import TabList from 'primevue/tablist'
import Tab from 'primevue/tab'
import TabPanel from 'primevue/tabpanel'

const props = defineProps<{ editor: Editor }>()

// ── Tab icon components ──
const IconUpload = defineComponent(
  () => () =>
    h(
      'svg',
      {
        viewBox: '0 0 16 16',
        fill: 'none',
        stroke: 'currentColor',
        'stroke-width': '1.6',
        'stroke-linecap': 'round'
      },
      [h('path', { d: 'M8 10V3M5.5 5.5L8 3l2.5 2.5M2 12h12' })]
    )
)

const IconLink = defineComponent(
  () => () =>
    h(
      'svg',
      {
        viewBox: '0 0 16 16',
        fill: 'none',
        stroke: 'currentColor',
        'stroke-width': '1.6',
        'stroke-linecap': 'round'
      },
      [
        h('path', { d: 'M6 9l-1.5 1.5a3 3 0 004.24 0l2-2a3 3 0 00-4.24-4.24L5 5.75' }),
        h('path', { d: 'M10 7l1.5-1.5a3 3 0 00-4.24 0L5.26 7.5' })
      ]
    )
)

const IconMic = defineComponent(
  () => () =>
    h(
      'svg',
      {
        viewBox: '0 0 16 16',
        fill: 'none',
        stroke: 'currentColor',
        'stroke-width': '1.6',
        'stroke-linecap': 'round'
      },
      [
        h('rect', { x: '5', y: '1', width: '6', height: '9', rx: '3' }),
        h('path', { d: 'M2.5 8a5.5 5.5 0 0011 0M8 13.5V15M6 15h4' })
      ]
    )
)

const tabs = [
  { id: 'upload', label: '上传', icon: IconUpload },
  { id: 'url', label: '链接', icon: IconLink },
  { id: 'record', label: '录音', icon: IconMic }
]

// ── Popover ──
const rootRef = ref<HTMLElement | null>(null)
const popoverRef = ref()
const popoverVisible = ref(false)
const activeTab = ref('upload')

function togglePopover(event: MouseEvent) {
  popoverRef.value?.toggle(event)
}
function closePopover() {
  popoverRef.value?.hide()
}
function onPopoverHide() {
  popoverVisible.value = false
  // Reset recording state on close
  if (recordState.value === 'recording') stopRecording()
}

// ── Upload ──
const fileInputRef = ref<HTMLInputElement | null>(null)
const dragOver = ref(false)

function onFileChange(e: Event) {
  const files = Array.from((e.target as HTMLInputElement).files ?? [])
  insertFiles(files)
}

function onDrop(e: DragEvent) {
  dragOver.value = false
  const files = Array.from(e.dataTransfer?.files ?? []).filter((f) => f.type.startsWith('audio/'))
  insertFiles(files)
}

function insertFiles(files: File[]) {
  if (!files.length) return
  files.forEach((f) => props.editor.chain().focus().insertAudioFile(f).run())
  closePopover()
}

// ── URL ──
const urlDraft = ref('')

function insertFromUrl() {
  const url = urlDraft.value.trim()
  if (!url) return
  props.editor.chain().focus().setAudioBlock({ src: url, title: '' }).run()
  urlDraft.value = ''
  closePopover()
}

// ── Record ──
type RecordState = 'idle' | 'recording' | 'preview'
const recordState = ref<RecordState>('idle')
const micDenied = ref(false)
const recordedSeconds = ref(0)
const recordedBlobUrl = ref<string | null>(null)
const recordedBlob = ref<Blob | null>(null)
const previewPlaying = ref(false)
const previewAudioRef = ref<HTMLAudioElement | null>(null)
const waveformBars = ref<number[]>(Array(28).fill(8))

let mediaRecorder: MediaRecorder | null = null
let chunks: Blob[] = []
let timerInterval: ReturnType<typeof setInterval> | null = null
let animFrame: number | null = null
let analyser: AnalyserNode | null = null

async function startRecording() {
  micDenied.value = false
  try {
    const stream = await navigator.mediaDevices.getUserMedia({ audio: true })

    const ctx = new AudioContext()
    const source = ctx.createMediaStreamSource(stream)
    analyser = ctx.createAnalyser()
    analyser.fftSize = 64
    source.connect(analyser)
    drawWaveform()

    mediaRecorder = new MediaRecorder(stream)
    chunks = []
    mediaRecorder.ondataavailable = (e) => {
      if (e.data.size > 0) chunks.push(e.data)
    }
    mediaRecorder.onstop = () => {
      const blob = new Blob(chunks, { type: 'audio/webm' })
      recordedBlob.value = blob
      recordedBlobUrl.value = URL.createObjectURL(blob)
      recordState.value = 'preview'
      stream.getTracks().forEach((t) => t.stop())
      ctx.close()
      if (animFrame) cancelAnimationFrame(animFrame)
    }
    mediaRecorder.start()
    recordedSeconds.value = 0
    recordState.value = 'recording'
    timerInterval = setInterval(() => {
      recordedSeconds.value++
    }, 1000)
  } catch {
    micDenied.value = true
  }
}

function stopRecording() {
  if (timerInterval) clearInterval(timerInterval)
  mediaRecorder?.stop()
}

function drawWaveform() {
  if (!analyser) return
  const data = new Uint8Array(analyser.frequencyBinCount)
  const tick = () => {
    analyser!.getByteFrequencyData(data)
    const step = Math.floor(data.length / waveformBars.value.length)
    waveformBars.value = Array.from({ length: 28 }, (_, i) => {
      const v = data[i * step] / 255
      return Math.max(8, Math.round(v * 92))
    })
    animFrame = requestAnimationFrame(tick)
  }
  animFrame = requestAnimationFrame(tick)
}

function discardRecording() {
  if (recordedBlobUrl.value) URL.revokeObjectURL(recordedBlobUrl.value)
  recordedBlobUrl.value = null
  recordedBlob.value = null
  recordState.value = 'idle'
  waveformBars.value = Array(28).fill(8)
}

function togglePreviewPlay() {
  const a = previewAudioRef.value
  if (!a) return
  if (a.paused) {
    a.play()
    previewPlaying.value = true
  } else {
    a.pause()
    previewPlaying.value = false
  }
}

function insertRecording() {
  const blob = recordedBlob.value
  if (!blob) return
  const file = new File([blob], `录音_${new Date().toLocaleString('zh')}.webm`, { type: blob.type })
  props.editor.chain().focus().insertAudioFile(file).run()
  discardRecording()
  closePopover()
}

onBeforeUnmount(() => {
  if (timerInterval) clearInterval(timerInterval)
  if (animFrame) cancelAnimationFrame(animFrame)
  if (recordedBlobUrl.value) URL.revokeObjectURL(recordedBlobUrl.value)
})

function formatDuration(s: number) {
  const m = Math.floor(s / 60)
    .toString()
    .padStart(2, '0')
  const ss = (s % 60).toString().padStart(2, '0')
  return `${m}:${ss}`
}
</script>
