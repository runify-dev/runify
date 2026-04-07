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
      <button v-if="messages.length" class="hbtn">
        <svg width="14" height="14" viewBox="0 0 14 14" fill="none">
          <path
            d="M2 3h10M5 3V2h4v1M5.5 5v5M8.5 5v5M2.5 3l.5 9h8l.5-9"
            stroke="currentColor"
            stroke-width="1.3"
            stroke-linecap="round"
            stroke-linejoin="round"
          />
        </svg>
      </button>
      <span v-else style="width: 28px; display: inline-block" />
    </header>

    <!-- 消息列表 -->
    <div ref="msgBox" class="msgs">
      <!-- 欢迎页 -->
      <div v-if="messages.length == 0" class="welcome">
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

      <!-- 消息 -->
      <template v-else>
        <div v-for="(m, i) in messages" :key="i" :class="m.role">
          <ContentList :content-list="m.content"></ContentList>
        </div>
        <div v-if="loading" class="mrow assistant">
          <span class="av assistant">AI</span>
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
          :disabled="loading"
          @focus="focused = true"
          @blur="focused = false"
          @keydown.enter.exact.prevent="conversation(question)"
          @input="resize"
        />
        <button
          class="sbtn"
          :class="{ on: text.trim() && !loading }"
          :disabled="!text.trim() || loading"
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
import { ref, nextTick, reactive, inject } from 'vue'
import { useChatStore } from '../common/use-chat-store/index'
import { ConversationStream } from '@/api/common'
import Content from '@/components/conversation-plus/content/index.vue'
import { aggregators } from '@/components/conversation-plus/index'
import ContentList from '@/components/conversation-plus/content-list/index.vue'
const conversationAPI = inject('conversationAPI') as any

const emit = defineEmits<{ toggle: []; chanage: [] }>()

const { messages, current, pushMessage, newChat } = useChatStore('DEBUG')

const text = ref('')
const focused = ref(false)
const loading = ref(false)
const msgBox = ref<HTMLElement | null>(null)
const ta = ref<HTMLTextAreaElement | null>(null)

const prompts = [
  { icon: '✦', text: '帮我写一篇关于 AI 的技术文章' },
  { icon: '◈', text: '用 TypeScript 解释泛型' },
  { icon: '◉', text: '如何学习 Vue3 + Vite？' },
  { icon: '◇', text: '推荐健康的晚餐食谱' }
]

const scrollBottom = () =>
  nextTick(() => msgBox.value?.scrollTo({ top: msgBox.value.scrollHeight, behavior: 'smooth' }))

const getOnStream = (message: any) => {
  const index: any[] = []
  const onStream = (chunk: any) => {
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
    emit('chanage')
  }
  return onStream
}
const isConversation = ref<boolean>(false)

const question = ref<any>({
  content: ''
})
const conversation = (q: any) => {
  if (isConversation.value) {
    return
  }
  if (!current.value) {
    newChat()
  }
  pushMessage({
    type: 'USER',
    content: [{ ...q, type: 'QUESTION' }]
  })

  const answerMessage = reactive({
    type: 'LOADING',
    content: []
  })
  pushMessage(answerMessage)

  new ConversationStream(
    conversationAPI({ ...q }),
    getOnStream(answerMessage),
    () => {},
    () => {}
  ).stream()
  question.value.content = ''
}

const resize = () => {
  const el = ta.value
  if (!el) return
  el.style.height = 'auto'
  el.style.height = Math.min(el.scrollHeight, 120) + 'px'
}

const fmt = (d: Date) => {
  const now = new Date()
  return d.toDateString() === now.toDateString()
    ? d.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit', hour12: false })
    : d.toLocaleDateString('zh-CN', { month: 'numeric', day: 'numeric' })
}
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

/* Topbar：z-index 高于 drawer(40) */
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
.hbtn {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: none;
  border-radius: 7px;
  background: transparent;
  color: var(--t3);
  cursor: pointer;
  flex-shrink: 0;
  transition:
    background 0.12s,
    color 0.12s;
}
.hbtn:hover {
  background: var(--hv);
  color: var(--t1);
}

/* 消息区 */
.msgs {
  flex: 1;
  overflow-y: auto;
  padding: 14px 12px;
  scrollbar-width: thin;
  scrollbar-color: var(--bd) transparent;
}

.welcome {
  height: 100%;
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

.av {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 10px;
  font-weight: 600;
}
.av.assistant {
  background: var(--ab);
  color: var(--t2);
}
.av.user {
  background: #e8f0fe;
  color: #3b5bdb;
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
.bub.user p {
  color: #f0f0f0;
}
.bub.assistant p {
  color: var(--t1);
}
.bub time {
  display: block;
  font-size: 10px;
  text-align: right;
  color: rgba(255, 255, 255, 0.3);
}
.bub.assistant time {
  color: var(--t3);
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

/* 输入框 */
.ibar {
  flex-shrink: 0;
  padding: 8px 10px;
  border-top: 1px solid var(--bd);
  background: var(--bg);
}
.iwrap {
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
