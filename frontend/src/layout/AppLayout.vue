<template>
  <el-container class="app-layout">
    <AppAside></AppAside>
    <div class="content-container">
      <router-view />
    </div>

  </el-container>
</template>
<script setup lang="ts">
import AppAside from "@/layout/components/app-aside/index.vue";
</script>
<style lang="scss">
@import url(@/layout/index.scss);
</style>
