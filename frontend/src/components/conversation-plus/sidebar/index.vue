<template>
  <aside class="sb" :class="[`sb--${mode}`, open && 'sb--open']">
    <!-- 头部 -->
    <div class="sb-head">
      <span class="sb-title">AI Chat</span>
    </div>

    <!-- 会话列表 -->
    <div class="sb-nav">
      <VirtualScroller
        v-if="flatItems.length"
        :items="flatItems"
        :item-size="ITEM_H"
        scroll-height="100%"
        :lazy="true"
        :loading="loadingMore"
        show-loader
        class="vs-scroller"
        @lazy-load="onLazyLoad"
      >
        <template #item="{ item }">
          <!-- 分组标签 -->
          <p v-if="item.type === 'label'" class="nlabel" :style="{ height: LABEL_H + 'px' }">
            {{ item.label }}
          </p>

          <!-- 会话项 -->
          <div
            v-else
            class="nitem"
            :style="{ height: ITEM_H + 'px' }"
            :class="{ active: conversationId === item.id }"
            @click="handleSwitch(item.id)"
          >
            <input
              v-if="renamingId === item.id"
              ref="renRef"
              v-model="renVal"
              class="ren-input"
              @keydown.enter="confirmRen"
              @keydown.esc="cancelRen"
              @blur="confirmRen"
              @click.stop
            />
            <template v-else>
              <span class="nitem-text">{{ item.name }}</span>
              <span class="nitem-btns" @click.stop>
                <button :title="t('conversation.rename')" @click="startRen(item)">✎</button>
                <button class="del" :title="t('conversation.delete')" @click="deleteChat(item.id)">
                  ✕
                </button>
              </span>
            </template>
          </div>
        </template>

        <template #loader>
          <div class="vs-loader" :style="{ height: ITEM_H + 'px' }">
            <span class="vs-loader-dot" />
            <span class="vs-loader-dot" />
            <span class="vs-loader-dot" />
          </div>
        </template>
      </VirtualScroller>

      <p v-else class="nempty">{{ t('conversation.empty') }}</p>
    </div>

    <!-- 底部操作 -->
    <div class="sb-foot">
      <button @click="toNewConversation()">
        <span class="icon">＋</span>{{ t('conversation.newChat') }}
      </button>
      <button @click="toggleMode">
        <span class="icon">⇄</span
        >{{ mode === 'push' ? t('conversation.drawerMode') : t('conversation.pushMode') }}
      </button>
      <button @click="$emit('update:isDark', !isDark)">
        <span class="icon">{{ isDark ? '☀' : '☾' }}</span>
        {{ isDark ? t('conversation.lightMode') : t('conversation.darkMode') }}
      </button>
    </div>
  </aside>
</template>

<script setup lang="ts">
import { ref, nextTick, onMounted, computed } from 'vue'
import VirtualScroller from 'primevue/virtualscroller'
import { useChatStore } from '../common/use-chat-store/index'
import { t } from '@/locales'
import type { FlatItem } from '@/components/conversation-plus/common/types'

// ─── 行高常量（必须与 CSS 一致）────────────────────────────────────
const LABEL_H = 28
const ITEM_H = 36

// ─── Props / Emits ──────────────────────────────────────────────────
const props = defineProps<{
  open: boolean
  mode: 'push' | 'drawer'
  isDark: boolean
  type: 'DEBUG' | 'CONVERSATION'
}>()

const emit = defineEmits<{
  'update:open': [val: boolean]
  'update:mode': [val: 'push' | 'drawer']
  'update:isDark': [val: boolean]
}>()

// ─── Store ──────────────────────────────────────────────────────────
const {
  toNewConversation,
  flatItems,
  hasMore,
  loadingMore,
  conversationId,
  newChat,
  switchChat,
  deleteChat,
  renameChat,
  loadMore,
  init
} = useChatStore(props.type)

// ─── VirtualScroller ────────────────────────────────────────────────
const itemSizes = computed(() =>
  flatItems.value.map((row) => (row.type === 'label' ? LABEL_H : ITEM_H))
)
const onLazyLoad = async (event: { first: number; last: number }) => {
  if (!hasMore.value) return
  if (event.last >= flatItems.value.length - 5) await loadMore()
}

