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
      :model="[]"
    >
      <template #item="{ item, props, hasSubmenu, root }"></template>
      <template #end>
        <div class="flex items-center gap-2">
          <div @click="back" class="cursor-pointer">{{ t('common.back') }}</div>
          <button class="layout-topbar-action" @click="toggle">
            <Avatar
              v-if="user.user?.icon && user.user?.icon != './user.svg'"
              :image="resetUrl(user.user?.icon)"
            />
            <svg
              v-else
              class="user-default-icon"
              width="28"
              height="28"
              viewBox="0 0 512 512"
              xmlns="http://www.w3.org/2000/svg"
            >
              <g fill="currentColor" color="var(--p-primary-color)">
                <circle cx="256" cy="170" r="104" />
                <path
                  d="M256 300c-103 0-187 70-203 162a30 30 0 0 0 29.6 35h346.8a30 30 0 0 0 29.6-35c-16-92-100-162-203-162z"
                />
              </g>
            </svg>
          </button>
          <Menu ref="menuRef" :model="_buttons" :popup="true"></Menu>
        </div>
      </template>
    </Menubar>
  </div>
</template>
<script setup lang="ts">
import { useLayout } from '@/layout-plus/index'
import { ref } from 'vue'
import AppIcon from '@/assets/app.svg'
import useStore from '@/stores'
import { resetUrl } from '@/utils/common'
import { useRoute, useRouter } from 'vue-router'
import { t } from '@/locales'

const router = useRouter()
const { user } = useStore()
const back = () => {
  router.push({ name: 'application' })
}
const menuRef = ref()
const toggle = (event: any) => {
  menuRef.value.toggle(event)
}
const _buttons = [
  {
    label: t('topbar.logout'),
    icon: 'pi pi-sign-out',
    command: () => {
      user.logout()
      router.push({ name: 'login' })
    }
  }
]

const { toggleMenu, toggleDarkMode, isDarkTheme } = useLayout()
</script>
