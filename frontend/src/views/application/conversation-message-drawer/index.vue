<template>
  <Drawer
    v-model:visible="visible"
    :header="t('application.conversationLogData.title')"
    position="right"
    class="!w-full md:!w-[40rem] lg:!w-[60%] xl:!w-[50rem] max-w-[70rem] h-full"
    :pt="{
      content: { class: 'p-0 flex flex-col overflow-hidden h-full' },
      header: { class: 'border-b border-[var(--bd)] px-4 py-3' }
    }"
  >
    <div class="view-wrap">
      <!-- 对话 / 记忆 切换 -->
      <div class="tab-bar">
        <div class="tab-switch">
          <button :class="['tab-btn', { active: tab === 'chat' }]" @click="tab = 'chat'">
            {{ t('application.conversationLogData.chatTab') }}
          </button>
          <button :class="['tab-btn', { active: tab === 'memory' }]" @click="switchMemory">
            {{ t('application.conversationLogData.memoryTab') }}
          </button>
        </div>
        <span style="flex: 1"></span>
        <button v-if="tab === 'memory'" class="icon-btn" @click="loadMemory">
          <i class="pi pi-refresh" :class="{ 'pi-spin': memoryLoading }" />
        </button>
      </div>

      <!-- 顶部懒加载指示器 -->
      <div class="top-loader" :class="{ visible: loadingMore }">
        <span class="typing"><span /><span /><span /></span>
      </div>

      <!-- 记忆面板（跨对话层：摘要 + 便签，含产物） -->
      <div v-if="tab === 'memory'" class="memory-panel">
        <div v-if="memoryLoading" class="init-loading">
          <span class="typing"><span /><span /><span /></span>
        </div>
        <template v-else>
          <div v-if="memoryEmpty" class="empty-state">
            <i class="pi pi-sparkles empty-icon" />
            <p>{{ t('application.conversationLogData.memoryEmpty') }}</p>
          </div>
          <template v-else>
            <div v-if="memory.summary?.text" class="mem-section">
              <div class="mem-title">{{ t('application.conversationLogData.memorySummary') }}</div>
              <div class="mem-card">
                <pre class="mem-summary">{{ memory.summary.text }}</pre>
                <div v-if="memory.summary.coveredUpto" class="mem-meta">
                  {{ t('application.conversationLogData.memoryCoveredUpto') }}:
                  {{ memory.summary.coveredUpto }}
                </div>
              </div>
            </div>
            <div v-for="group in factGroups" :key="group.subtype" class="mem-section">
              <div class="mem-title">{{ group.label }}</div>
              <div class="mem-card">
                <div v-for="fact in group.facts" :key="fact.subtype + fact.key" class="mem-fact">
                  <span class="mem-key">{{ fact.key }}</span>
                  <span class="mem-value">{{ fact.value }}</span>
                  <Tag
                    v-if="fact.scopeType === 'application'"
                    severity="info"
                    :value="t('application.conversationLogData.appLevelFact')"
                  />
                  <Tag
                    v-else-if="fact.scopeType === 'user'"
                    severity="warn"
                    :value="t('application.conversationLogData.userLevelFact')"
                  />
                </div>
              </div>
            </div>
          </template>
        </template>
      </div>

      <!-- 消息滚动区 -->
      <div v-show="tab === 'chat'" ref="msgBox" class="msgs" @scroll="onScroll">
        <!-- 初始加载中 -->
        <div v-if="initialLoading" class="init-loading">
          <span class="typing"><span /><span /><span /></span>
        </div>

        <!-- 空状态 -->
        <div v-else-if="messages.length === 0" class="empty-state">
          <i class="pi pi-comments empty-icon" />
          <p>{{ t('application.conversationLogData.noConversations') }}</p>
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
              <div class="user-block">
                <div class="bub user">
                  <p>{{ getTextContent(m) }}</p>
                </div>
                <button class="copy-btn" @click="copyText(getTextContent(m), i)">
                  <i :class="copiedIndex === i ? 'pi pi-check' : 'pi pi-copy'" />
                  <span>{{ t('common.copy') }}</span>
                </button>
              </div>
            </template>

            <!-- AI 消息：包裹 conversation-plus 主题上下文，供复用组件取色 -->
            <template v-else>
              <div class="cp-scope">
                <ContentList v-if="m.content?.length" :content-list="m.content" />
                <div v-else class="bub assistant">
                  <p>{{ getTextContent(m) }}</p>
                </div>
              </div>
            </template>
          </div>
        </template>
      </div>
    </div>
  </Drawer>
