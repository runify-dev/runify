<template>
  <main class="main">
    <!-- Topbar -->
    <header class="bar">
      <button v-if="showBack" class="hbtn" @click="goBack">
        <svg width="16" height="16" viewBox="0 0 16 16" fill="none">
          <path
            d="M10 3L5 8l5 5"
            stroke="currentColor"
            stroke-width="1.6"
            stroke-linecap="round"
            stroke-linejoin="round"
          />
        </svg>
      </button>
      <button class="hbtn" @click="$emit('toggle')">
        <svg width="16" height="16" viewBox="0 0 16 16" fill="none">
          <path
            d="M2 4h12M2 8h12M2 12h12"
            stroke="currentColor"
            stroke-width="1.6"
            stroke-linecap="round"
          />
        </svg>
      </button>
      <div class="bar-app">
        <img v-if="appInfo?.icon" :src="appInfo.icon" class="bar-icon" />
        <div class="bar-info">
          <span v-if="appInfo?.name" class="bar-app-name">{{ appInfo.name }}</span>
          <span class="bar-title">{{ current?.name || t('conversation.newChat') }}</span>
        </div>
      </div>
      <slot name="header"></slot>
    </header>

    <!-- 消息区 -->
    <div ref="msgBox" class="msgs" @scroll="onScroll">
      <!-- 顶部加载指示 -->
      <div class="top-loader" :class="{ visible: msgLoading && messages.length > 0 }">
        <Loading :size="24" />
      </div>

      <!-- 欢迎页 -->
      <div v-if="messages.length === 0 && !msgLoading" class="welcome">
        <p class="wt">{{ t('conversation.panel.welcomeTitle') }}</p>
        <p class="ws">{{ t('conversation.panel.welcomeSubtitle') }}</p>
        <div class="qgrid">
          <button
            v-for="p in prompts"
            :key="p.text"
            class="qcard"
            @click="conversation({ content: p.text })"
          >
            <span class="qicon">{{ p.icon }}</span>
            <span>{{ p.text }}</span>
          </button>
        </div>
      </div>

      <!-- 初始加载中 -->
      <div v-else-if="messages.length === 0 && msgLoading" class="init-loading">
        <Loading :size="36" />
      </div>

      <!-- 消息列表 -->
      <template v-else>
        <div
          v-for="(m, i) in messages"
          :key="m.id || i"
          :class="['mrow', m.role === 'USER' ? 'user' : 'assistant']"
        >
          <ContentList :content-list="m.content" />
        </div>

        <!-- 流式回复 loading -->
        <div v-if="streamLoading" class="mrow assistant">
          <Loading :size="18" />
        </div>
      </template>
    </div>

    <!-- 图片预览 -->
    <div v-if="previewSrc" class="preview-overlay" @click.self="previewSrc = null">
      <img :src="previewSrc" class="preview-img" />
      <button class="preview-close" @click="previewSrc = null">✕</button>
    </div>

    <!-- 大文本预览 -->
    <div v-if="previewText" class="preview-overlay" @click.self="previewText = null">
      <div class="preview-text-box">
        <button class="preview-close" @click="previewText = null">✕</button>
        <CodePreview :modelValue="previewText.text" :filename="previewText.filename" class="preview-code" />
      </div>
    </div>

    <!-- 输入框 -->
    <footer class="ibar">
      <!-- 审批浮窗 -->
      <div v-if="pendingApproval" class="approval-bar">
        <div class="approval-header">
          <span class="approval-icon">
            <svg width="16" height="16" viewBox="0 0 16 16" fill="none">
              <path d="M8 1.5a6.5 6.5 0 1 0 0 13 6.5 6.5 0 0 0 0-13ZM8 4v4M8 10.5h.006" stroke="currentColor" stroke-width="1.4" stroke-linecap="round"/>
            </svg>
          </span>
          <span class="approval-msg">{{ pendingApproval.content }}</span>
        </div>
        <div class="approval-actions">
          <button class="approval-btn reject" @click="closeApproval('reject')">{{ t('conversation.panel.reject') }}</button>
          <button class="approval-btn approve" @click="closeApproval('approve')">{{ t('conversation.panel.approve') }}</button>
        </div>
      </div>
      <div class="iwrap" :class="{ focused }">
        <!-- 附件缩略图 -->
        <div v-if="pastedImages.length || pastedTexts.length || pastedVideos.length || pastedFiles.length" class="athumbs">
          <div v-for="(img, i) in pastedImages" :key="'img-' + i" class="athumb">
            <img :src="img.previewUrl" class="athumb-img" @click="previewSrc = img.previewUrl" />
            <button class="athumb-rm" @click.stop="removeImage(i)">✕</button>
          </div>
          <div v-for="(t, i) in pastedTexts" :key="'txt-' + i" class="athumb-text" @click="previewText = t">
            <svg class="athumb-text-icon" width="20" height="20" viewBox="0 0 20 20" fill="none">
              <path d="M12 2H5a2 2 0 0 0-2 2v12a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2V7l-5-5Z" stroke="currentColor" stroke-width="1.2" stroke-linecap="round" stroke-linejoin="round"/>
              <path d="M12 2v5h5" stroke="currentColor" stroke-width="1.2" stroke-linecap="round" stroke-linejoin="round"/>
              <path d="M6 10h8M6 13.5h5" stroke="currentColor" stroke-width="1.2" stroke-linecap="round"/>
            </svg>
            <span class="athumb-text-label">Text</span>
            <button class="athumb-rm" @click.stop="removeText(i)">✕</button>
          </div>
          <!-- 视频 -->
          <div v-for="(v, i) in pastedVideos" :key="'vid-' + i" class="athumb-text">
            <svg class="athumb-text-icon" width="20" height="20" viewBox="0 0 20 20" fill="none">
              <rect x="2" y="4" width="16" height="12" rx="2" stroke="currentColor" stroke-width="1.2"/>
              <path d="M8.5 7.5v5l4.5-2.5-4.5-2.5Z" fill="currentColor"/>
            </svg>
            <span class="athumb-text-label">Video</span>
            <button class="athumb-rm" @click.stop="removeVideo(i)">✕</button>
          </div>
          <!-- 文件 -->
          <div v-for="(f, i) in pastedFiles" :key="'file-' + i" class="athumb-text">
            <svg class="athumb-text-icon" width="20" height="20" viewBox="0 0 20 20" fill="none">
              <path d="M12 2H5a2 2 0 0 0-2 2v12a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2V7l-5-5Z" stroke="currentColor" stroke-width="1.2" stroke-linecap="round" stroke-linejoin="round"/>
              <path d="M12 2v5h5" stroke="currentColor" stroke-width="1.2" stroke-linecap="round" stroke-linejoin="round"/>
              <path d="M10 11v4M8 13l2 2 2-2" stroke="currentColor" stroke-width="1.2" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
            <span class="athumb-text-label">{{ f.file.name.length > 8 ? f.file.name.slice(0, 8) + '…' : f.file.name }}</span>
            <button class="athumb-rm" @click.stop="removeFile(i)">✕</button>
          </div>
        </div>

        <div class="irow">
          <input
            ref="fileInputRef"
            type="file"
            multiple
            accept="image/*,video/*,.txt,.md,.json,.csv,.xml,.yaml,.yml,.ts,.js,.jsx,.tsx,.html,.css,.sh,.sql,.py,.rb,.go,.rs,.java,.c,.cpp,.h,.ppt,.pptx,.doc,.docx,.xls,.xlsx,.pdf"
            style="display: none"
            @change="handleFileSelect"
          />
          <MdEditor
            ref="editorRef"
            v-model="question.content"
            :disabled="streamLoading || !!pendingApproval"
            :placeholder="t('conversation.panel.placeholder')"
            @submit="conversation(question)"
            @paste-images="handlePasteImages"
            @paste-videos="handlePasteVideos"
            @paste-files="handlePasteFiles"
            @paste-text="handlePasteText"
            @focus="focused = true"
            @blur="focused = false"
          />
          <button
            class="upload-btn"
            :disabled="streamLoading || !!pendingApproval"
            @click="fileInputRef?.click()"
          >
            <svg width="16" height="16" viewBox="0 0 16 16" fill="none">
              <path d="M14 10v2.5a1.5 1.5 0 01-1.5 1.5h-9A1.5 1.5 0 012 12.5V10M8 2v8.5M5 5l3-3 3 3" stroke="currentColor" stroke-width="1.4" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
          </button>
          <button
            v-if="!streamLoading"
            class="sbtn"
            :class="{ on: hasContent }"
            :disabled="!hasContent"
            @click="conversation(question)"
          >
            <svg width="16" height="16" viewBox="0 0 16 16" fill="none">
              <path
                d="M8 14V2M3 7l5-5 5 5"
                stroke="currentColor"
                stroke-width="1.8"
                stroke-linecap="round"
                stroke-linejoin="round"
              />
            </svg>
          </button>
          <button
            v-else
            class="sbtn stop"
            @click="handleStop"
          >
            <svg width="14" height="14" viewBox="0 0 14 14" fill="none">
              <rect x="2" y="2" width="10" height="10" rx="2" fill="currentColor"/>
            </svg>
          </button>
        </div>
      </div>
    </footer>
  </main>
