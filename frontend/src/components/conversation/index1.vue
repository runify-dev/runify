<template>
  <div class="cw" :class="{ dark: isDark }">
    <!-- 遮罩：仅 drawer 模式展开时显示 -->
    <div v-if="open && mode === 'drawer'" class="mask" @click="open = false" />

    <!-- 侧边栏 -->
    <aside class="sb" :class="[`sb--${mode}`, open && 'sb--open']">
      <div class="sb-head">
        <svg width="18" height="18" viewBox="0 0 18 18" fill="none">
          <rect width="18" height="18" rx="5" fill="currentColor" />
          <path
            d="M5 9l2.5 2.5 4.5-4.5"
            stroke="white"
            stroke-width="1.6"
            stroke-linecap="round"
            stroke-linejoin="round"
          />
        </svg>
        <span class="sb-title">AI Chat</span>
      </div>

      <nav class="sb-nav">
        <template v-for="g in grouped" :key="g.label">
          <p class="nlabel">{{ g.label }}</p>
          <ul>
            <li
              v-for="c in g.items"
              :key="c.id"
              class="nitem"
              :class="{ active: activeId === c.id }"
              @click="switchChat(c.id)"
            >
              <template v-if="renamingId === c.id">
                <input
                  ref="renRef"
                  v-model="renVal"
                  class="ren-input"
                  @keydown.enter="confirmRen"
                  @keydown.esc="renamingId = null"
                  @blur="confirmRen"
                  @click.stop
                />
              </template>
              <template v-else>
                <span class="nitem-text">{{ c.title }}</span>
                <span class="nitem-btns" @click.stop>
                  <button @click="startRen(c)">✎</button>
                  <button class="del" @click="delChat(c.id)">✕</button>
                </span>
              </template>
            </li>
          </ul>
        </template>
        <p v-if="!grouped.length" class="nempty">暂无对话</p>
      </nav>

      <div class="sb-foot">
        <button @click="newChat">＋ 新建对话</button>
        <button @click="mode = mode === 'push' ? 'drawer' : 'push'">
          {{ mode === 'push' ? '⇄ 抽屉模式' : '⇄ 挤压模式' }}
        </button>
        <button @click="isDark = !isDark">{{ isDark ? '☀ 亮色' : '☾ 暗色' }}</button>
      </div>
    </aside>

    <!-- 主区域 -->
    <main class="main">
      <header class="bar">
        <button class="hbtn" @click="open = !open">
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
        <button v-if="current?.messages.length" class="hbtn" @click="clearChat">
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

      <div ref="msgBox" class="msgs">
        <div v-if="!current?.messages.length" class="welcome">
          <p class="wt">今天有什么可以帮你？</p>
          <p class="ws">选择快捷提示或直接输入</p>
          <div class="qgrid">
            <button v-for="p in prompts" :key="p.text" class="qcard" @click="send(p.text)">
              <span class="qicon">{{ p.icon }}</span>
              <span>{{ p.text }}</span>
            </button>
          </div>
        </div>
        <template v-else>
          <div v-for="(m, i) in current.messages" :key="i" class="mrow" :class="m.role">
            <span class="av" :class="m.role">{{ m.role === 'user' ? '我' : 'AI' }}</span>
            <div class="bub" :class="m.role">
              <p>{{ m.content }}</p>
              <time>{{ fmt(m.ts) }}</time>
            </div>
          </div>
          <div v-if="loading" class="mrow assistant">
            <span class="av assistant">AI</span>
            <div class="bub assistant typing"><span /><span /><span /></div>
          </div>
        </template>
      </div>

      <footer class="ibar">
        <div class="iwrap" :class="{ focused }">
          <textarea
            ref="ta"
            v-model="text"
            rows="1"
            placeholder="发送消息…"
            :disabled="loading"
            @focus="focused = true"
            @blur="focused = false"
            @keydown.enter.exact.prevent="doSend"
            @input="resize"
          />
          <button
            class="sbtn"
            :class="{ on: text.trim() && !loading }"
            :disabled="!text.trim() || loading"
            @click="doSend"
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
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, nextTick } from 'vue'

const props = withDefaults(
  defineProps<{
    defaultOpen?: boolean
    defaultMode?: 'push' | 'drawer'
  }>(),
  {
    defaultOpen: false,
    defaultMode: 'push'
  }
)

interface Msg {
  role: 'user' | 'assistant'
  content: string
  ts: Date
}
interface Chat {
  id: number
  title: string
  ts: Date
  messages: Msg[]
}

let uid = 1
const isDark = ref(false)
const open = ref(props.defaultOpen)
const mode = ref<'push' | 'drawer'>(props.defaultMode)
const text = ref('')
const focused = ref(false)
const loading = ref(false)
const activeId = ref(1)
const renamingId = ref<number | null>(null)
const renVal = ref('')

