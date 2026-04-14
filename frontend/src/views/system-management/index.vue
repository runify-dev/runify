<style lang="scss"></style>
<template>
  <div class="layout-wrapper" :class="containerClass">
    <AppTopbar />
    <AppMenuContent>
      <template #menu>
        <Menu
          :model="menus"
          class="w-full application-menu"
          :pt="{ root: { style: { border: 0 } } }"
        >
          <template #item="{ item, props }">
            <a
              v-ripple
              @click="to(item.name)"
              class="flex items-center"
              :class="item.name === route.name ? 'p-menu-item-selected' : ''"
              v-bind="props.action"
            >
              <span :class="item.icon" />
              <span>{{ item.label }}</span>
              <Badge v-if="item.badge" class="ml-auto" :value="item.badge" />
              <span
                v-if="item.shortcut"
                class="ml-auto border border-surface rounded bg-emphasis text-muted-color text-xs p-1"
                >{{ item.shortcut }}</span
              >
            </a>
          </template></Menu
        >
      </template>
      <RouterView></RouterView>
    </AppMenuContent>
    <div class="layout-main-container"></div>
    <div class="layout-mask animate-fadein" @click="hideMobileMenu" />
  </div>
  <Toast />
</template>
<script setup lang="ts">
import AppMenuContent from '@/layout-plus/app-menu-content/index.vue'
import { useLayout } from '@/layout-plus/index'
import { computed, ref } from 'vue'
import AppTopbar from '@/layout-plus/app-system-topbar/index.vue'
import { useRoute, useRouter } from 'vue-router'
const route = useRoute()
const router = useRouter()
const { layoutConfig, layoutState, hideMobileMenu } = useLayout()
const to = (name: string) => {
  router.push({ name: name })
}
const containerClass = computed(() => {
  return {
    'layout-overlay': layoutConfig.menuMode === 'overlay',
    'layout-static': layoutConfig.menuMode === 'static',
    'layout-overlay-active': layoutState.overlayMenuActive,
    'layout-mobile-active': layoutState.mobileMenuActive,
    'layout-static-inactive': layoutState.staticMenuInactive
  }
})

const menus = ref([
  {
    label: '用户管理',
    name: 'user-management',
    icon: 'pi pi-cog'
  }
])
</script>