</template>

<script setup lang="ts">
import { ref, computed, nextTick, reactive, onMounted, onUnmounted, watch } from 'vue'
import { t } from '@/locales'
import { useChatStore } from '../common/use-chat-store/index'
import { aggregators, Scroll } from '@/components/conversation-plus/index'
import ContentList from '@/components/conversation-plus/content-list/index.vue'
import Loading from '@/components/conversation-plus/loading/index.vue'
import MdEditor from '@/components/conversation-plus/editor/index.vue'
import CodePreview from '@/components/conversation-plus/code-preview/index.vue'
import { randomId } from '@/utils/common'
import FileAPI from '@/api/file'

const props = defineProps<{ type: 'DEBUG' | 'CONVERSATION' | 'ADMIN_CONVERSATION' }>()
const emit = defineEmits<{ toggle: []; chanage: []; close: [] }>()

const {
  appInfo,
  messages,
  current,
  msgLoading,
  hasMoreMsg,
  loadMoreMessages,
  pushMessage,
  newChat,
  chat,

  streamLoading,
  startStream,
  cancel,
  cancelStream,
  switchConversation,

  statusStream,
  resumeStream,
  setStreamIndex,
  clearStreamIndex,

  showBack,
  goBack
} = useChatStore(props.type)

const focused = ref(false)
const msgBox = ref<HTMLElement | null>(null)
const editorRef = ref<InstanceType<typeof MdEditor> | null>(null)
const fileInputRef = ref<HTMLInputElement | null>(null)
const question = ref<any>({ content: '' })