</template>

<script setup lang="ts">
import { ref, nextTick, computed } from 'vue'
import applicationAPI from '@/api/application'
import { t } from '@/locales'
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

// ── 记忆（跨对话层：摘要 + 便签，产物是 subtype=artifact 的便签） ──
const tab = ref<'chat' | 'memory'>('chat')
const memory = ref<{ summary: any; facts: any[] }>({ summary: {}, facts: [] })
const memoryLoading = ref(false)
const memoryLoaded = ref(false)

const memoryEmpty = computed(
  () => !memory.value.summary?.text && (!memory.value.facts || memory.value.facts.length === 0)
)

const SECTION_ORDER = ['goal', 'preference', 'convention', 'env', 'todo', 'artifact']
const sectionLabel = (subtype: string) => {
  const key = `application.conversationLogData.section_${subtype}`
  const label = t(key)
  return label === key ? subtype : label
}

const factGroups = computed(() => {
  const grouped: Record<string, any[]> = {}
  for (const fact of memory.value.facts || []) {
    ;(grouped[fact.subtype] ??= []).push(fact)
  }
  const order = [
    ...SECTION_ORDER,
    ...Object.keys(grouped).filter((s) => !SECTION_ORDER.includes(s))
  ]
  return order
    .filter((subtype) => grouped[subtype]?.length)
    .map((subtype) => ({ subtype, label: sectionLabel(subtype), facts: grouped[subtype] }))
})

const loadMemory = async () => {
  memoryLoading.value = true
  try {
    const res = await applicationAPI.getConversationContext(_applicationId, _conversationId)
    memory.value = {
      summary: res?.data?.summary ?? {},
      facts: res?.data?.facts ?? []
    }
    memoryLoaded.value = true
  } finally {
    memoryLoading.value = false
  }
}

const switchMemory = () => {
  tab.value = 'memory'
  if (!memoryLoaded.value) {
    loadMemory()
  }
}

const current = ref(1)
const pageSize = ref(20)
const hasMore = ref(true)

let _applicationId = ''
let _conversationId = ''

const msgBox = ref<HTMLElement | null>(null)

// ── 复制用户问题（点击后图标短暂变对勾） ──────────────────────────
const copiedIndex = ref<number | null>(null)
let copiedTimer: ReturnType<typeof setTimeout> | null = null
const copyText = async (text: string, index: number) => {
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
  copiedIndex.value = index
  if (copiedTimer) clearTimeout(copiedTimer)
  copiedTimer = setTimeout(() => {
    copiedIndex.value = null
  }, 1500)
}

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
  tab.value = 'chat'
  memory.value = { summary: {}, facts: [] }
  memoryLoaded.value = false

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
  background: var(--bg, var(--p-surface-0));
}
.top-loader.visible {
  opacity: 1;
  height: 32px;
}

/* 对话 / 记忆 切换 —— 分段控件 */
.tab-bar {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 14px;
  border-bottom: 1px solid var(--bd, var(--p-content-border-color));
}
.tab-switch {
  display: inline-flex;
  gap: 2px;
  padding: 3px;
  background: var(--ab, var(--p-surface-100));
  border-radius: 10px;
}
.tab-btn {
  border: none;
  background: transparent;
  padding: 5px 16px;
  border-radius: 8px;
  font-size: 13px;
  line-height: 1.4;
  color: var(--t3, var(--p-text-muted-color));
  cursor: pointer;
  transition: background 0.15s, color 0.15s, box-shadow 0.15s;
}
.tab-btn:hover:not(.active) {
  color: var(--t1, var(--p-text-color));
}
.tab-btn.active {
  background: var(--bg, var(--p-surface-0));
  color: var(--t1, var(--p-text-color));
  font-weight: 500;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
}
.icon-btn {
  border: none;
  background: transparent;
  width: 30px;
  height: 30px;
  border-radius: 8px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: var(--t3, var(--p-text-muted-color));
  cursor: pointer;
  transition: background 0.15s, color 0.15s;
}
.icon-btn:hover {
  background: var(--hv, var(--p-surface-100));
  color: var(--t1, var(--p-text-color));
}
.icon-btn .pi {
  font-size: 13px;
}

