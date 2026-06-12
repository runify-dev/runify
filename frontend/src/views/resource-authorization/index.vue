<template>
  <el-tabs
    v-model="resource"
    tab-position="left"
    class="demo-tabs"
    style="height: 100%"
    @tab-click="handleClick"
  >
    <el-tab-pane :label="t('common.appName')" name="application"
      ><ResourceAuthorzation :resource="resource"></ResourceAuthorzation>
    </el-tab-pane>
    <el-tab-pane :label="t('knowledge.knowledgeLabel')" name="knowlegde">{{ t('knowledge.knowledgeLabel') }}</el-tab-pane>
    <el-tab-pane :label="t('model.modelLabel')" name="model">{{ t('model.modelLabel') }}</el-tab-pane>
  </el-tabs>
</template>
<script setup lang="ts">
import { ref } from 'vue'
import { t } from '@/locales'
import ResourceAuthorzation from './components/ResourceAuthorzation.vue'
import type { Resource } from '@/api/type/common'
const resource = ref<Resource>()
const handleClick = (v: Resource) => {
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