const handleStop = () => {
  cancel()
}
const pendingApproval = computed(() => {
  const lastMsg = messages.value[messages.value.length - 1]
  if (!lastMsg || !Array.isArray(lastMsg.content)) return null
  return lastMsg.content.find(
    (item: any) => item.type === 'APPROVAL'
  ) || null
})
const closeApproval = (action: 'approve' | 'reject') => {
  const approval = pendingApproval.value
  if (!approval) return

  // push 用户审批消息，历史记录展示
  pushMessage({
    role: 'USER',
    content: [
      {
        type: 'APPROVAL_SUBMIT',
        content: approval.content,
        result: action
      }
    ],
    id: '',
    conversationId: '',
    applicationId: '',
    createTime: '',
    updateTime: ''
  })

  const cid = current.value?.id
  if (!cid) return

  clearStreamIndex()

  const answerMessage = createAnswerMessage()
  pushMessage(answerMessage as any)

  startStream({
    cid,
    request: () =>
      chat({
        content: {
          type: 'APPROVAL_SUBMIT',
          content: approval.content,
          result: action,
          position: approval.position
        },
        workflowRunId: approval.workflowRunId
      }),
    onStream: getOnStream(answerMessage),
    onFinish: () => {
      clearStreamIndex()
    }
  })
}