/* 记忆面板 */
.memory-panel {
  flex: 1;
  overflow-y: auto;
  padding: 14px 16px;
  scrollbar-width: thin;
  scrollbar-color: var(--bd, var(--p-content-border-color)) transparent;
  display: flex;
  flex-direction: column;
  gap: 14px;
}
.mem-section {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.mem-title {
  font-size: 12px;
  font-weight: 600;
  color: var(--t3, var(--p-text-muted-color));
  letter-spacing: 0.05em;
}
.mem-card {
  background: var(--ab, var(--p-surface-50));
  border: 1px solid var(--bd, var(--p-content-border-color));
  border-radius: 10px;
  padding: 10px 12px;
}
.mem-summary {
  font-size: 13px;
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-word;
  margin: 0;
  font-family: inherit;
  color: var(--t1, var(--p-text-color));
}
.mem-meta {
  margin-top: 8px;
  font-size: 12px;
  color: var(--t3, var(--p-text-muted-color));
}
.mem-fact {
  display: flex;
  align-items: baseline;
  gap: 8px;
  padding: 4px 0;
  font-size: 13px;
  border-bottom: 1px dashed var(--bd, var(--p-content-border-color));
}
.mem-fact:last-child {
  border-bottom: none;
}
.mem-key {
  flex-shrink: 0;
  max-width: 40%;
  color: var(--t3, var(--p-text-muted-color));
  word-break: break-all;
}
.mem-value {
  flex: 1;
  color: var(--t1, var(--p-text-color));
  word-break: break-word;
  white-space: pre-wrap;
}

/* 消息区 */
.msgs {
  flex: 1;
  overflow-y: auto;
  padding: 18px 16px;
  scrollbar-width: thin;
  scrollbar-color: var(--bd, var(--p-content-border-color)) transparent;
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
  color: var(--t3, var(--p-text-muted-color));
  min-height: 200px;
}
.empty-icon {
  font-size: 32px;
  opacity: 0.35;
}
.empty-state p {
  font-size: 13px;
}

/* 消息行 */
.mrow {
  display: flex;
  width: 100%;
  max-width: 640px;
  margin: 0 auto 14px;
}
.mrow.user {
  justify-content: flex-end;
}
/* AI 消息：思考过程 + 回答需纵向堆叠、占满整行、左对齐 */
.mrow.assistant {
  flex-direction: column;
  align-items: stretch;
  gap: 4px;
}

/* conversation-plus 主题上下文：复用的 ContentList/Reasoning/Text/Tool
   组件内部用 var(--t1/--t2/--t3/--bd/--hv/--ab...) 且无 fallback，
   这里对齐 .cw 的映射把变量补齐（仅作用于 AI 内容，不影响自定义 chrome） */
.cp-scope {
  --bg: var(--p-content-background);
  --bg2: var(--p-content-background);
  --bd: var(--p-content-border-color);
  --t1: var(--p-text-color);
  --t2: var(--p-text-muted-color);
  --t3: var(--p-text-muted-color);
  --hv: var(--p-content-background);
  --ac: var(--p-content-background);
  --ub: var(--p-text-color);
  --ab: var(--p-content-background);
  --shadow: 0 1px 3px rgba(0, 0, 0, 0.2);
  --danger-bg: rgba(239, 68, 68, 0.15);
  --danger-text: #ef4444;
  --focus-border: var(--p-primary-color);
  width: 100%;
}

/* 用户问题块：气泡 + 底部复制按钮 */
.user-block {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 3px;
  max-width: 82%;
}
.copy-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  border: none;
  background: transparent;
  padding: 2px 6px;
  border-radius: 6px;
  font-size: 12px;
  line-height: 1.4;
  color: var(--t3, var(--p-text-muted-color));
  cursor: pointer;
  opacity: 0.6;
  transition: opacity 0.15s, background 0.15s, color 0.15s;
}
.mrow.user:hover .copy-btn {
  opacity: 1;
}
.copy-btn:hover {
  background: var(--hv, var(--p-surface-100));
  color: var(--t1, var(--p-text-color));
}
.copy-btn .pi {
  font-size: 12px;
}

/* 气泡 */
.bub {
  max-width: 100%;
  padding: 9px 13px;
  border-radius: 14px;
  word-break: break-word;
}
.bub.user {
  background: var(--ub, var(--p-primary-100));
  border-bottom-right-radius: 4px;
}
.bub.assistant {
  background: var(--ab, var(--p-surface-100));
  border-bottom-left-radius: 4px;
}
.bub p {
  font-size: 13px;
  line-height: 1.6;
  white-space: pre-wrap;
  margin: 0;
  color: var(--t1, var(--p-text-color));
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
  background: var(--p-surface-400);
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
