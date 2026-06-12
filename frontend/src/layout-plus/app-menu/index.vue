<script setup>
import { computed } from 'vue'
import AppMenuItem from './AppMenuItem.vue'
import { t } from '@/locales'

const model = computed(() => [
  {
    label: t('application.overview'),
    icon: 'pi pi-fw pi-objects-column',
    to: '/',
    shortcut: '⌘+N'
  },
  {
    label: t('application.settings'),
    icon: 'pi pi-fw pi-cog',
    to: '/',
    shortcut: '⌘+N'
  },
  {
    label: t('application.conversationLog'),
    icon: 'pi pi-fw pi-file',
    to: '/',
    shortcut: '⌘+N'
  }
])
</script>

<template>
  <Menu :model="model" class="w-full" :pt="{ root: { style: { border: 0 } } }">
    <template #submenulabel="{ item }">
      <span class="text-primary font-bold">{{ item.label }}</span>
    </template>
    <template #item="{ item, props }">
      <a v-ripple class="flex items-center" v-bind="props.action">
        <span :class="item.icon" />
        <span>{{ item.label }}</span>
        <Badge v-if="item.badge" class="ml-auto" :value="item.badge" />
        <span
          v-if="item.shortcut"
          class="ml-auto border border-surface rounded bg-emphasis text-muted-color text-xs p-1"
          >{{ item.shortcut }}</span
        >
      </a>
    </template>
  </Menu>
</template>

<style lang="scss" scoped></style>