// ─── 上传接口 ──────────────────────────────────────────────────
interface FileEntry { url: string; name: string }

const uploadFile = async (file: File): Promise<FileEntry> => {
  const cid = current.value?.id
  const fd = new FormData()
  fd.append('file', file)
  if (cid) {
    fd.append('refType', 'CONVERSATION')
    fd.append('ref', cid)
  }
  const ok = await FileAPI.uploadFile(fd)
  return { url: `./api/storage/file/${ok.data.id}`, name: file.name }
}

// ─── 粘贴图片附件 ──────────────────────────────────────────────
interface PastedImage {
  file: File
  previewUrl: string
  uploaded?: FileEntry
  uploading?: boolean
}
const pastedImages = ref<PastedImage[]>([])

const handlePasteImages = async (files: File[]) => {
  for (const file of files) {
    const item: PastedImage = { file, previewUrl: URL.createObjectURL(file), uploading: true }
    pastedImages.value.push(item)
    try {
      item.uploaded = await uploadFile(file)
    } catch (e) {
      console.error('upload image failed', e)
    } finally {
      item.uploading = false
    }
  }
}

const removeImage = (index: number) => {
  URL.revokeObjectURL(pastedImages.value[index].previewUrl)
  pastedImages.value.splice(index, 1)
}

const clearImages = () => {
  pastedImages.value.forEach((img) => URL.revokeObjectURL(img.previewUrl))
  pastedImages.value = []
}

// ─── 粘贴大文本附件 ─────────────────────────────────────────────
interface PastedText {
  text: string
  filename?: string
}
const pastedTexts = ref<PastedText[]>([])

const LONG_TEXT_THRESHOLD = 300

const handlePasteText = (text: string, filename?: string) => {
  pastedTexts.value.push({ text, filename })
}

const removeText = (index: number) => {
  pastedTexts.value.splice(index, 1)
}

const clearTexts = () => {
  pastedTexts.value = []
}

// ─── 粘贴视频附件 ─────────────────────────────────────────────
interface PastedFile {
  file: File
  uploaded?: FileEntry
  uploading?: boolean
}
const pastedVideos = ref<PastedFile[]>([])

const handlePasteVideos = async (files: File[]) => {
  for (const file of files) {
    const item: PastedFile = { file, uploading: true }
    pastedVideos.value.push(item)
    try {
      item.uploaded = await uploadFile(file)
    } catch (e) {
      console.error('upload video failed', e)
    } finally {
      item.uploading = false
    }
  }
}

const removeVideo = (index: number) => {
  pastedVideos.value.splice(index, 1)
}

const clearVideos = () => {
  pastedVideos.value = []
}

// ─── 粘贴文件附件 ─────────────────────────────────────────────
const pastedFiles = ref<PastedFile[]>([])

const handlePasteFiles = async (files: File[]) => {
  for (const file of files) {
    const item: PastedFile = { file, uploading: true }
    pastedFiles.value.push(item)
    try {
      item.uploaded = await uploadFile(file)
    } catch (e) {
      console.error('upload file failed', e)
    } finally {
      item.uploading = false
    }
  }
}

