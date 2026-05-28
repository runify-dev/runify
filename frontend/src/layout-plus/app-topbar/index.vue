<template>
  <div class="layout-topbar">
    <button class="layout-menu-button layout-topbar-action" @click="toggleMenu">
      <Avatar :image="AppIcon" style="width: 40px" />
    </button>
    <Menubar
      breakpoint="480px"
      :pt="{
        root: { style: 'border: none;', class: 'w-full' }
      }"
      :model="menus"
    >
      <template #item="{ item, props, hasSubmenu, root }">
        <a
          @click="router.push({ name: item.name })"
          v-ripple
          class="flex items-center"
          :class="item.name === route.meta.activeMenu ? 'p-menubar-item-selected' : ''"
          v-bind="props.action"
        >
          <span>{{ item.label }}</span>
          <Badge
            v-if="item.badge"
            :class="{ 'ml-auto': !root, 'ml-2': root }"
            :value="item.badge"
          />
          <span
            v-if="item.shortcut"
            class="ml-auto border border-surface rounded bg-emphasis text-muted-color text-xs p-1"
          >{{ item.shortcut }}</span>
          <i
            v-if="hasSubmenu"
            :class="[
              'pi pi-angle-down ml-auto',
              { 'pi-angle-down': root, 'pi-angle-right': !root }
            ]"
          ></i>
        </a>
      </template>
      <template #end>
        <div class="flex items-center gap-1">
          <!-- 暗色模式 -->
          <button class="topbar-btn" @click="toggleDarkMode" :title="isDarkTheme ? '切换亮色' : '切换暗色'">
            <i :class="isDarkTheme ? 'pi pi-sun' : 'pi pi-moon'"></i>
          </button>
          <!-- 主题配置 -->
          <div ref="themePickerRef" class="relative">
            <button class="topbar-btn" :style="paletteBtnStyle" @click.stop="showThemePicker = !showThemePicker" title="主题设置">
              <i class="pi pi-palette"></i>
            </button>
            <Transition name="palette">
              <div v-if="showThemePicker" class="palette-panel" @click.stop>
                <!-- Primary -->
                <div class="palette-section">
                  <div class="palette-label">Primary</div>
                  <div class="palette-row">
                    <button
                      v-for="c in primaryColors"
                      :key="c.name"
                      class="swatch"
                      :class="{ 'swatch--active': activePrimary === c.name }"
                      :style="{ background: c.preview }"
                      :title="c.label"
                      @click="onPrimaryChange(c)"
                    ></button>
                    <!-- 自定义色 -->
                    <label
                      class="swatch swatch--custom"
                      :class="{ 'swatch--active': activePrimary === '__custom__' }"
                      :style="activePrimary === '__custom__' ? { background: customPrimaryHex } : {}"
                      title="自定义颜色"
                    >
                      <input type="color" :value="customPrimaryHex" @input="onCustomPrimaryInput" />
                      <i v-if="activePrimary !== '__custom__'" class="pi pi-plus"></i>
                    </label>
                  </div>
                </div>
                <!-- Surface -->
                <div class="palette-section">
                  <div class="palette-label">Surface</div>
                  <div class="palette-row">
                    <button
                      v-for="s in surfaceColors"
                      :key="s.name"
                      class="swatch swatch--surface"
                      :class="{ 'swatch--active': activeSurface === s.name }"
                      :style="{ background: s.preview }"
                      :title="s.label"
                      @click="onSurfaceChange(s)"
                    ></button>
                  </div>
                </div>
              </div>
            </Transition>
          </div>
          <!-- 用户头像 -->
          <Avatar
            class="cursor-pointer ml-1"
            @click="toggle"
            :image="user.user?.icon ? resetUrl(user.user?.icon) : ''"
            shape="circle"
          />
          <Menu ref="menuRef" :model="_buttons" :popup="true"></Menu>
        </div>
      </template>
    </Menubar>
  </div>
