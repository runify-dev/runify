<template>
  <aside class="sb" :class="[`sb--${mode}`, open && 'sb--open']">
    <!-- 头部 -->
    <div class="sb-head">
      <img v-if="appInfo?.icon" :src="appInfo.icon" class="sb-icon" />
      <span class="sb-title">{{ appInfo?.name || 'AI Chat' }}</span>
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
      <!-- 用户头像入口 -->
      <div v-if="tokenStore.isLogged && type === 'CONVERSATION'" class="sb-user" @click="togglePopover">
        <img v-if="userIcon" :src="resetUrl(userIcon)" class="sb-user-icon" />
        <span v-else class="sb-user-avatar">{{ userAvatar }}</span>
        <span class="sb-user-name">{{ tokenStore.displayName }}</span>
      </div>
    </div>

    <!-- 用户 Popover -->
    <Popover ref="popoverRef">
      <div class="sb-pop">
        <div v-if="!tokenStore.isAnonymous" class="sb-pop-user" @click="showProfileDialog = true; popoverRef?.hide()">
          <img v-if="userIcon" :src="resetUrl(userIcon)" class="sb-pop-icon" />
          <span v-else class="sb-pop-avatar">{{ userAvatar }}</span>
          <div class="sb-pop-info">
            <span class="sb-pop-name">{{ tokenStore.displayName }}</span>
            <span class="sb-pop-email">{{ profile?.user?.email }}</span>
          </div>
          <i class="pi pi-chevron-right sb-pop-arrow" />
        </div>
        <div v-if="!tokenStore.isAnonymous" class="sb-pop-divider" />
        <button v-if="tokenStore.isAnonymous" class="sb-pop-login" @click="handleLogin">
          <i class="pi pi-sign-in" />
          <span>登录</span>
        </button>
        <button v-else class="sb-pop-logout" @click="handleLogout">
          <i class="pi pi-power-off" />
          <span>退出登录</span>
        </button>
      </div>
    </Popover>

    <!-- 用户信息 Dialog -->
    <Dialog v-model:visible="showProfileDialog" modal :style="{ width: '360px' }" :pt="{ content: { class: 'pt-0' } }">
      <template #header><span /></template>
      <div class="sb-dialog">
        <div class="sb-dialog-header">
          <img v-if="userIcon" :src="resetUrl(userIcon)" class="sb-dialog-icon" />
          <span v-else class="sb-dialog-avatar">{{ userAvatar }}</span>
          <h3 class="sb-dialog-name">{{ profile?.user?.nickname || profile?.user?.username }}</h3>
          <span v-if="profile?.type === 'ANONYMOUS'" class="sb-dialog-tag">匿名</span>
        </div>
        <div v-if="!tokenStore.isAnonymous" class="sb-dialog-fields">
          <div v-if="profile?.user?.username" class="sb-dialog-row">
            <span class="sb-dialog-label">用户名</span>
            <span class="sb-dialog-value">{{ profile.user.username }}</span>
          </div>
          <div v-if="profile?.user?.email" class="sb-dialog-row">
            <span class="sb-dialog-label">邮箱</span>
            <span class="sb-dialog-value">{{ profile.user.email }}</span>
          </div>
          <div v-if="profile?.user?.phone" class="sb-dialog-row">
            <span class="sb-dialog-label">手机</span>
            <span class="sb-dialog-value">{{ profile.user.phone }}</span>
          </div>
          <div v-if="profile?.user?.createTime" class="sb-dialog-row">
            <span class="sb-dialog-label">注册时间</span>
            <span class="sb-dialog-value">{{ profile.user.createTime }}</span>
          </div>
        </div>
      </div>
    </Dialog>
  </aside>
</template>

<script setup lang="ts">
import { ref, nextTick, onMounted, computed } from 'vue'
import VirtualScroller from 'primevue/virtualscroller'
import Popover from 'primevue/popover'
import Dialog from 'primevue/dialog'
import { useChatStore } from '../common/use-chat-store/index'
import useConversationTokenStore from '@/stores/converstaion/modules/conversation-token'
import { t } from '@/locales'
import {resetUrl} from "@/utils/common"
import type { FlatItem } from '@/components/conversation-plus/common/types'
import bus from '@/bus/index'

// ─── 行高常量（必须与 CSS 一致）────────────────────────────────────
const LABEL_H = 28
const ITEM_H = 36

// ─── Props / Emits ──────────────────────────────────────────────────
const props = defineProps<{
  open: boolean
  mode: 'push' | 'drawer'
  isDark: boolean
  type: 'DEBUG' | 'CONVERSATION' | 'ADMIN_CONVERSATION'
}>()

const emit = defineEmits<{
  'update:open': [val: boolean]
  'update:mode': [val: 'push' | 'drawer']
  'update:isDark': [val: boolean]
}>()

// ─── Store ──────────────────────────────────────────────────────────
const tokenStore = useConversationTokenStore()
const profile = computed(() => tokenStore.profile)
const popoverRef = ref<InstanceType<typeof Popover> | null>(null)
const showProfileDialog = ref(false)

