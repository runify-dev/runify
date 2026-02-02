<template>
  <Panel header="连接信息">
    <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
      <div class="space-y-1">
        <div class="font-bold text-gray-600">主机</div>
        <div>{{ row.meta.collection.host }}</div>
      </div>

      <div class="space-y-1">
        <div class="font-bold text-gray-600">端口</div>
        <div>{{ row.meta.collection.port }}</div>
      </div>

      <div class="space-y-1">
        <div class="font-bold text-gray-600">数据库</div>
        <div>{{ row.meta.collection.database }}</div>
      </div>

      <div class="space-y-1">
        <div class="font-bold text-gray-600">用户</div>
        <div>{{ row.meta.collection.user }}</div>
      </div>
      <div class="space-y-1">
        <div class="font-bold text-gray-600">密码</div>
        <div>{{ row.meta.collection.password }}</div>
      </div>
    </div>
  </Panel>
  <Panel header="连接池信息" class="mt-4">
    <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
      <div class="space-y-1">
        <div class="font-bold text-gray-600">最大连接数</div>
        <div>{{ row.meta.pool.maxSize }}</div>
      </div>
    </div>
  </Panel>
</template>
<script setup lang="ts">
defineProps<{
  row: any
}>()
</script>
<style lang="scss" scoped></style>