const handleFileSelect = async (event: Event) => {
  const input = event.target as HTMLInputElement
  if (!input.files?.length) return

  const files = Array.from(input.files)
  for (const file of files) {
    if (file.type.startsWith('image/')) {
      const item: PastedImage = { file, previewUrl: URL.createObjectURL(file), uploading: true }
      pastedImages.value.push(item)
      try {
        item.uploaded = await uploadFile(file)
      } catch (e) {
        console.error('upload image failed', e)
      } finally {
        item.uploading = false
      }
    } else if (file.type.startsWith('video/')) {
      const item: PastedFile = { file, uploading: true }
      pastedVideos.value.push(item)
      try {
        item.uploaded = await uploadFile(file)
      } catch (e) {
        console.error('upload video failed', e)
      } finally {
        item.uploading = false
      }
    } else {
      const item: PastedFile = { file, uploading: true }
      pastedFiles.value.push(item)
      try {
        item.uploaded = await uploadFile(file)
      } catch (e) {
        console.error('upload file failed', e)
      } finally {
        item.uploading = false
      }
    }
  }

  // 清空 input 以便重复选择同一文件
  input.value = ''
}

const removeFile = (index: number) => {
  pastedFiles.value.splice(index, 1)
}

const clearFiles = () => {
  pastedFiles.value = []
}

// ─── 是否有内容可发送 ──────────────────────────────────────────
const hasContent = computed(() => {
  return (
    question.value.content.trim() ||
    pastedImages.value.length ||
    pastedTexts.value.length ||
    pastedVideos.value.length ||
    pastedFiles.value.length
  )
})

// ─── 图片预览 ──────────────────────────────────────────────────
const previewSrc = ref<string | null>(null)
const previewText = ref<PastedText | null>(null)

let scroll: any
let newChatInProgress = false

const prompts = computed(() => [
  { icon: '✦', text: t('conversation.panel.prompt1') },
  { icon: '◈', text: t('conversation.panel.prompt2') },
  { icon: '◉', text: t('conversation.panel.prompt3') },
  { icon: '◇', text: t('conversation.panel.prompt4') }
])

// ─── 创建 assistant 占位消息 ─────────────────────────────────────
const createAnswerMessage = () => {
  return reactive({
    type: 'LOADING',
    role: 'ASSISTANT' as const,
    content: [],
    id: '',
    conversationId: '',
    applicationId: '',
    createTime: '',
    updateTime: ''
  })
}

// ─── 恢复时复用最后一条 assistant，没有就创建 ─────────────────────
const getOrCreateLastAnswerMessage = () => {
  const last = messages.value[messages.value.length - 1]

  if (last && last.role === 'ASSISTANT') {
    return last
  }

  const answerMessage = createAnswerMessage()
  pushMessage(answerMessage as any)
  return answerMessage
}

// ─── 流式回复聚合 ─────────────────────────────────────────────────
const getOnStream = (message: any) => {
  const indexList: string[] = Array.isArray(message.content)
    ? message.content.map((content: any) => content.id + '_' + content.type)
    : []

  return (chunk: any) => {
    if (typeof chunk.index === 'number') {
      setStreamIndex(chunk.index)
    }

    if (!Array.isArray(chunk.content)) {
      return
    }

    chunk.content.forEach((content: any) => {
      const id = content.id + '_' + content.type
      let i = indexList.indexOf(id)

      if (i < 0) {
        i = indexList.length
        indexList.push(id)
      }

      if (i < message.content.length) {
        message.content[i] = aggregators[content.type](message.content[i], content)
      } else {
        message.content[i] = content
      }
    })

    emit('chanage')
    scroll?.scrollBottom()
  }
}

// ─── 切换会话时加载消息并尝试恢复流 ───────────────────────────────
watch(
  () => current.value?.id,
  async (id) => {
    if (!id) return
    if (newChatInProgress) return

    await switchConversation({
      cid: id,
      getOnStream: () => getOnStream(getOrCreateLastAnswerMessage()),
      onFinish: () => {
        clearStreamIndex()
      }
    })

    await nextTick()
    // PreView 组件有 100ms 延迟渲染，需要等 DOM 完全更新
    setTimeout(() => scroll?.forceBottom(), 150)
  },
  { immediate: true }
)

