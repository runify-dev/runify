<template>
  <div ref="rootRef">
    <!-- Toolbar button -->
    <button
      type="button"
      data-style="ghost"
      class="tiptap-button"
      :class="{ 'is-active': popoverVisible }"
      title="插入文件"
      @click="togglePopover"
    >
      <span class="pi pi-file" />
    </button>

    <Popover ref="popoverRef" @show="popoverVisible = true" @hide="onPopoverHide">
      <div class="flex w-72 flex-col overflow-hidden rounded-xl">
        <!-- Header -->
        <div class="flex items-center gap-2 border-b border-white/6 px-4 py-2.5">
          <span class="h-1.5 w-1.5 rounded-full bg-[#00c8ff]" style="box-shadow: 0 0 6px #00c8ff" />
          <span class="font-mono text-[10px] uppercase tracking-widest text-white/35"
            >插入文件</span
          >
        </div>

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
            <!-- 空闲：drop zone -->
            <label
              v-if="uploadStatus === 'idle'"
              class="flex cursor-pointer flex-col items-center gap-3 rounded-lg border border-dashed border-white/12 px-4 py-5 transition-all duration-150 hover:border-[#00c8ff]/30 hover:bg-[#00c8ff]/4"
              :class="dragOver ? '!border-[#00c8ff]/50 !bg-[#00c8ff]/6' : ''"
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
                <div class="text-xs font-medium">点击选择或拖拽文件</div>
                <div class="mt-0.5 font-mono text-[10px] text-white/30">支持任意文件类型</div>
              </div>
              <input ref="fileInputRef" type="file" class="hidden" @change="onFileChange" />
            </label>

            <!-- 上传中 / 完成 / 失败 -->
            <div v-else class="flex flex-col gap-3">
              <!-- 文件信息行 -->
              <div
                class="flex items-center gap-2 rounded-lg border border-white/8 bg-white/4 px-3 py-2.5"
              >
                <div
                  class="flex h-8 w-8 flex-shrink-0 items-center justify-center rounded-md border border-white/10 bg-white/5"
                >
                  <span
                    v-if="uploadStatus === 'uploading'"
                    class="pi pi-spin pi-spinner text-xs text-[#00c8ff]"
                  />
                  <span
                    v-else-if="uploadStatus === 'done'"
                    class="pi pi-check text-xs text-[#00ffc8]"
                  />
                  <span
                    v-else-if="uploadStatus === 'error'"
                    class="pi pi-times text-xs text-red-400"
                  />
                </div>
                <div class="min-w-0 flex-1">
                  <div class="truncate text-xs text-white/80">{{ currentFile?.name }}</div>
                  <div class="font-mono text-[10px]" :class="statusColor">{{ statusLabel }}</div>
                </div>
                <span
                  v-if="uploadStatus === 'uploading'"
                  class="font-mono text-[10px] tabular-nums text-[#00c8ff]"
                >
                  {{ progress }}%
                </span>
              </div>

              <!-- 进度条 -->
              <div class="h-0.5 w-full overflow-hidden rounded-full bg-white/8">
                <div
                  class="h-full rounded-full transition-all duration-200"
                  :style="{
                    width: `${progress}%`,
                    background:
                      uploadStatus === 'error'
                        ? '#f87171'
                        : 'linear-gradient(90deg,#00c8ff,#00ffc8)'
                  }"
                />
              </div>

              <!-- 操作按钮 -->
              <div class="flex gap-2">
                <button
                  v-if="uploadStatus === 'error'"
                  type="button"
                  class="tiptap-button flex-1 justify-center !h-8 text-xs"
                  @click="reset"
                >
                  <span class="pi pi-refresh mr-1" />重新选择
                </button>
                <button
                  v-if="uploadStatus === 'done'"
                  type="button"
                  class="tiptap-button flex-1 justify-center !h-8 text-xs"
                  @click="reset"
                >
                  继续上传
                </button>
                <button
                  v-if="uploadStatus === 'done'"
                  type="button"
                  class="tiptap-button flex-1 justify-center !h-8 text-xs is-active"
                  @click="closePopover"
                >
                  完成
                </button>
              </div>
            </div>
          </TabPanel>

          <!-- ── URL ── -->
          <TabPanel value="url" class="p-4">
            <div class="flex flex-col gap-2.5">
              <InputText
                v-model="urlName"
                placeholder="文件名称（可选）"
                size="small"
                fluid
                @keydown.esc="closePopover"
              />
              <InputText
                v-model="urlDraft"
                placeholder="https://example.com/file.pdf"
                size="small"
                fluid
                @keydown.enter="insertFromUrl"
                @keydown.esc="closePopover"
              />
              <button
                type="button"
                class="tiptap-button w-full justify-center !h-8 text-xs"
                :disabled="!urlDraft.trim()"
                @click="insertFromUrl"
              >
                插入链接
              </button>
            </div>
          </TabPanel>
        </Tabs>
      </div>
    </Popover>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, defineComponent, h } from 'vue'
