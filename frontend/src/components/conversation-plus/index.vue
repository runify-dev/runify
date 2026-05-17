<template>
  <div class="cw" :class="{ dark: isDark }">
    <!-- 遮罩：统一在这里管理，点击关闭侧边栏 -->
    <div v-if="open" class="mask" @click="open = false" />

    <SideBar :type="type" v-model:open="open" v-model:mode="mode" v-model:isDark="isDark" />
    <ChatPanel @toggle="open = !open" :type="type" @close="$emit('close')">
      <template #header>
        <template v-if="$slots.header">
          <slot name="header"></slot>
        </template>
      </template>
    </ChatPanel>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import SideBar from './sidebar/index.vue'
import ChatPanel from './chat-panel/index.vue'
defineEmits(['close'])

type OpenMode = boolean | 'auto'
type LayoutMode = 'push' | 'drawer' | 'auto'

const props = withDefaults(
  defineProps<{
    defaultOpen?: OpenMode
    defaultMode?: LayoutMode
    type: 'DEBUG' | 'CONVERSATION' | 'ADMIN_CONVERSATION'
  }>(),
  {
    defaultOpen: 'auto',
    defaultMode: 'auto'
  }
)

const BREAKPOINT = 768
const isMobile = ref(window.innerWidth < BREAKPOINT)

const updateMobile = () => {
  isMobile.value = window.innerWidth < BREAKPOINT
}

onMounted(() => window.addEventListener('resize', updateMobile))
onUnmounted(() => window.removeEventListener('resize', updateMobile))

const mode = ref<'push' | 'drawer'>(
  props.defaultMode === 'auto'
    ? isMobile.value ? 'drawer' : 'push'
    : props.defaultMode
)

// 监听 auto 模式下屏幕变化，更新 mode
if (props.defaultMode === 'auto') {
  watch(isMobile, (mobile) => {
    mode.value = mobile ? 'drawer' : 'push'
  })
}

const open = ref(
  props.defaultOpen === 'auto' ? !isMobile.value : props.defaultOpen
)

// 监听 auto 模式下屏幕变化，更新 open
if (props.defaultOpen === 'auto') {
  watch(isMobile, (mobile) => {
    open.value = !mobile
  })
}

const isDark = ref(false)
</script>

<style>
.cw {
  /* ── 尺寸 ──────────────────────────────────────────────────────── */
  --sb-w: 200px;

  /* ── 浅色主题 ──────────────────────────────────────────────────── */
  --bg: #fafafa;
  --bg2: #f5f5f3;
  --bd: #e5e5e3;
  --t1: #37352f;
  --t2: #6b6b6b;
  --t3: #9b9b9b;
  --hv: #efefed;
  --ac: #e8e8e6;
  --ub: #37352f;
  --ab: #f0f0ee;
  --mask: rgba(0, 0, 0, 0.25);
  --shadow: rgba(0, 0, 0, 0.06);
  --danger-bg: #fef2f2;
  --danger-text: #dc2626;
  --focus-border: #c0c0c0;
  --hover-overlay: rgba(0, 0, 0, 0.04);

  container-type: inline-size;
  container-name: chat;

  position: relative;
  display: flex;
  width: 100%;
  height: 100%;
  overflow: hidden;
  font-family: 'PingFang SC', 'Noto Sans SC', system-ui, sans-serif;
  background: var(--bg);
  color: var(--t1);
}

/* ── 深色主题 ─────────────────────────────────────────────────────── */
.dark.cw {
  --bg: #191919;
  --bg2: #1f1f1f;
  --bd: #2e2e2e;
  --t1: #e0e0e0;
  --t2: #a0a0a0;
  --t3: #666666;
  --hv: #252525;
  --ac: #2a2a2a;
  --ub: #e0e0e0;
  --ab: #232323;
  --mask: rgba(0, 0, 0, 0.4);
  --shadow: rgba(0, 0, 0, 0.2);
  --danger-bg: #3b1111;
  --danger-text: #f87171;
  --focus-border: #555555;
  --hover-overlay: rgba(255, 255, 255, 0.06);
}

/* ── 遮罩 ──────────────────────────────────────────────────────────── */
.cw > .mask {
  display: none;
  position: absolute;
  inset: 0;
  z-index: 30;
  background: var(--mask);
}

/* drawer 模式：始终显示遮罩 */
.cw:has(.sb--drawer.sb--open) > .mask {
  display: block;
}

/* ── 窄屏适配：push 模式强制变 drawer ─────────────────────────────── */
@container chat (max-width: 300px) {
  .cw > .mask {
    display: block;
  }

  .sb--push {
    position: absolute;
    top: 0;
    left: 0;
    bottom: 0;
    width: var(--sb-w);
    min-width: var(--sb-w);
    border-right-width: 1px;
    pointer-events: none;
    transform: translateX(-100%);
    z-index: 40;
    transition:
      transform 0.25s cubic-bezier(0.4, 0, 0.2, 1),
      box-shadow 0.25s;
  }
  .sb--push.sb--open {
    pointer-events: auto;
    transform: translateX(0);
    box-shadow: 4px 0 16px var(--shadow);
    width: var(--sb-w);
  }

  /* 窄屏 push 未展开时遮罩也不需要 */
  .cw:not(:has(.sb--open)) > .mask {
    display: none;
  }
}
</style>
