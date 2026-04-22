<template>
  <div class="layout-topbar">
    <button class="layout-menu-button layout-topbar-action" @click="toggleMenu">
      <i class="pi pi-th-large"></i>
    </button>
    <Menubar
      breakpoint="480px"
      :pt="{
        root: { style: 'border: none;', class: 'w-full' }
      }"
      :model="menus"
    >
      <template #start>
        <Avatar :image="AppIcon" style="width: 40px" />
      </template>
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
            >{{ item.shortcut }}</span
          >
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
        <div class="flex items-center gap-2">
          <Avatar
            class="cursor-pointer"
            @click="toggle"
            image="https://primefaces.org/cdn/primevue/images/avatar/amyelsner.png"
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
import { ref } from 'vue'
import AppIcon from '@/assets/app.svg'
import useStore from '@/stores'
import { useRoute, useRouter } from 'vue-router'
import { computed } from 'vue'
import { PermissionConstants } from '@/permission/data'
import { hasPermission } from '@/permission'
import { Role } from '@/permission/common'
const { user } = useStore()
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
const route = useRoute()
const router = useRouter()

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
  }
])
const menus = computed(() => {
  return items.value.filter((m: any) => {
    return hasPermission(m.permissions, 'OR')
  })
})
const { toggleMenu, toggleDarkMode, isDarkTheme } = useLayout()
</script>