const chats = reactive<Chat[]>([{ id: uid++, title: '新建对话', ts: new Date(), messages: [] }])
const msgBox = ref<HTMLElement | null>(null)
const ta = ref<HTMLTextAreaElement | null>(null)
const renRef = ref<HTMLInputElement | null>(null)

const prompts = [
  { icon: '✦', text: '帮我写一篇关于 AI 的技术文章' },
  { icon: '◈', text: '用 TypeScript 解释泛型' },
  { icon: '◉', text: '如何学习 Vue3 + Vite？' },
  { icon: '◇', text: '推荐健康的晚餐食谱' }
]

const current = computed(() => chats.find((c) => c.id === activeId.value) ?? chats[0])

const grouped = computed(() => {
  const now = new Date()
  const t0 = new Date(now.getFullYear(), now.getMonth(), now.getDate())
  const t1 = new Date(t0.getTime() - 864e5)
  const t7 = new Date(t0.getTime() - 7 * 864e5)
  const m: Record<string, Chat[]> = { 今天: [], 昨天: [], 本周: [], 更早: [] }
  ;[...chats]
    .sort((a, b) => +b.ts - +a.ts)
    .forEach((c) => {
      const d = new Date(new Date(c.ts).setHours(0, 0, 0, 0))
      if (d >= t0) m['今天'].push(c)
      else if (d >= t1) m['昨天'].push(c)
      else if (d >= t7) m['本周'].push(c)
      else m['更早'].push(c)
    })
  return Object.entries(m)
    .filter(([, v]) => v.length)
    .map(([label, items]) => ({ label, items }))
})

const newChat = () => {
  const c: Chat = { id: uid++, title: '新建对话', ts: new Date(), messages: [] }
  chats.unshift(c)
  activeId.value = c.id
}
const switchChat = (id: number) => {
  activeId.value = id
  if (mode.value === 'drawer') open.value = false
}
const delChat = (id: number) => {
  const i = chats.findIndex((c) => c.id === id)
  if (i < 0) return
  chats.splice(i, 1)
  if (!chats.length) {
    newChat()
    return
  }
  if (activeId.value === id) activeId.value = chats[0].id
}
const startRen = (c: Chat) => {
  renamingId.value = c.id
  renVal.value = c.title
  nextTick(() => {
    const el = Array.isArray(renRef.value) ? renRef.value[0] : renRef.value
    el?.focus()
  })
}
const confirmRen = () => {
  if (renamingId.value !== null) {
    const c = chats.find((x) => x.id === renamingId.value)
    if (c && renVal.value.trim()) c.title = renVal.value.trim()
  }
  renamingId.value = null
}
const clearChat = () => {
  if (current.value) {
    current.value.messages = []
    current.value.title = '新建对话'
  }
}
const scrollBottom = () =>
  nextTick(() => msgBox.value?.scrollTo({ top: msgBox.value.scrollHeight, behavior: 'smooth' }))
const send = (t: string) => {
  const s = t.trim()
  if (!s || loading.value || !current.value) return
  if (!current.value.messages.length) {
    current.value.title = s.slice(0, 24) + (s.length > 24 ? '…' : '')
    current.value.ts = new Date()
  }
  current.value.messages.push({ role: 'user', content: s, ts: new Date() })
  text.value = ''
  if (ta.value) ta.value.style.height = 'auto'
  loading.value = true
  scrollBottom()
  setTimeout(() => {
    current.value?.messages.push({
      role: 'assistant',
      content: '这是模拟回复，可替换为真实 API。',
      ts: new Date()
    })
    loading.value = false
    scrollBottom()
  }, 1200)
}
const doSend = () => send(text.value)
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
*,
*::before,
*::after {
  box-sizing: border-box;
  -webkit-tap-highlight-color: transparent;
}

/* ── 根容器 ── */
.cw {
  --w: 200px;
  --bg: #fff;
  --bg2: #f7f7f5;
  --bd: #eaeaea;
  --t1: #1a1a1a;
  --t2: #555;
  --t3: #aaa;
  --hv: #eeeeec;
  --ac: #e9e9e7;
  --ub: #1a1a1a;
  --ab: #f5f5f3;

  position: relative;
  display: flex;
  width: 100%;
  height: 100%;
  overflow: hidden;
  font-family: 'PingFang SC', 'Noto Sans SC', system-ui, sans-serif;
  background: var(--bg);
  color: var(--t1);
}
.dark.cw {
  --bg: #111;
  --bg2: #181818;
  --bd: #252525;
  --t1: #f0f0f0;
  --t2: #bbb;
  --t3: #555;
  --hv: #222;
  --ac: #252525;
  --ub: #efefef;
  --ab: #1d1d1d;
}

/* ── 遮罩（drawer 专用） ── */
.mask {
  position: absolute;
  inset: 0;
  z-index: 30;
  background: rgba(0, 0, 0, 0.35);
}

/* ── 侧边栏基础样式 ── */
.sb {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: var(--bg2);
  border-right: 1px solid var(--bd);
  flex-shrink: 0;
  overflow: hidden;
}

