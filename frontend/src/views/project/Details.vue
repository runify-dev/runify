<template>
  <TopHeaderVue root-route-name="projectDetails"> </TopHeaderVue>
  <div style="height: calc(100vh - 40px)">
    <router-view></router-view>
  </div>
</template>
<script setup lang="ts">
import { useRoute } from 'vue-router'
import Editor from '@/editor/index.vue'
const route = useRoute()
import TopHeaderVue from '@/layout/top-header/index.vue'
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { TreeCommonAPI } from '@/api/tree'
const treeCommonAPI = new TreeCommonAPI('note')
</script>
<style lang="scss" scoped></style>