// ─── 切换会话 ───────────────────────────────────────────────────────
const handleSwitch = (id: string) => {
  switchChat(id)
  if (props.mode === 'drawer') emit('update:open', false)
}

// ─── 重命名 ─────────────────────────────────────────────────────────
const renamingId = ref<string | null>(null)
const renVal = ref('')
const renRef = ref<HTMLInputElement | null>(null)

const startRen = (item: FlatItem) => {
  renamingId.value = item.id
  renVal.value = item.name
  nextTick(() => {
    const el = Array.isArray(renRef.value) ? renRef.value[0] : renRef.value
    el?.focus()
    el?.select()
  })
}

const confirmRen = () => {
  if (renamingId.value) renameChat(renamingId.value, renVal.value)
  renamingId.value = null
}

const cancelRen = () => {
  renamingId.value = null
}

// ─── 模式切换 ───────────────────────────────────────────────────────
const toggleMode = () => emit('update:mode', props.mode === 'push' ? 'drawer' : 'push')
onMounted(() => {
  init()
})
</script>

<style scoped>
/* ── 容器 ─────────────────────────────────────────────────────────── */
.sb {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: var(--bg2);
  border-right: 1px solid var(--bd);
  flex-shrink: 0;
  overflow: hidden;
}

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
  width: var(--sb-w);
  min-width: var(--sb-w);
  border-right-width: 1px;
  pointer-events: auto;
}

.sb--drawer {
  position: absolute;
  top: 0;
  left: 0;
  bottom: 0;
  width: var(--sb-w);
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

/* ── 头部 ─────────────────────────────────────────────────────────── */
.sb-head {
  display: flex;
  align-items: center;
  padding: 12px 12px 8px;
  flex-shrink: 0;
  color: var(--t1);
}
.sb-title {
  font-size: 13px;
  font-weight: 600;
}

/* ── 导航区 ───────────────────────────────────────────────────────── */
.sb-nav {
  flex: 1;
  overflow: hidden;
  padding: 0 6px 6px;
}

.vs-scroller {
  height: 100%;
}

/* ── 分组标签 ─────────────────────────────────────────────────────── */
.nlabel {
  display: flex;
  align-items: center;
  padding: 0 6px;
  font-size: 10.5px;
  font-weight: 500;
  color: var(--t3);
  letter-spacing: 0.04em;
  user-select: none;
  box-sizing: border-box;
}

/* ── 会话项 ───────────────────────────────────────────────────────── */
.nitem {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 0 7px;
  border-radius: 6px;
  cursor: pointer;
  box-sizing: border-box;
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
  transition:
    background 0.12s,
    color 0.12s;
}
.nitem-btns button:hover {
  background: rgba(0, 0, 0, 0.07);
  color: var(--t2);
}
.nitem-btns button.del:hover {
  background: #fee2e2;
  color: #dc2626;
}

/* ── 重命名输入框 ──────────────────────────────────────────────────── */
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
.ren-input:focus {
  border-color: var(--ac);
}

/* ── 懒加载占位 ────────────────────────────────────────────────────── */
.vs-loader {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 5px;
}
.vs-loader-dot {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: var(--t3);
  animation: dot-pulse 1.2s ease-in-out infinite;
}
.vs-loader-dot:nth-child(2) {
  animation-delay: 0.2s;
}
.vs-loader-dot:nth-child(3) {
  animation-delay: 0.4s;
}

@keyframes dot-pulse {
  0%,
  80%,
  100% {
    opacity: 0.2;
    transform: scale(0.8);
  }
  40% {
    opacity: 1;
    transform: scale(1);
  }
}

/* ── 空态 ─────────────────────────────────────────────────────────── */
.nempty {
  font-size: 12px;
  color: var(--t3);
  text-align: center;
  padding: 20px;
}

/* ── 底部 ─────────────────────────────────────────────────────────── */
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
.sb-foot .icon {
  font-size: 12px;
}
</style>
