<template>
  <div class="question-wrapper">
    <div class="question-col">
    <div class="question-card">
      <div v-if="images.length + texts.length + videos.length + files.length > 0" class="question-attachments">
        <!-- 图片 -->
        <img
          v-for="(img, i) in images"
          :key="'img-' + i"
          :src="fileUrl(img)"
          class="question-img"
          @click="previewSrc = fileUrl(img)"
        />
        <!-- 视频 -->
        <div
          v-for="(v, i) in videos"
          :key="'vid-' + i"
          class="question-file-card"
          @click="previewVideo = fileUrl(v)"
        >
          <div class="qfc-inner">
            <svg class="qfc-icon" width="20" height="20" viewBox="0 0 20 20" fill="none">
              <rect x="2" y="4" width="16" height="12" rx="2" stroke="currentColor" stroke-width="1.2"/>
              <path d="M8.5 7.5v5l4.5-2.5-4.5-2.5Z" fill="currentColor"/>
            </svg>
            <span class="qfc-label">{{ fileLabel(v) }}</span>
          </div>
        </div>
        <!-- 文本 -->
        <div
          v-for="(t, i) in texts"
          :key="'txt-' + i"
          class="question-file-card"
          @click="previewText = t"
        >
          <div class="qfc-inner">
            <svg class="qfc-icon" width="20" height="20" viewBox="0 0 20 20" fill="none">
              <path d="M12 2H5a2 2 0 0 0-2 2v12a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2V7l-5-5Z" stroke="currentColor" stroke-width="1.2" stroke-linecap="round" stroke-linejoin="round"/>
              <path d="M12 2v5h5" stroke="currentColor" stroke-width="1.2" stroke-linecap="round" stroke-linejoin="round"/>
              <path d="M6 10h8M6 13.5h5" stroke="currentColor" stroke-width="1.2" stroke-linecap="round"/>
            </svg>
            <span class="qfc-label">Text</span>
          </div>
        </div>
        <!-- 文件 -->
        <div
          v-for="(f, i) in files"
          :key="'file-' + i"
          class="question-file-card"
          @click="downloadFile(f)"
        >
          <div class="qfc-inner">
            <svg class="qfc-icon" width="20" height="20" viewBox="0 0 20 20" fill="none">
              <path d="M12 2H5a2 2 0 0 0-2 2v12a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2V7l-5-5Z" stroke="currentColor" stroke-width="1.2" stroke-linecap="round" stroke-linejoin="round"/>
              <path d="M12 2v5h5" stroke="currentColor" stroke-width="1.2" stroke-linecap="round" stroke-linejoin="round"/>
              <path d="M10 11v4M8 13l2 2 2-2" stroke="currentColor" stroke-width="1.2" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
            <span class="qfc-label">{{ fileLabel(f) }}</span>
          </div>
        </div>
      </div>
      <div
        v-if="content"
        ref="textRef"
        class="question-text"
        :class="{ collapsed: !expanded && isLong }"
      >
        {{ content }}
      </div>
      <button v-if="isLong" class="expand-btn" @click="expanded = !expanded">
        {{ expanded ? t('conversation.panel.collapse') : t('conversation.panel.expand') }}
      </button>
    </div>
      <!-- 复制提问 -->
      <button v-if="content" class="q-copy" :class="{ done: copied }" @click="copyQuestion">
        <svg v-if="copied" class="q-copy-ic" viewBox="0 0 16 16" fill="none">
          <path d="M3.5 8.5l3 3 6-6.5" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
        </svg>
        <svg v-else class="q-copy-ic" viewBox="0 0 16 16" fill="none">
          <rect x="5" y="5" width="8.5" height="9.5" rx="1.6" stroke="currentColor" stroke-width="1.2"/>
          <path d="M3 11V3.6A1.6 1.6 0 0 1 4.6 2H10.5" stroke="currentColor" stroke-width="1.2" stroke-linecap="round"/>
        </svg>
        <span>{{ copied ? t('conversation.panel.copied') : t('conversation.panel.copy') }}</span>
      </button>
    </div>

    <!-- 图片预览 -->
    <Teleport to="body">
      <div v-if="previewSrc" class="q-preview-overlay" @click.self="previewSrc = null">
        <img :src="previewSrc" class="q-preview-img" />
        <button class="q-preview-close" @click="previewSrc = null">✕</button>
      </div>
    </Teleport>

    <!-- 视频预览 -->
    <Teleport to="body">
      <div v-if="previewVideo" class="q-preview-overlay" @click.self="previewVideo = null">
        <div class="q-preview-video-box">
          <button class="q-preview-close" @click="previewVideo = null">✕</button>
          <video :src="previewVideo" controls autoplay class="q-preview-video" />
        </div>
      </div>
    </Teleport>

    <!-- 大文本预览 -->
    <Teleport to="body">
      <div v-if="previewText" class="q-preview-overlay" @click.self="previewText = null">
        <div class="q-preview-text-box">
          <button class="q-preview-close" @click="previewText = null">✕</button>
          <CodePreview :modelValue="previewText" class="q-preview-code" />
        </div>
      </div>
    </Teleport>
  </div>
</template>
<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { t } from '@/locales'
import CodePreview from '@/components/conversation-plus/code-preview/index.vue'

const COLLAPSE_LINES = 3

const props = defineProps<{ content: any }>()
const content = computed(() => props.content.content)

const images = computed(() => props.content.images || [])
const texts = computed(() => props.content.texts || [])
const videos = computed(() => props.content.videos || [])
const files = computed(() => props.content.files || [])

const attachments = computed(
  () => images.value.length + texts.value.length + videos.value.length + files.value.length
)
const fileUrl = (f: any) => (typeof f === 'string' ? f : f.url)

