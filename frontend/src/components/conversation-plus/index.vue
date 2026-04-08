<template>
  <div class="cw" :class="{ dark: isDark }">
    <!-- 遮罩：统一在这里管理，点击关闭侧边栏 -->
    <div v-if="open" class="mask" @click="open = false" />

    <SideBar v-model:open="open" v-model:mode="mode" v-model:isDark="isDark" />
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
import { ref } from 'vue'
import SideBar from './sidebar/index.vue'
import ChatPanel from './chat-panel/index.vue'
defineEmits(['close'])

const props = withDefaults(
  defineProps<{
    defaultOpen?: boolean
    defaultMode?: 'push' | 'drawer'
    type: 'DEBUG' | 'CONVERSATION'
  }>(),
  {
    defaultOpen: false,
    defaultMode: 'push'
  }
)

const open = ref(props.defaultOpen)
const mode = ref<'push' | 'drawer'>(props.defaultMode)
const isDark = ref(false)
</script>

<style>
.cw {
  --sb-w: 200px;
  --bg: #ffffff;
  --bg2: #f7f7f5;
  --bd: #eaeaea;
  --t1: #1a1a1a;
  --t2: #555555;
  --t3: #aaaaaa;
  --hv: #eeeeec;
  --ac: #e9e9e7;
  --ub: #1a1a1a;
  --ab: #f5f5f3;

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
.dark.cw {
  --bg: #111111;
  --bg2: #181818;
  --bd: #252525;
  --t1: #f0f0f0;
  --t2: #bbbbbb;
  --t3: #555555;
  --hv: #222222;
  --ac: #252525;
  --ub: #efefef;
  --ab: #1d1d1d;
}

/* 遮罩基础：默认隐藏（push 模式宽屏不需要） */
.cw > .mask {
  display: none;
  position: absolute;
  inset: 0;
  z-index: 30;
  background: rgba(0, 0, 0, 0.35);
}

/* drawer 模式：始终显示遮罩 */
.cw:has(.sb--drawer.sb--open) > .mask {
  display: block;
}

/* 容器宽度 < 500px：push 模式强制变 drawer 行为，同时显示遮罩 */
@container chat (max-width: 300px) {
  .cw > .mask {
    display: block;
  }

  .sb--push {
    position: absolute !important;
    top: 0;
    left: 0;
    bottom: 0;
    width: var(--sb-w) !important;
    min-width: var(--sb-w) !important;
    border-right-width: 1px !important;
    pointer-events: none;
    transform: translateX(-100%);
    z-index: 40 !important;
    transition:
      transform 0.25s cubic-bezier(0.4, 0, 0.2, 1),
      box-shadow 0.25s !important;
  }
  .sb--push.sb--open {
    pointer-events: auto;
    transform: translateX(0) !important;
    box-shadow: 4px 0 16px rgba(0, 0, 0, 0.1);
    /* push 的 width 动画在此失效，确保不占文档流 */
    width: var(--sb-w) !important;
  }

  /* 窄屏 push 未展开时遮罩也不需要 */
  .cw:not(:has(.sb--open)) > .mask {
    display: none;
  }
}
</style>
