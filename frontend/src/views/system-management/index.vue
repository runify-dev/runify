<template>
  <TopHeaderVue root-route-name="system-management"> </TopHeaderVue>
  <div style="height: calc(100vh - 40px)">
    <router-view></router-view>
  </div>
</template>
<script setup lang="ts">
import TopHeaderVue from '@/layout/top-header/index.vue'
</script>
<style lang="scss"></style>