/*
  PUSH 模式：在文档流内，width 从 0 → var(--w)
  收起时 width:0 + min-width:0 + pointer-events:none
  确保不占空间也不拦截事件
*/
.sb--push {
  position: relative;
  z-index: 1;
  width: 0;
  min-width: 0;
  border-right-width: 0;
  pointer-events: none;
  transition:
    width 0.25s cubic-bezier(0.4, 0, 0.2, 1),
    min-width 0.25s cubic-bezier(0.4, 0, 0.2, 1),
    border-right-width 0.25s;
}
.sb--push.sb--open {
  width: var(--w);
  min-width: var(--w);
  border-right-width: 1px;
  pointer-events: auto;
}

/*
  DRAWER 模式：absolute 覆盖，不占文档流
  z-index 高于 main(1)，低于 topbar(50)
*/
.sb--drawer {
  position: absolute;
  top: 0;
  left: 0;
  bottom: 0;
  width: var(--w);
  z-index: 40;
  transform: translateX(-100%);
  transition:
    transform 0.25s cubic-bezier(0.4, 0, 0.2, 1),
    box-shadow 0.25s;
}
.sb--drawer.sb--open {
  transform: translateX(0);
  box-shadow: 4px 0 16px rgba(0, 0, 0, 0.1);
}

/* ── 侧边栏内部 ── */
.sb-head {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 12px 8px;
  flex-shrink: 0;
  color: var(--t1);
}
.sb-title {
  font-size: 13px;
  font-weight: 600;
}

.sb-nav {
  flex: 1;
  overflow-y: auto;
  padding: 0 6px 6px;
  scrollbar-width: thin;
  scrollbar-color: var(--bd) transparent;
}
.nlabel {
  font-size: 10.5px;
  font-weight: 500;
  color: var(--t3);
  padding: 8px 6px 3px;
  letter-spacing: 0.04em;
  user-select: none;
}
ul {
  list-style: none;
  padding: 0;
  margin: 0 0 3px;
}
.nitem {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 7px 7px;
  border-radius: 6px;
  cursor: pointer;
  min-width: 0;
  transition: background 0.12s;
}
.nitem:hover {
  background: var(--hv);
}
.nitem.active {
  background: var(--ac);
}
.nitem-text {
  flex: 1;
  min-width: 0;
  font-size: 12.5px;
  color: var(--t2);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.nitem.active .nitem-text {
  color: var(--t1);
  font-weight: 500;
}
.nitem-btns {
  display: none;
  gap: 2px;
  flex-shrink: 0;
}
.nitem:hover .nitem-btns,
.nitem.active .nitem-btns {
  display: flex;
}
.nitem-btns button {
  width: 20px;
  height: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: none;
  border-radius: 4px;
  background: transparent;
  color: var(--t3);
  cursor: pointer;
  font-size: 11px;
}
.nitem-btns button:hover {
  background: rgba(0, 0, 0, 0.07);
  color: var(--t2);
}
.dark .nitem-btns button:hover {
  background: rgba(255, 255, 255, 0.07);
}
.nitem-btns button.del:hover {
  background: #fee2e2;
  color: #dc2626;
}
.dark .nitem-btns button.del:hover {
  background: #3a1414;
  color: #f87171;
}
.nempty {
  font-size: 12px;
  color: var(--t3);
  text-align: center;
  padding: 20px;
}
.ren-input {
  flex: 1;
  font-size: 12px;
  font-family: inherit;
  background: var(--bg);
  border: 1px solid var(--bd);
  border-radius: 4px;
  padding: 2px 6px;
  color: var(--t1);
  outline: none;
}

.sb-foot {
  padding: 6px;
  border-top: 1px solid var(--bd);
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  gap: 1px;
}
.sb-foot button {
  display: flex;
  align-items: center;
  gap: 6px;
  width: 100%;
  padding: 7px 8px;
  border-radius: 6px;
  border: none;
  background: transparent;
  color: var(--t3);
  font-size: 12px;
  font-family: inherit;
  cursor: pointer;
  text-align: left;
  transition:
    background 0.12s,
    color 0.12s;
}
.sb-foot button:hover {
  background: var(--hv);
  color: var(--t2);
}

/* ── 主区域 ── */
.main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  height: 100%;
  background: var(--bg);
  overflow: hidden;
  /* z-index:1 低于 drawer(40)，但 .bar 会自己提升 */
  position: relative;
  z-index: 1;
}

/* topbar：z-index 高于 drawer(40)，按钮永远可点 */
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

/* ── 消息区 ── */
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
.dark .av.user {
  background: #1e2a4a;
  color: #7a9cf7;
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
.dark .bub.user p {
  color: #1a1a1a;
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
.dark .bub.user time {
  color: rgba(0, 0, 0, 0.3);
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

/* ── 输入框 ── */
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
.dark .iwrap.focused {
  border-color: #3a3a3a;
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
