<template>
  <main class="main">
    <!-- Topbar -->
    <header class="bar">
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
      <span class="bar-title">{{ current?.title || '新建对话' }}</span>
      <slot name="header"></slot>
    </header>

    <!-- 消息区 -->
    <div ref="msgBox" class="msgs" @scroll="onScroll">
      <!-- 顶部加载指示 -->
      <div class="top-loader" :class="{ visible: msgLoading && messages.length > 0 }">
        <span class="typing"><span /><span /><span /></span>
      </div>

      <!-- 欢迎页 -->
      <div v-if="messages.length === 0 && !msgLoading" class="welcome">
        <p class="wt">今天有什么可以帮你？</p>
        <p class="ws">选择快捷提示或直接输入</p>
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
        <span class="typing"><span /><span /><span /></span>
      </div>

      <!-- 消息列表 -->
      <template v-else>
        <div
          v-for="(m, i) in messages"
          :key="i"
          :class="['mrow', m.type === 'USER' ? 'user' : 'assistant']"
        >
          <ContentList :content-list="m.content" />
        </div>

        <!-- 流式回复 loading -->
        <div v-if="streamLoading" class="mrow assistant">
          <div class="bub assistant typing"><span /><span /><span /></div>
        </div>
      </template>
    </div>

    <!-- 输入框 -->
    <footer class="ibar">
      <div class="iwrap" :class="{ focused }">
        <textarea
          ref="ta"
          v-model="question.content"
          rows="1"
          placeholder="发送消息…"
          :disabled="streamLoading"
          @focus="focused = true"
          @blur="focused = false"
          @keydown.enter.exact.prevent="conversation(question)"
          @input="resize"
        />
        <button
          class="sbtn"
          :class="{ on: question.content.trim() && !streamLoading }"
          :disabled="!question.content.trim() || streamLoading"
          @click="conversation(question)"
        >
          <svg width="13" height="13" viewBox="0 0 13 13" fill="none">
            <path
              d="M6.5 11V2M2.5 6L6.5 2l4 4"
              stroke="currentColor"
              stroke-width="1.7"
              stroke-linecap="round"
              stroke-linejoin="round"
            />
          </svg>
        </button>
      </div>
    </footer>
  </main>
</template>

<script setup lang="ts">
import { ref, nextTick, reactive, inject, onMounted, watch } from 'vue'
import { useChatStore } from '../common/use-chat-store/index'
import { ConversationStream } from '@/api/common'
import { aggregators, Scroll } from '@/components/conversation-plus/index'
import ContentList from '@/components/conversation-plus/content-list/index.vue'

const conversationAPI = inject('conversationAPI') as any
const props = defineProps<{ type: 'DEBUG' | 'CONVERSATION' }>()
const emit = defineEmits<{ toggle: []; chanage: []; close: [] }>()

const {
  messages,
  current,
  msgLoading,
  hasMoreMsg,
  loadMessages,
  loadMoreMessages,
  pushMessage,
  newChat
} = useChatStore(props.type)

const focused = ref(false)
const streamLoading = ref(false)
const msgBox = ref<HTMLElement | null>(null)
const ta = ref<HTMLTextAreaElement | null>(null)
const question = ref<any>({ content: '' })

const prompts = [
  { icon: '✦', text: '帮我写一篇关于 AI 的技术文章' },
  { icon: '◈', text: '用 TypeScript 解释泛型' },
  { icon: '◉', text: '如何学习 Vue3 + Vite？' },
  { icon: '◇', text: '推荐健康的晚餐食谱' }
]
let scroll: any
// ─── 滚动到底部 ───────────────────────────────────────────────────
const scrollToBottom = async () => {
  await nextTick()
  scroll.scrollBottom()
}

// ─── 切换会话时加载消息并滚到底 ──────────────────────────────────
watch(
  () => current.value?.id,
  async (id) => {
    if (!id) return
    if (streamLoading.value) return
    await loadMessages(id)
    setTimeout(() => {
      scrollToBottom()
    }, 100)
  },
  { immediate: true }
)

