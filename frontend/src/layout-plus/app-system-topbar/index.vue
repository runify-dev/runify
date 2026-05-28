<template>
  <div class="layout-topbar">
    <button class="layout-menu-button layout-topbar-action" @click="toggleMenu">
      <Avatar :image="AppIcon" style="width: 40px"/>
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
          <div @click="back" class="cursor-pointer">返回工作台</div>
          <Avatar
            class="cursor-pointer"
            @click="toggle"
            :image="user.user?.icon?resetUrl(user.user?.icon):''"
            shape="circle"
          />
          <Menu ref="menuRef" :model="_buttons" :popup="true"></Menu>
        </div>
      </template>
    </Menubar>
  </div>
</template>
<script setup lang="ts">
import {useLayout} from '@/layout-plus/index'
import {ref} from 'vue'
import AppIcon from '@/assets/app.svg'
import useStore from '@/stores'
import {resetUrl} from "@/utils/common"
import {useRoute, useRouter} from 'vue-router'

const router = useRouter()
const {user} = useStore()
const back = () => {
  router.push({name: 'application'})
}
const menuRef = ref()
const toggle = (event: any) => {
  menuRef.value.toggle(event)
}
const _buttons=[ {
  label: '退出登录',
  icon: 'pi pi-sign-out',
  command: () => {
    user.logout()
    router.push({ name: 'login' })
  }
}];

const {toggleMenu, toggleDarkMode, isDarkTheme} = useLayout()
</script>