const userIcon = computed(() => {
  if (tokenStore.isAnonymous) return ''
  return tokenStore.profile?.user?.icon || ''
})

const userAvatar = computed(() => {
  const name = tokenStore.displayName
  return name ? name.charAt(0) : '?'
})

const togglePopover = (event: Event) => {
  popoverRef.value?.toggle(event)
}

const handleLogout = () => {
  popoverRef.value?.hide()
  bus.emit('auth:logout')
}

const handleLogin = () => {
  popoverRef.value?.hide()
  bus.emit('auth:login')
}

const {
  appInfo,
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
const renamingId = ref<string | null | undefined>(null)
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
  box-shadow: 4px 0 16px var(--shadow);
}

/* ── 头部 ─────────────────────────────────────────────────────────── */
.sb-head {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 12px 8px;
  flex-shrink: 0;
  color: var(--t1);
}
.sb-icon {
  width: 20px;
  height: 20px;
  border-radius: 5px;
  object-fit: cover;
  flex-shrink: 0;
}

.sb-title {
  font-size: 13px;
  font-weight: 600;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
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
  background: var(--hover-overlay);
  color: var(--t2);
}
.nitem-btns button.del:hover {
  background: var(--danger-bg);
  color: var(--danger-text);
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

/* ── 用户入口 ─────────────────────────────────────────────────────── */
.sb-user {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 7px 8px;
  border-top: 1px solid var(--bd);
  margin-top: 2px;
  cursor: pointer;
  border-radius: 6px;
  transition: background 0.12s;
}

.sb-user:hover {
  background: var(--hv);
}

.sb-user-icon {
  width: 22px;
  height: 22px;
  border-radius: 50%;
  object-fit: cover;
  flex-shrink: 0;
}

.sb-user-avatar {
  width: 22px;
  height: 22px;
  border-radius: 50%;
  background: var(--ac);
  color: var(--t2);
  font-size: 11px;
  font-weight: 500;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.sb-user-name {
  flex: 1;
  font-size: 12px;
  color: var(--t2);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* ── Popover + Dialog（teleported，用 global）────────────────────── */

</style>

<style>
.sb-pop { min-width: 200px; padding: 4px 0; }
.sb-pop-user { display: flex; align-items: center; gap: 10px; padding: 10px 12px; cursor: pointer; transition: background 0.12s; }
.sb-pop-user:hover { background: var(--hv); }
.sb-pop-icon { width: 32px; height: 32px; border-radius: 50%; object-fit: cover; flex-shrink: 0; }
.sb-pop-avatar { width: 32px; height: 32px; border-radius: 50%; background: var(--ac); color: var(--t2); font-size: 14px; font-weight: 500; display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.sb-pop-info { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 2px; }
.sb-pop-name { font-size: 13px; font-weight: 500; color: var(--t1); }
.sb-pop-email { font-size: 11px; color: var(--t3); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.sb-pop-arrow { font-size: 11px; color: var(--t3); flex-shrink: 0; }
.sb-pop-divider { height: 1px; background: var(--bd); margin: 4px 0; }
.sb-pop-login { display: flex; align-items: center; gap: 8px; width: 100%; padding: 8px 12px; border: none; background: transparent; color: var(--t2); font-size: 12.5px; font-family: inherit; cursor: pointer; transition: background 0.12s; }
.sb-pop-login:hover { background: var(--hv); }
.sb-pop-logout { display: flex; align-items: center; gap: 8px; width: 100%; padding: 8px 12px; border: none; background: transparent; color: var(--danger-text); font-size: 12.5px; font-family: inherit; cursor: pointer; transition: background 0.12s; }
.sb-pop-logout:hover { background: var(--danger-bg); }

.sb-dialog { display: flex; flex-direction: column; align-items: center; gap: 16px; padding: 8px 0; }
.sb-dialog-header { display: flex; flex-direction: column; align-items: center; gap: 8px; }
.sb-dialog-icon { width: 56px; height: 56px; border-radius: 50%; object-fit: cover; }
.sb-dialog-avatar { width: 56px; height: 56px; border-radius: 50%; background: var(--ac); color: var(--t2); font-size: 22px; font-weight: 500; display: flex; align-items: center; justify-content: center; }
.sb-dialog-name { font-size: 16px; font-weight: 600; color: var(--t1); margin: 0; }
.sb-dialog-tag { font-size: 11px; padding: 2px 8px; border-radius: 10px; background: var(--hv); color: var(--t3); }
.sb-dialog-fields { width: 100%; display: flex; flex-direction: column; gap: 10px; }
.sb-dialog-row { display: flex; justify-content: space-between; align-items: center; padding: 0 4px; }
.sb-dialog-label { font-size: 12px; color: var(--t3); }
.sb-dialog-value { font-size: 12.5px; color: var(--t1); }
</style>