// ─── 向上滚动懒加载 ───────────────────────────────────────────────
const onScroll = async () => {
  const el = msgBox.value
  if (!el) return

  if (el.scrollTop > 60) return
  if (!hasMoreMsg.value || msgLoading.value) return

  const prevScrollHeight = el.scrollHeight
  await loadMoreMessages()
  await nextTick()
  el.scrollTop = el.scrollHeight - prevScrollHeight
}

// ─── 发起新对话 ───────────────────────────────────────────────────
const conversation = async (q: any) => {
  if (!q.content?.trim() && !hasContent.value) return
  if (streamLoading.value) return

  if (!current.value) {
    newChatInProgress = true
    const chatData = await newChat(q.content)
    newChatInProgress = false
    await nextTick()
  }

  const cid = current.value?.id
  if (!cid) return

  clearStreamIndex()

  // 获取已上传的文件
  const getUploaded = (items: PastedFile[]): FileEntry[] =>
    items.filter((item) => item.uploaded).map((item) => item.uploaded!)

  // 图片
  let imageEntries: FileEntry[] = []
  if (pastedImages.value.length) {
    imageEntries = pastedImages.value.filter((img) => img.uploaded).map((img) => img.uploaded!)
    clearImages()
  }

  // 视频
  let videoEntries: FileEntry[] = []
  if (pastedVideos.value.length) {
    videoEntries = getUploaded(pastedVideos.value)
    clearVideos()
  }

  // 文件
  let fileEntries: FileEntry[] = []
  if (pastedFiles.value.length) {
    fileEntries = getUploaded(pastedFiles.value)
    clearFiles()
  }

  // 大文本
  let texts: string[] = []
  if (pastedTexts.value.length) {
    texts = pastedTexts.value.map((t) => t.text)
    clearTexts()
  }

  const contentObj: any = { type: 'QUESTION', content: q.content }
  if (imageEntries.length) contentObj.images = imageEntries
  if (videoEntries.length) contentObj.videos = videoEntries
  if (fileEntries.length) contentObj.files = fileEntries
  if (texts.length) contentObj.texts = texts

  const payload: any = { content: contentObj, workflowRunId: randomId() }

  const contentItem: any = { ...q, type: 'QUESTION' }
  if (imageEntries.length) contentItem.images = imageEntries
  if (videoEntries.length) contentItem.videos = videoEntries
  if (fileEntries.length) contentItem.files = fileEntries
  if (texts.length) contentItem.texts = texts
  pushMessage({
    role: 'USER',
    content: [contentItem],
    id: '',
    conversationId: '',
    applicationId: '',
    createTime: '',
    updateTime: ''
  })

  const answerMessage = createAnswerMessage()
  pushMessage(answerMessage as any)

  startStream({
    cid,
    request: () => chat(payload),
    onStream: getOnStream(answerMessage),
    onFinish: () => {
      clearStreamIndex()
    }
  })

  question.value.content = ''
  editorRef.value?.clear()
}

onMounted(() => {
  scroll = new Scroll(msgBox.value)
})

onUnmounted(() => {
})
</script>
<style scoped>
.main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  height: 100%;
  background: var(--bg);
  overflow: hidden;
  position: relative;
  z-index: 1;
}

.bar {
  display: flex;
  align-items: center;
  gap: 6px;
  height: 44px;
  padding: 0 10px;
  flex-shrink: 0;
  border-bottom: 1px solid var(--bd);
  background: var(--bg);
  position: relative;
  z-index: 50;
}

.bar-app {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1;
  min-width: 0;
}

.bar-icon {
  width: 24px;
  height: 24px;
  border-radius: 6px;
  object-fit: cover;
  flex-shrink: 0;
}