</template>

<script setup lang="ts">
import { useLayout } from '@/layout-plus/index'
import { ref, computed, onMounted, onUnmounted } from 'vue'
import AppIcon from '@/assets/app.svg'
import useStore from '@/stores'
import { useRoute, useRouter } from 'vue-router'
import { PermissionConstants } from '@/permission/data'
import { hasPermission } from '@/permission'
import { Role } from '@/permission/common'
import { resetUrl } from '@/utils/common'
import { updatePrimaryPalette, updateSurfacePalette, palette } from '@primeuix/themes'
import type { PaletteDesignToken } from '@primeuix/themes/types'

const { user } = useStore()
const route = useRoute()
const router = useRouter()
const { toggleMenu, toggleDarkMode, isDarkTheme } = useLayout()

// ===================== 用户菜单 =====================
const menuRef = ref()
const toggle = (event: any) => {
  menuRef.value.toggle(event)
}

const buttons = ref([
  {
    label: '系统设置',
    icon: 'pi pi-cog',
    command: () => {
      router.push({ name: 'system-management' })
    },
    permissions: [
      PermissionConstants.ROLE_MANAGEMENT_READ,
      PermissionConstants.USER_MANAGEMENT_READ,
      Role.ADMIN
    ]
  },
  {
    label: '退出登录',
    icon: 'pi pi-sign-out',
    command: () => {
      user.logout()
      router.push({ name: 'login' })
    }
  }
])

const _buttons = computed(() => {
  return buttons.value.filter((m: any) => {
    if (m.permissions) {
      return hasPermission(m.permissions, 'OR')
    }
    return true
  })
})

// ===================== 导航菜单 =====================
const items = ref([
  {
    name: 'application',
    label: '应用',
    icon: 'pi pi-home',
    permissions: [PermissionConstants.APPLICATION_READ, Role.ADMIN, Role.USER]
  },
  {
    label: '笔记',
    icon: 'pi pi-home',
    name: 'note',
    permissions: [PermissionConstants.NOTE_READ, Role.ADMIN, Role.USER]
  },
  {
    label: '模型',
    icon: 'pi pi-home',
    name: 'model',
    permissions: [PermissionConstants.MODEL_READ, Role.ADMIN, Role.USER]
  },
  {
    label: '项目',
    icon: 'pi pi-search',
    name: 'project',
    permissions: [PermissionConstants.PROJECT_READ, Role.ADMIN, Role.USER]
  },
  {
    label: '数据源',
    icon: 'pi pi-database',
    name: 'datasource',
    permissions: [PermissionConstants.PROJECT_READ, Role.ADMIN, Role.USER]
  }
])

const menus = computed(() => {
  return items.value.filter((m: any) => {
    return hasPermission(m.permissions, 'OR')
  })
})

// ===================== 主题 =====================
const showThemePicker = ref(false)
const themePickerRef = ref<HTMLElement | null>(null)

interface ColorOption {
  name: string
  label: string
  preview: string
}

// 17 色 — 覆盖 PrimeVue 所有内置 primitive，按色相环排列
const primaryColors: ColorOption[] = [
  { name: 'rose',    label: '玫瑰',   preview: '#f43f5e' },
  { name: 'pink',    label: '蔷薇',   preview: '#ec4899' },
  { name: 'fuchsia', label: '品红',   preview: '#d946ef' },
  { name: 'purple',  label: '深紫',   preview: '#a855f7' },
  { name: 'violet',  label: '紫罗兰', preview: '#8b5cf6' },
  { name: 'indigo',  label: '靛蓝',   preview: '#6366f1' },
  { name: 'blue',    label: '经典蓝', preview: '#3b82f6' },
  { name: 'sky',     label: '天蓝',   preview: '#0ea5e9' },
  { name: 'cyan',    label: '天青',   preview: '#06b6d4' },
  { name: 'teal',    label: '水鸭青', preview: '#14b8a6' },
  { name: 'emerald', label: '碧翠',   preview: '#10b981' },
  { name: 'green',   label: '翠绿',   preview: '#22c55e' },
  { name: 'lime',    label: '青柠',   preview: '#84cc16' },
  { name: 'yellow',  label: '柠黄',   preview: '#eab308' },
  { name: 'amber',   label: '琥珀',   preview: '#f59e0b' },
  { name: 'orange',  label: '橘橙',   preview: '#f97316' },
  { name: 'red',     label: '赤红',   preview: '#ef4444' }
]

