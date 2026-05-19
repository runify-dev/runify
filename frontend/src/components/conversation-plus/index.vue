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
  --sb-w: 260px;

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

  /* CodeMirror / PrimeVue 暗色主题变量 */
  --p-surface-0: #191919;
  --p-surface-50: #1f1f1f;
  --p-surface-100: #252525;
  --p-surface-200: #2a2a2a;
  --p-surface-300: #333333;
  --p-surface-400: #444444;
  --p-surface-500: #555555;
  --p-surface-600: #666666;
  --p-surface-700: #777777;
  --p-surface-800: #888888;
  --p-surface-900: #999999;
  --p-surface-950: #aaaaaa;
  --p-surface-hover: #2a2a2a;
  --p-content-background: #1f1f1f;
  --p-content-border-color: #2e2e2e;
  --p-content-border-radius: 8px;
  --p-content-hover-background: #2a2a2a;
  --p-overlay-popover-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
  --p-text-color: #e0e0e0;
  --p-text-muted-color: #a0a0a0;
  --p-transition-duration: 0.2s;
  --p-shadow-1: 0 1px 3px rgba(0, 0, 0, 0.3);
  --p-shadow-2: 0 4px 12px rgba(0, 0, 0, 0.3);
  --p-primary-50: #0d2137;
  --p-primary-100: #1a3a5c;
  --p-primary-200: #1e4a7a;
  --p-primary-300: #2a5f9a;
  --p-primary-400: #3d7ab8;
  --p-primary-500: #5090d0;
  --p-primary-600: #6ba8e0;
  --p-primary-700: #7db3e0;
  --p-primary-800: #a0c8ea;
  --p-primary-900: #c0daf0;
  --p-primary-950: #e0ecf8;
  --p-primary-color: #5090d0;
  --p-primary-contrast-color: #ffffff;

  /* 颜色变量 */
  --p-blue-100: #1a3a5c;
  --p-blue-200: #1e4a7a;
  --p-blue-400: #3d7ab8;
  --p-blue-500: #5090d0;
  --p-blue-800: #a0c8ea;
  --p-blue-900: #c0daf0;
  --p-green-50: #0d3320;
  --p-green-100: #1a5c3a;
  --p-green-200: #1e7a4a;
  --p-green-300: #2a9a5f;
  --p-green-400: #3db87a;
  --p-green-500: #50d090;
  --p-green-600: #6be0a8;
  --p-green-700: #7de0b3;
  --p-green-800: #a0eac8;
  --p-green-900: #c0f0da;
  --p-green-950: #e0f8ec;
  --p-red-50: #3b1111;
  --p-red-100: #5c1a1a;
  --p-red-200: #7a1e1e;
  --p-red-300: #9a2a2a;
  --p-red-400: #d05050;
  --p-red-500: #e06060;
  --p-red-600: #e87070;
  --p-red-700: #f08080;
  --p-red-800: #f5a0a0;
  --p-red-900: #f8c0c0;
  --p-red-950: #fce0e0;
  --p-orange-100: #5c3a1a;
  --p-orange-200: #7a4a1e;
  --p-orange-400: #b87a3d;
  --p-orange-500: #d09050;
  --p-orange-800: #eac8a0;
  --p-orange-900: #f0dac0;
  --p-yellow-50: #332b0d;
  --p-yellow-100: #5c4f1a;
  --p-yellow-200: #7a6a1e;
  --p-yellow-300: #9a8a2a;
  --p-yellow-400: #b8a83d;
  --p-yellow-500: #d0c050;
  --p-yellow-600: #e0d06b;
  --p-yellow-700: #e0d87d;
  --p-yellow-800: #eae0a0;
  --p-yellow-900: #f0e8c0;
  --p-yellow-950: #f8f0e0;
  --p-cyan-300: #2a9a9a;
  --p-cyan-500: #50d0d0;
  --p-teal-400: #3db8b8;
  --p-pink-100: #5c1a3a;
  --p-pink-200: #7a1e4a;
  --p-pink-400: #b83d7a;
  --p-pink-500: #d05090;
  --p-pink-800: #eaa0c8;
  --p-pink-900: #f0c0d8;
  --p-slate-400: #666666;
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
