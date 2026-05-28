<template>
  <div class="cw">
    <!-- 遮罩：统一在这里管理，点击关闭侧边栏 -->
    <div v-if="open" class="mask" @click="open = false" />

    <SideBar :type="type" v-model:open="open" v-model:mode="mode" />
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

</script>

<style>
.cw {
  /* ── 尺寸 ──────────────────────────────────────────────────────── */
  --sb-w: 260px;

  /* ── 主题变量（映射到 PrimeVue） ──────────────────────────────── */
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
  --mask: rgba(0, 0, 0, 0.4);
  --shadow: 0 1px 3px rgba(0, 0, 0, 0.2);
  --danger-bg: rgba(239, 68, 68, 0.15);
  --danger-text: #ef4444;
  --focus-border: var(--p-primary-color);
  --hover-overlay: rgba(255, 255, 255, 0.06);

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
