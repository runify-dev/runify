<template>
  <el-tabs
    v-model="resource"
    tab-position="left"
    class="demo-tabs"
    style="height: 100%"
    @tab-click="handleClick"
  >
    <el-tab-pane label="应用" name="application"
      ><ResourceAuthorzation :resource="resource"></ResourceAuthorzation>
    </el-tab-pane>
    <el-tab-pane label="知识库" name="knowlegde">知识库</el-tab-pane>
    <el-tab-pane label="模型" name="model">模型</el-tab-pane>
  </el-tabs>
</template>
<script setup lang="ts">
import { ref } from 'vue'
import ResourceAuthorzation from './components/ResourceAuthorzation.vue'
const resource = ref<string>()
const handleClick = (v: string) => {
  resource.value = v
}
</script>
<style lang="scss" scoped>
.demo-tabs {
  :deep(.el-tabs__header.is-left) {
    margin-right: 0px;
  }
}
</style>