.bar-info {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.bar-app-name {
  font-size: 11px;
  font-weight: 400;
  color: var(--t3);
  line-height: 1.2;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.bar-title {
  font-size: 13px;
  font-weight: 500;
  color: var(--t1);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  line-height: 1.3;
}

/* 消息区 */
.msgs {
  flex: 1;
  overflow-y: auto;
  padding: 14px 12px;
  scrollbar-width: thin;
  scrollbar-color: var(--bd) transparent;
  display: flex;
  flex-direction: column;
  align-items: center;
}

/* 顶部懒加载指示器 */
.top-loader {
  display: flex;
  justify-content: center;
  padding: 4px 0 8px;
  opacity: 0;
  height: 0;
  overflow: hidden;
  transition:
    opacity 0.2s,
    height 0.2s;
}

.top-loader.visible {
  opacity: 1;
  height: 28px;
}

/* 初始加载居中 */
.init-loading {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}

.welcome {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  padding: 16px 10px 24px;
}

.wt {
  font-size: 16px;
  font-weight: 600;
  color: var(--t1);
  margin-bottom: 4px;
}

.ws {
  font-size: 12px;
  color: var(--t3);
  margin-bottom: 16px;
}

.qgrid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 6px;
  width: 100%;
  max-width: 320px;
}

.qcard {
  display: flex;
  align-items: flex-start;
  gap: 6px;
  padding: 9px 10px;
  border-radius: 8px;
  border: 1px solid var(--bd);
  background: var(--bg2);
  cursor: pointer;
  text-align: left;
  font-size: 11.5px;
  color: var(--t2);
  font-family: inherit;
  transition: background 0.13s;
}

.qcard:hover {
  background: var(--hv);
}

.qicon {
  font-size: 11px;
  color: var(--t3);
  flex-shrink: 0;
  margin-top: 1px;
}

.mrow {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 6px;
  margin-bottom: 10px;
  max-width: 680px;
  margin-left: auto;
  margin-right: auto;
  width: 100%;
}

.mrow.assistant {
  align-items: flex-start;
}

.mrow.user {
  flex-direction: row-reverse;
}

.bub {
  max-width: min(82%, 400px);
  padding: 8px 11px 6px;
  border-radius: 12px;
  word-break: break-word;
}

.bub.user {
  background: var(--ub);
  border-bottom-right-radius: 3px;
}

.bub.assistant {
  background: var(--ab);
  border-bottom-left-radius: 3px;
}

.bub p {
  font-size: 14px;
  line-height: 1.6;
  white-space: pre-wrap;
  margin: 0 0 3px;
  color: var(--t2);
}

.ibar {
  flex-shrink: 0;
  padding: 12px 16px 16px;
  background: var(--bg);
  display: flex;
  flex-direction: column;
  align-items: center;
}

/* 图片附件缩略图 */
.athumbs {
  display: flex;
  gap: 8px;
  padding: 8px 8px 0;
  flex-wrap: wrap;
}

.athumb {
  position: relative;
  width: 56px;
  height: 56px;
  border-radius: 8px;
  overflow: hidden;
  border: 1px solid var(--bd);
  background: var(--bg2);
  flex-shrink: 0;
}

.athumb-text {
  position: relative;
  width: 56px;
  height: 56px;
  border-radius: 8px;
  border: 1px solid var(--bd);
  background: linear-gradient(135deg, var(--bg) 0%, var(--hv) 100%);
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 2px;
  box-sizing: border-box;
  cursor: pointer;
  transition: border-color 0.15s;
}

.athumb-text:hover {
  border-color: var(--t3);
}

.athumb-text-icon {
  color: var(--t2);
  flex-shrink: 0;
}

.athumb-text-label {
  font-size: 9px;
  font-weight: 500;
  color: var(--t3);
  letter-spacing: 0.3px;
}

.athumb-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  cursor: pointer;
  display: block;
}

.athumb-rm {
  position: absolute;
  top: 2px;
  right: 2px;
  width: 16px;
  height: 16px;
  border-radius: 50%;
  border: none;
  background: rgba(0, 0, 0, 0.55);
  color: var(--p-content-background);
  font-size: 9px;
  line-height: 16px;
  text-align: center;
  cursor: pointer;
  padding: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.15s;
}

