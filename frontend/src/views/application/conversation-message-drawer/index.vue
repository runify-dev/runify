<template>
  <Drawer
    v-model:visible="visible"
    header="对话详情"
    position="right"
    class="!w-full md:!w-[40rem] lg:!w-[60%] xl:!w-[50rem] max-w-[70rem] h-full"
    :pt="{
      content: { class: 'p-0 flex flex-col overflow-hidden h-full' },
      header: { class: 'border-b border-[var(--bd)] px-4 py-3' }
    }"
  >
    <div class="view-wrap">
      <!-- 顶部懒加载指示器 -->
      <div class="top-loader" :class="{ visible: loadingMore }">
        <span class="typing"><span /><span /><span /></span>
      </div>

      <!-- 消息滚动区 -->
      <div ref="msgBox" class="msgs" @scroll="onScroll">
        <!-- 初始加载中 -->
        <div v-if="initialLoading" class="init-loading">
          <span class="typing"><span /><span /><span /></span>
        </div>

        <!-- 空状态 -->
        <div v-else-if="messages.length === 0" class="empty-state">
          <span class="empty-icon">◈</span>
          <p>暂无对话记录</p>
        </div>

        <!-- 消息列表 -->
        <template v-else>
          <div
            v-for="(m, i) in messages"
            :key="i"
            :class="['mrow', m.type === 'USER' ? 'user' : 'assistant']"
          >
            <!-- 用户消息 -->
            <template v-if="m.type === 'USER'">
              <div class="bub user">
                <p>{{ getTextContent(m) }}</p>
              </div>
            </template>

            <!-- AI 消息 -->
            <template v-else>
              <ContentList v-if="m.content?.length" :content-list="m.content" />
              <div v-else class="bub assistant">
                <p>{{ getTextContent(m) }}</p>
              </div>
            </template>
          </div>
        </template>
      </div>
    </div>
  </Drawer>
</template>

<script setup lang="ts">
import { ref, nextTick } from 'vue'
import applicationAPI from '@/api/application'
// 如果你的项目有 ContentList 组件，按需引入
// import ContentList from '@/components/conversation-plus/content-list/index.vue'

// ── 类型 ──────────────────────────────────────────────────────────
interface Message {
  type: 'USER' | 'AI' | 'ASSISTANT'
  content: any[]
  [key: string]: any
}

// ── 状态 ──────────────────────────────────────────────────────────
const visible = ref(false)
const messages = ref<Message[]>([])
const initialLoading = ref(false)
const loadingMore = ref(false)

const current = ref(1)
const pageSize = ref(20)
const hasMore = ref(true)

let _applicationId = ''
let _conversationId = ''

const msgBox = ref<HTMLElement | null>(null)

// ── 工具函数 ──────────────────────────────────────────────────────
const getTextContent = (m: Message): string => {
  if (!m.content?.length) return ''
  const block = m.content.find(
    (c: any) => c.type === 'QUESTION' || c.type === 'text' || typeof c.content === 'string'
  )
  return block?.content ?? block?.text ?? ''
}

const scrollToBottom = async () => {
  await nextTick()
  const el = msgBox.value
  if (el) el.scrollTop = el.scrollHeight
}

// ── 数据加载 ──────────────────────────────────────────────────────
const fetchPage = async (page: number) => {
  const res = await applicationAPI.pageConversationMessage(
    _applicationId,
    _conversationId,
    page,
    pageSize.value,
    {}
  )

  // 根据你的接口返回结构调整
  const list: Message[] = res?.data?.records ?? []
  const total: number = res?.data?.total ?? 0
  return { list, total }
}

// ── 初始打开 ──────────────────────────────────────────────────────
const open = async (applicationId: string, conversationId: string) => {
  _applicationId = applicationId
  _conversationId = conversationId

  // 重置
  messages.value = []
  current.value = 1
  hasMore.value = true
  visible.value = true

  initialLoading.value = true
  try {
    const { list, total } = await fetchPage(1)
    // 接口通常返回倒序，这里正序展示
    messages.value = [...list].reverse()
    hasMore.value = messages.value.length < total
    await scrollToBottom()
  } finally {
    initialLoading.value = false
  }
}

// ── 向上滚动加载更多（历史消息） ──────────────────────────────────
const onScroll = async () => {
  const el = msgBox.value
  if (!el) return
  if (el.scrollTop > 60) return
  if (!hasMore.value || loadingMore.value) return

  loadingMore.value = true
  const prevScrollHeight = el.scrollHeight

  try {
    current.value++
    const { list, total } = await fetchPage(current.value)
    const older = [...list].reverse()
    messages.value = [...older, ...messages.value]
    hasMore.value = messages.value.length < total

    await nextTick()
    el.scrollTop = el.scrollHeight - prevScrollHeight
  } finally {
    loadingMore.value = false
  }
}

// ── 关闭 ──────────────────────────────────────────────────────────
const close = () => {
  visible.value = false
}

defineExpose({ open, close })
</script>

<style scoped>
.view-wrap {
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
  position: relative;
}

/* 顶部懒加载指示器 */
.top-loader {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  display: flex;
  justify-content: center;
  padding: 4px 0;
  opacity: 0;
  height: 0;
  overflow: hidden;
  transition:
    opacity 0.2s,
    height 0.2s;
  z-index: 10;
  background: var(--bg, #fff);
}
.top-loader.visible {
  opacity: 1;
  height: 32px;
}

/* 消息区 */
.msgs {
  flex: 1;
  overflow-y: auto;
  padding: 14px 12px;
  scrollbar-width: thin;
  scrollbar-color: var(--bd, #e5e5e5) transparent;
  display: flex;
  flex-direction: column;
  align-items: center;
}

/* 初始加载居中 */
.init-loading {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  min-height: 200px;
}

/* 空状态 */
.empty-state {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  color: var(--t3, #aaa);
  min-height: 200px;
}
.empty-icon {
  font-size: 28px;
  opacity: 0.4;
}
.empty-state p {
  font-size: 13px;
}

/* 消息行 */
.mrow {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 6px;
  margin-bottom: 10px;
  max-width: 500px;
  margin-left: auto;
  margin-right: auto;
  width: 100%;
}
.mrow.user {
  flex-direction: row-reverse;
}

/* 气泡 */
.bub {
  max-width: min(82%, 400px);
  padding: 8px 11px 6px;
  border-radius: 12px;
  word-break: break-word;
}
.bub.user {
  background: var(--ub, #e8f0fe);
  border-bottom-right-radius: 3px;
}
.bub.assistant {
  background: var(--ab, #f5f5f5);
  border-bottom-left-radius: 3px;
}
.bub p {
  font-size: 13px;
  line-height: 1.6;
  white-space: pre-wrap;
  margin: 0 0 3px;
  color: var(--t1, #1a1a1a);
}

/* 打字动画 */
.typing {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 10px 13px;
}
.typing span {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: #c0c0c0;
  animation: dot 1.2s infinite ease-in-out;
}
.typing span:nth-child(2) {
  animation-delay: 0.18s;
}
.typing span:nth-child(3) {
  animation-delay: 0.36s;
}

@keyframes dot {
  0%,
  80%,
  100% {
    transform: translateY(0);
    opacity: 0.3;
  }
  40% {
    transform: translateY(-4px);
    opacity: 1;
  }
}
</style>