const surfaceColors: ColorOption[] = [
  { name: 'slate',   label: 'Slate',   preview: '#64748b' },
  { name: 'gray',    label: 'Gray',    preview: '#6b7280' },
  { name: 'zinc',    label: 'Zinc',    preview: '#71717a' },
  { name: 'neutral', label: 'Neutral', preview: '#737373' },
  { name: 'stone',   label: 'Stone',   preview: '#78716c' }
]

const DEFAULT_PRIMARY = 'emerald'
const DEFAULT_SURFACE = 'zinc'

const activePrimary = ref(localStorage.getItem('theme-primary') || DEFAULT_PRIMARY)
const activeSurface = ref(localStorage.getItem('theme-surface') || DEFAULT_SURFACE)
const customPrimaryHex = ref(localStorage.getItem('theme-custom-primary') || '#6366f1')

const paletteBtnStyle = computed(() => {
  if (activePrimary.value === '__custom__') {
    return { background: customPrimaryHex.value, color: '#fff', borderColor: customPrimaryHex.value }
  }
  const found = primaryColors.find(c => c.name === activePrimary.value)
  if (found) {
    return { background: found.preview, color: '#fff', borderColor: found.preview }
  }
  return {}
})

function buildPaletteTokens(colorName: string) {
  return {
    50:  `{${colorName}.50}`,
    100: `{${colorName}.100}`,
    200: `{${colorName}.200}`,
    300: `{${colorName}.300}`,
    400: `{${colorName}.400}`,
    500: `{${colorName}.500}`,
    600: `{${colorName}.600}`,
    700: `{${colorName}.700}`,
    800: `{${colorName}.800}`,
    900: `{${colorName}.900}`,
    950: `{${colorName}.950}`
  }
}

const onPrimaryChange = (c: ColorOption) => {
  activePrimary.value = c.name
  localStorage.setItem('theme-primary', c.name)
  updatePrimaryPalette(buildPaletteTokens(c.name))
}

let debounceTimer: ReturnType<typeof setTimeout> | null = null
const onCustomPrimaryInput = (e: Event) => {
  const hex = (e.target as HTMLInputElement).value
  customPrimaryHex.value = hex
  if (debounceTimer) clearTimeout(debounceTimer)
  debounceTimer = setTimeout(() => {
    activePrimary.value = '__custom__'
    localStorage.setItem('theme-primary', '__custom__')
    localStorage.setItem('theme-custom-primary', hex)
    updatePrimaryPalette(palette(hex) as PaletteDesignToken)
  }, 30)
}

const onSurfaceChange = (s: ColorOption) => {
  activeSurface.value = s.name
  localStorage.setItem('theme-surface', s.name)
  updateSurfacePalette(buildPaletteTokens(s.name))
}

// 初始化
const initSavedTheme = () => {
  if (activePrimary.value === '__custom__') {
    updatePrimaryPalette(palette(customPrimaryHex.value) as PaletteDesignToken)
  } else if (activePrimary.value !== DEFAULT_PRIMARY) {
    updatePrimaryPalette(buildPaletteTokens(activePrimary.value))
  }
  if (activeSurface.value !== DEFAULT_SURFACE) {
    updateSurfacePalette(buildPaletteTokens(activeSurface.value))
  }
}
initSavedTheme()