.athumb:hover .athumb-rm,
.athumb-text:hover .athumb-rm {
  opacity: 1;
}

/* 图片预览 */
.preview-overlay {
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

.preview-img {
  max-width: 85%;
  max-height: 85%;
  border-radius: 8px;
  object-fit: contain;
  cursor: default;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.3);
}

.preview-text-box {
  position: relative;
  width: min(70vw, 800px);
  height: min(75vh, 600px);
  background: var(--bg);
  border-radius: 12px;
  overflow: hidden;
  cursor: default;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.3);
  display: flex;
  flex-direction: column;
}

.preview-code {
  flex: 1;
  min-height: 0;
}

.preview-close {
  position: absolute;
  top: 12px;
  right: 12px;
  width: 28px;
  height: 28px;
  border-radius: 50%;
  border: none;
  background: rgba(0, 0, 0, 0.5);
  color: var(--p-content-background);
  font-size: 14px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 10;
}

.preview-close:hover {
  background: rgba(0, 0, 0, 0.7);
}

.iwrap {
  width: 100%;
  max-width: 680px;
  display: flex;
  flex-direction: column;
  background: var(--bg);
  border: 1px solid var(--bd);
  border-radius: 16px;
  transition:
    border-color 0.2s,
    box-shadow 0.2s;
}

.irow {
  display: flex;
  align-items: flex-end;
  gap: 8px;
  padding: 8px 8px 8px 14px;
}

.upload-btn {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  border: none;
  background: transparent;
  color: var(--t3);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  transition: background 0.15s, color 0.15s;
}

.upload-btn:hover {
  background: var(--hv);
  color: var(--t2);
}

.upload-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.iwrap.focused {
  border-color: var(--focus-border);
  box-shadow: 0 0 0 2px var(--hover-overlay);
}

.sbtn {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  border: none;
  background: var(--bd);
  color: var(--t3);
  cursor: not-allowed;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  touch-action: manipulation;
  -webkit-tap-highlight-color: transparent;
  transition:
    background 0.2s,
    color 0.2s,
    transform 0.1s;
}

.sbtn.on {
  background: var(--t1);
  color: var(--bg);
  cursor: pointer;
}

.sbtn.on:hover {
  opacity: 0.85;
}

.sbtn.on:active {
  transform: scale(0.92);
}

.sbtn.stop {
  background: #ef4444;
  color: var(--p-content-background);
  cursor: pointer;
  pointer-events: auto;
}

.sbtn.stop:hover {
  background: #dc2626;
}

.sbtn.stop:active {
  transform: scale(0.92);
}

/* ── 审批浮窗 ──────────────────────────────────────────────────────── */
.approval-bar {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 10px 12px;
  margin-bottom: 8px;
  background: var(--bg);
  border: 1px solid var(--bd);
  border-radius: 10px;
  width: 100%;
  max-width: 680px;
}

.approval-header {
  display: flex;
  align-items: center;
  gap: 8px;
}

.approval-icon {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: var(--hv);
  color: var(--t2);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.approval-msg {
  flex: 1;
  font-size: 13px;
  color: var(--t1);
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.approval-actions {
  display: flex;
  gap: 6px;
  justify-content: flex-end;
}

.approval-btn {
  padding: 5px 14px;
  border-radius: 6px;
  border: 1px solid var(--bd);
  background: var(--bg);
  color: var(--t1);
  font-size: 12.5px;
  font-family: inherit;
  cursor: pointer;
  transition: background 0.15s;
}

.approval-btn:hover {
  background: var(--hv);
}

.approval-btn.approve {
  background: var(--t1);
  color: var(--bg);
  border-color: var(--t1);
}

.approval-btn.approve:hover {
  opacity: 0.85;
}
</style>