const fileLabel = (f: any) => {
  const name = typeof f === 'string' ? f.split('/').pop() || 'File' : f.name || 'File'
  return name.length > 10 ? name.slice(0, 10) + '…' : name
}

const downloadFile = (f: any) => {
  const url = fileUrl(f)
  if (url) window.open(url, '_blank')
}

const expanded = ref(false)
const textRef = ref<HTMLElement | null>(null)
const previewSrc = ref<string | null>(null)
const previewText = ref<string | null>(null)
const previewVideo = ref<string | null>(null)

const isLong = computed(() => {
  const text = content.value || ''
  return text.length > 80 || (text.match(/\n/g) || []).length >= COLLAPSE_LINES
})

watch(content, () => {
  expanded.value = false
})

// 复制提问（点击后短暂显示“已复制”）
const copied = ref(false)
let copyTimer: ReturnType<typeof setTimeout> | null = null
const copyQuestion = async () => {
  const text = content.value || ''
  if (!text) return
  try {
    await navigator.clipboard.writeText(text)
  } catch {
    // 降级：兼容非安全上下文
    const ta = document.createElement('textarea')
    ta.value = text
    ta.style.position = 'fixed'
    ta.style.opacity = '0'
    document.body.appendChild(ta)
    ta.select()
    document.execCommand('copy')
    document.body.removeChild(ta)
  }
  copied.value = true
  if (copyTimer) clearTimeout(copyTimer)
  copyTimer = setTimeout(() => (copied.value = false), 1500)
}
</script>
<style scoped>
.question-wrapper {
  display: flex;
  justify-content: flex-end;
  width: 100%;
  padding: 8px 0;
}

/* 卡片 + 复制按钮竖排、右对齐 */
.question-col {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 3px;
  max-width: 420px;
}

.question-card {
  max-width: 100%;
  padding: 8px 12px;
  background: var(--p-primary-100);
  color: var(--t1);
  border-radius: 12px;
  border-bottom-right-radius: 3px;
  font-size: 14px;
  line-height: 1.6;
  word-break: break-word;
  text-align: right;
}

/* 复制按钮 */
.q-copy {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 2px 6px;
  border: none;
  background: none;
  border-radius: 6px;
  font-size: 12px;
  line-height: 1.4;
  color: var(--t3);
  cursor: pointer;
  font-family: inherit;
  opacity: 0.6;
  transition: opacity 0.15s, color 0.15s, background 0.15s;
}

.question-wrapper:hover .q-copy {
  opacity: 1;
}

.q-copy:hover {
  color: var(--t1);
  background: var(--hv);
}

.q-copy.done {
  color: var(--p-primary-color);
  opacity: 1;
}

.q-copy-ic {
  width: 13px;
  height: 13px;
  flex-shrink: 0;
}

.question-attachments {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 6px;
}

.question-img {
  width: 80px;
  height: 80px;
  object-fit: cover;
  border-radius: 8px;
  border: 1px solid var(--bd);
  cursor: pointer;
  display: block;
}

.question-img:hover {
  opacity: 0.85;
}

.question-file-card {
  width: 80px;
  height: 80px;
  border-radius: 10px;
  border: 1px solid var(--bd);
  background: linear-gradient(135deg, var(--bg) 0%, var(--hv) 100%);
  cursor: pointer;
  box-sizing: border-box;
  overflow: hidden;
  transition: border-color 0.15s, box-shadow 0.15s;
}

.question-file-card:hover {
  border-color: var(--t3);
  box-shadow: 0 2px 8px var(--shadow);
}

.qfc-inner {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 5px;
}

.qfc-icon {
  color: var(--t2);
}

.qfc-label {
  font-size: 10px;
  font-weight: 500;
  color: var(--t3);
  letter-spacing: 0.3px;
}

.question-text.collapsed {
  max-height: calc(3 * 1.6em);
  overflow: hidden;
  position: relative;
}

.question-text.collapsed::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 2.4em;
  background: linear-gradient(transparent, var(--p-primary-100));
  pointer-events: none;
}

.expand-btn {
  display: block;
  margin-top: 4px;
  padding: 0;
  border: none;
  background: none;
  color: var(--t3);
  font-size: 12px;
  cursor: pointer;
  font-family: inherit;
}

.expand-btn:hover {
  color: var(--t1);
}
</style>

<style>
/* 预览弹层 — Teleport 到 body，不 scoped */
.q-preview-overlay {
  position: fixed;
  inset: 0;
  z-index: 999999;
  background: var(--mask);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  backdrop-filter: blur(4px);
}

.q-preview-img {
  max-width: 85%;
  max-height: 85%;
  border-radius: 8px;
  object-fit: contain;
  cursor: default;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.3);
}

.q-preview-video-box {
  position: relative;
  max-width: 85vw;
  max-height: 85vh;
  cursor: default;
}

.q-preview-video {
  max-width: 85vw;
  max-height: 85vh;
  border-radius: 8px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.3);
}

.q-preview-text-box {
  position: relative;
  width: min(70vw, 800px);
  height: min(75vh, 600px);
  background: var(--p-content-background);
  border-radius: 12px;
  overflow: hidden;
  cursor: default;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.3);
  display: flex;
  flex-direction: column;
}

.q-preview-code {
  flex: 1;
  min-height: 0;
}

.q-preview-close {
  position: absolute;
  top: 8px;
  right: 8px;
  width: 28px;
  height: 28px;
  border-radius: 50%;
  border: none;
  background: var(--hover-overlay);
  color: var(--p-text-muted-color);
  font-size: 14px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 10;
}

.q-preview-close:hover {
  background: rgba(0, 0, 0, 0.12);
}
</style>