import type { Editor } from '@tiptap/core'
import FileAPI from '@/api/file'
import Popover from 'primevue/popover'
import InputText from 'primevue/inputtext'
import Tabs from 'primevue/tabs'
import TabList from 'primevue/tablist'
import Tab from 'primevue/tab'
import TabPanel from 'primevue/tabpanel'

const props = defineProps<{ editor: Editor }>()

// ── Tab icons ──
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
const tabs = [
  { id: 'upload', label: '上传', icon: IconUpload },
  { id: 'url', label: '链接', icon: IconLink }
]

// ── Popover ──
const rootRef = ref<HTMLElement | null>(null)
const popoverRef = ref()
const popoverVisible = ref(false)
const activeTab = ref('upload')

function togglePopover(e: MouseEvent) {
  popoverRef.value?.toggle(e)
}
function closePopover() {
  popoverRef.value?.hide()
}
function onPopoverHide() {
  popoverVisible.value = false
  if (uploadStatus.value !== 'uploading') resetAll()
}

// ── Upload ──
type Status = 'idle' | 'uploading' | 'done' | 'error'

const fileInputRef = ref<HTMLInputElement | null>(null)
const dragOver = ref(false)
const currentFile = ref<File | null>(null)
const uploadStatus = ref<Status>('idle')
const progress = ref(0)
const errorMsg = ref('')

const statusLabel = computed(() => {
  if (uploadStatus.value === 'uploading') return `上传中 ${progress.value}%`
  if (uploadStatus.value === 'done') return '上传完成'
  if (uploadStatus.value === 'error') return errorMsg.value || '上传失败'
  return ''
})
const statusColor = computed(() => {
  if (uploadStatus.value === 'done') return 'text-[#00ffc8]'
  if (uploadStatus.value === 'error') return 'text-red-400'
  return 'text-[#00c8ff]'
})

function onFileChange(e: Event) {
  const file = (e.target as HTMLInputElement).files?.[0]
  if (fileInputRef.value) fileInputRef.value.value = ''
  if (file) upload(file)
}

function onDrop(e: DragEvent) {
  dragOver.value = false
  const file = e.dataTransfer?.files?.[0]
  if (file) upload(file)
}

async function upload(file: File) {
  currentFile.value = file
  uploadStatus.value = 'uploading'
  progress.value = 0
  errorMsg.value = ''

  try {
    const formData = new FormData()
    formData.append('file', file)

    const result = await FileAPI.uploadFile(formData, (pct) => {
      progress.value = Math.min(Math.round(pct), 99)
    })

    progress.value = 100
    uploadStatus.value = 'done'

    props.editor
      .chain()
      .focus()
      .setFileBlock({
        src: result.data.url, // 根据 FileEntity 实际字段调整
        name: file.name,
        size: file.size,
        mime: file.type
      })
      .run()
  } catch (err) {
    uploadStatus.value = 'error'
    errorMsg.value = err instanceof Error ? err.message : '上传失败'
    progress.value = 100
  }
}

function reset() {
  currentFile.value = null
  uploadStatus.value = 'idle'
  progress.value = 0
  errorMsg.value = ''
}

// ── URL ──
const urlDraft = ref('')
const urlName = ref('')

function insertFromUrl() {
  const url = urlDraft.value.trim()
  if (!url) return
  const name = urlName.value.trim() || url.split('/').pop() || url
  const ext = name.split('.').pop()?.toLowerCase() ?? ''
  props.editor
    .chain()
    .focus()
    .setFileBlock({ src: url, name, size: 0, mime: mimeFromExt(ext) })
    .run()
  closePopover()
}

// ── Helpers ──
function resetAll() {
  reset()
  urlDraft.value = ''
  urlName.value = ''
  dragOver.value = false
}

function mimeFromExt(ext: string): string {
  const map: Record<string, string> = {
    pdf: 'application/pdf',
    doc: 'application/msword',
    docx: 'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
    xls: 'application/vnd.ms-excel',
    xlsx: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
    ppt: 'application/vnd.ms-powerpoint',
    pptx: 'application/vnd.openxmlformats-officedocument.presentationml.presentation',
    zip: 'application/zip',
    rar: 'application/x-rar-compressed',
    txt: 'text/plain',
    csv: 'text/csv',
    json: 'application/json'
  }
  return map[ext] ?? 'application/octet-stream'
}
</script>
