<template>
  <!-- 侧边栏 -->
  <aside class="sb" :class="[`sb--${mode}`, open && 'sb--open']">
    <div class="sb-head">
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
            @click="handleSwitch(c.id)"
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
              <span class="nitem-text">{{ c.name }}</span>
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
      <button @click="newChat()">＋ 新建对话</button>
      <button @click="$emit('update:mode', mode === 'push' ? 'drawer' : 'push')">
        {{ mode === 'push' ? '⇄ 抽屉模式' : '⇄ 挤压模式' }}
      </button>
      <button @click="$emit('update:isDark', !isDark)">
        {{ isDark ? '☀ 亮色' : '☾ 暗色' }}
      </button>
    </div>
  </aside>
</template>

<script setup lang="ts">
import { ref, nextTick } from 'vue'
import { useChatStore } from '../common/use-chat-store/index'
import type { Conversation } from '../common/types'

const props = defineProps<{
  open: boolean
  mode: 'push' | 'drawer'
  isDark: boolean
}>()

const emit = defineEmits<{
  'update:open': [val: boolean]
  'update:mode': [val: 'push' | 'drawer']
  'update:isDark': [val: boolean]
}>()

const {
  messages,
  grouped,
  activeId,
  newChat,
  switchChat,
  deleteChat: delChat,
  renameChat
} = useChatStore('DEBUG')

const renamingId = ref<number | null>(null)
const renVal = ref('')
const renRef = ref<HTMLInputElement | null>(null)

const handleSwitch = (id: number) => {
  switchChat(id)
  if (props.mode === 'drawer') emit('update:open', false)
}

const startRen = (c: Conversation) => {
  renamingId.value = c.id
  renVal.value = c.name
  nextTick(() => {
    const el = Array.isArray(renRef.value) ? renRef.value[0] : renRef.value
    el?.focus()
  })
}

const confirmRen = () => {
  if (renamingId.value !== null) renameChat(renamingId.value, renVal.value)
  renamingId.value = null
}
</script>

<style scoped>
.sb {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: var(--bg2);
  border-right: 1px solid var(--bd);
  flex-shrink: 0;
  overflow: hidden;
}

/* Push 模式 */
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

/* Drawer 模式 */
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
  padding: 7px;
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
.nitem-btns button.del:hover {
  background: #fee2e2;
  color: #dc2626;
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
</style>