// ─── 向上滚动懒加载 ───────────────────────────────────────────────
const onScroll = async () => {
  const el = msgBox.value
  if (!el) return
  // 滚到顶部附近 60px 时触发
  if (el.scrollTop > 60) return
  if (!hasMoreMsg.value || msgLoading.value) return

  // 记住加载前高度，加载后恢复位置防跳动
  const prevScrollHeight = el.scrollHeight
  await loadMoreMessages()
  await nextTick()
  el.scrollTop = el.scrollHeight - prevScrollHeight
}

// ─── 流式回复 ─────────────────────────────────────────────────────
const getOnStream = (message: any) => {
  const index: any[] = []
  return (chunk: any) => {
    const id = chunk.id + '_' + chunk.type
    let i = index.indexOf(id)
    if (i < 0) {
      i = index.length
      index.push(id)
    }
    if (message.content.length <= i) {
      message.content[i] = chunk
    } else {
      message.content[i] = aggregators[chunk.type](message.content[i], chunk)
    }
    scrollToBottom()
    emit('chanage')
  }
}

const conversation = async (q: any) => {
  if (!q.content.trim()) return
  streamLoading.value = true
  console.log('sss', current.value)
  if (!current.value) {
    await newChat(q.content)
    await nextTick()
  }

  pushMessage({
    type: 'USER',
    content: [{ ...q, type: 'QUESTION' }]
  })

  const answerMessage = reactive({ type: 'LOADING', content: [] })
  pushMessage(answerMessage)

  await scrollToBottom()

  new ConversationStream(
    conversationAPI({ ...q }),
    getOnStream(answerMessage),
    () => {
      streamLoading.value = false
      scrollToBottom()
    },
    () => {
      streamLoading.value = false
    }
  ).stream()

  question.value.content = ''
  resize()
}

// ─── textarea 自适应高度 ──────────────────────────────────────────
const resize = () => {
  const el = ta.value
  if (!el) return
  el.style.height = 'auto'
  el.style.height = Math.min(el.scrollHeight, 120) + 'px'
}
onMounted(() => {
  scroll = new Scroll(msgBox.value)
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
.bar-title {
  flex: 1;
  font-size: 13.5px;
  font-weight: 500;
  color: var(--t1);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
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
  align-items: center; /* ← 加这个 */
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
  max-width: 500px;
  margin-left: auto;
  margin-right: auto;
  width: 100%;
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
  font-size: 13px;
  line-height: 1.6;
  white-space: pre-wrap;
  margin: 0 0 3px;
}

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

.ibar {
  flex-shrink: 0;
  padding: 8px 10px;

  background: var(--bg);
  display: flex; /* ← 加 */
  justify-content: center; /* ← 加 */
}

.iwrap {
  width: 100%;
  max-width: 680px; /* ← 和 .mrow 保持一致 */
  display: flex;
  align-items: flex-end;
  gap: 6px;
  background: var(--ab);
  border: 1.5px solid var(--bd);
  border-radius: 11px;
  padding: 6px 6px 6px 11px;
  transition: border-color 0.15s;
}
.iwrap.focused {
  border-color: #c0c0c0;
}
textarea {
  flex: 1;
  background: transparent;
  border: none;
  outline: none;
  resize: none;
  font-size: 13px;
  font-family: inherit;
  color: var(--t1);
  line-height: 1.5;
  max-height: 120px;
  overflow-y: auto;
  padding: 0;
  margin: 0;
}
textarea::placeholder {
  color: var(--t3);
}
textarea:disabled {
  opacity: 0.5;
}

.sbtn {
  width: 30px;
  height: 30px;
  border-radius: 7px;
  border: none;
  background: var(--bd);
  color: var(--t3);
  cursor: not-allowed;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  transition:
    background 0.15s,
    color 0.15s,
    transform 0.1s;
}
.sbtn.on {
  background: var(--t1);
  color: var(--bg);
  cursor: pointer;
}
.sbtn.on:active {
  transform: scale(0.92);
}
</style>
