<template>
  <div class="layout-wrapper" :class="containerClass">
    <AppTopbar />
    <router-view />
    <div class="layout-main-container">
      <!-- <AppMenuContent
        ><template #menu>
          <AppSidebar></AppSidebar>
        </template>
        <router-view
      /></AppMenuContent> -->
    </div>
    <div class="layout-mask animate-fadein" @click="hideMobileMenu" />
  </div>
  <Toast />
</template>
<script setup lang="ts">
import { useLayout } from '@/layout-plus/index'
import { computed } from 'vue'
import AppFooter from './app-footer/index.vue'
import AppTopbar from './app-topbar/index.vue'

const { layoutConfig, layoutState, hideMobileMenu } = useLayout()

const containerClass = computed(() => {
  return {
    'layout-overlay': layoutConfig.menuMode === 'overlay',
    'layout-static': layoutConfig.menuMode === 'static',
    'layout-overlay-active': layoutState.overlayMenuActive,
    'layout-mobile-active': layoutState.mobileMenuActive,
    'layout-static-inactive': layoutState.staticMenuInactive
  }
})
</script>
