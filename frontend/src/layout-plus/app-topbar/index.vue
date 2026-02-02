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
      :model="items"
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
import { useRoute, useRouter } from 'vue-router'
const route = useRoute()
const router = useRouter()

const items = ref([
  {
    name: 'application',
    label: '应用',
    icon: 'pi pi-home'
  },
  {
    label: '笔记',
    icon: 'pi pi-home',
    name: 'note'
  },
  {
    label: '模型',
    icon: 'pi pi-home',
    name: 'model'
  },
  {
    label: '项目',
    icon: 'pi pi-search',
    name: 'project'
  }
])
const { toggleMenu, toggleDarkMode, isDarkTheme } = useLayout()
</script>
