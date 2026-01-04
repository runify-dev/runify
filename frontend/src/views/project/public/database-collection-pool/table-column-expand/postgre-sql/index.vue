<template>
  <el-descriptions title="连接信息" :column="4" direction="vertical">
    <el-descriptions-item label="主机" :span="2">{{
      row.meta.collection.host
    }}</el-descriptions-item>
    <el-descriptions-item label="端口">
      <el-tag size="small">{{ row.meta.collection.port }}</el-tag>
    </el-descriptions-item>
    <el-descriptions-item label="数据库">{{ row.meta.collection.database }}</el-descriptions-item>
    <el-descriptions-item label="用户">{{ row.meta.collection.user }}</el-descriptions-item>
    <el-descriptions-item label="密码">{{ row.meta.collection.password }}</el-descriptions-item>
  </el-descriptions>
  <el-descriptions title="连接池信息" :column="4" direction="vertical">
    <el-descriptions-item label="最大连接数" :span="2">{{
      row.meta.pool.maxSize
    }}</el-descriptions-item>
  </el-descriptions>
</template>
<script setup lang="ts">
defineProps<{
  row: any
}>()
</script>
<style lang="scss" scoped></style>