// 点击外部关闭
const onClickOutside = (e: MouseEvent) => {
  if (showThemePicker.value && themePickerRef.value && !themePickerRef.value.contains(e.target as Node)) {
    showThemePicker.value = false
  }
}
onMounted(() => document.addEventListener('click', onClickOutside, true))
onUnmounted(() => document.removeEventListener('click', onClickOutside, true))
</script>

<style scoped>
/* ========== Topbar 按钮（参考 primevue.org header） ========== */
.topbar-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 2rem;
  height: 2rem;
  border-radius: 50%;
  border: 1px solid var(--p-content-border-color);
  background: var(--p-content-background);
  color: var(--p-text-muted-color);
  cursor: pointer;
  transition: background 0.2s, color 0.2s, border-color 0.2s;
  font-size: 0.875rem;
}
.topbar-btn:hover {
  background: var(--p-primary-color);
  color: var(--p-primary-contrast-color, #fff);
  border-color: var(--p-primary-color);
}

/* ========== 调色板面板 ========== */
.palette-panel {
  position: absolute;
  right: 0;
  top: calc(100% + 8px);
  z-index: 100;
  width: 264px;
  padding: 1rem;
  border-radius: 12px;
  background: var(--p-content-background);
  border: 1px solid var(--p-content-border-color);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.08), 0 2px 8px rgba(0, 0, 0, 0.04);
  display: flex;
  flex-direction: column;
  gap: 0.875rem;
}

.palette-section {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.palette-label {
  font-size: 0.75rem;
  font-weight: 600;
  color: var(--p-text-muted-color);
}

.palette-row {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

/* ========== 色块（跟 primevue.org 一致的圆形 swatch） ========== */
.swatch {
  position: relative;
  width: 1.25rem;
  height: 1.25rem;
  border-radius: 50%;
  border: none;
  padding: 0;
  cursor: pointer;
  outline: none;
  transition: transform 0.15s, box-shadow 0.15s;
}
.swatch:hover {
  transform: scale(1.2);
}
.swatch:focus-visible {
  box-shadow: 0 0 0 2px var(--p-content-background), 0 0 0 4px var(--p-primary-color);
}
.swatch--active {
  box-shadow: 0 0 0 2px var(--p-content-background), 0 0 0 4px var(--p-primary-color);
  transform: scale(1.1);
}

/* Surface 色块稍微加个内描边，区分深浅灰 */
.swatch--surface {
  box-shadow: inset 0 0 0 1px rgba(0, 0, 0, 0.08);
}
.swatch--surface.swatch--active {
  box-shadow: inset 0 0 0 1px rgba(0, 0, 0, 0.08),
              0 0 0 2px var(--p-content-background),
              0 0 0 4px var(--p-primary-color);
}

/* ========== 自定义颜色 ========== */
.swatch--custom {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: var(--p-content-background);
  border: 1.5px dashed var(--p-surface-300);
  cursor: pointer;
  overflow: hidden;
}
.swatch--custom:hover {
  border-color: var(--p-primary-color);
}
.swatch--custom.swatch--active {
  border-style: solid;
  border-color: transparent;
}
.swatch--custom input[type="color"] {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  opacity: 0;
  cursor: pointer;
  border: none;
  padding: 0;
}
.swatch--custom i {
  font-size: 0.55rem;
  color: var(--p-text-muted-color);
  pointer-events: none;
}
.swatch--custom:hover i {
  color: var(--p-primary-color);
}

/* ========== 面板动画 ========== */
.palette-enter-active {
  transition: opacity 0.2s ease, transform 0.2s cubic-bezier(0.34, 1.56, 0.64, 1);
}
.palette-leave-active {
  transition: opacity 0.12s ease, transform 0.12s ease;
}
.palette-enter-from {
  opacity: 0;
  transform: translateY(-6px) scale(0.97);
}
.palette-leave-to {
  opacity: 0;
  transform: translateY(-3px) scale(0.98);
}
</style>
