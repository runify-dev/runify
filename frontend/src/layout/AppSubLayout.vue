<template>
  <el-container class="note-layout">
    <AppSubAside>
      <slot name="aside"></slot>
    </AppSubAside>

    <div class="main-contaner">
      <slot name="main"></slot>
    </div>

  </el-container>
</template>
<script setup lang="ts">
import AppSubAside from "@/layout/components/AppSubAside.vue";
</script>
<style lang="scss" scoped>
.main-contaner {
  width: 100%;
  height: 100%;
}
</style>
