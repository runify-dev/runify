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
      :model="[]"
    >
      <template #start>
        <Avatar :image="AppIcon" style="width: 40px" />
      </template>
      <template #item="{ item, props, hasSubmenu, root }"> </template>
      <template #end>
        <div class="flex items-center gap-2">
          <div @click="back" class="cursor-pointer">返回工作台</div>
          <Avatar
            image="https://primefaces.org/cdn/primevue/images/avatar/amyelsner.png"
            shape="circle"
          />
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
const router = useRouter()
const { user } = useStore()
const back = () => {
  router.push({ name: 'application' })
}
const menuRef = ref()
const toggle = (event: any) => {
  menuRef.value.toggle(event)
}

const { toggleMenu, toggleDarkMode, isDarkTheme } = useLayout()
</script>
