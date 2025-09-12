<template>
  <SimpleSearch v-model="keyword"><el-button>创建用户</el-button></SimpleSearch>
  <TableVue :data="[{ name: 'ss', age: '11' }]">
    <TableColumnVue prop="name">
      <template #header>姓名</template>
      <template #default="{ row }">{{ row.name }}</template>
    </TableColumnVue>
    <TableColumnVue prop="age">
      <template #header>年龄</template>
      <template #default="{ row }">{{ row.age }}</template>
    </TableColumnVue>
  </TableVue>
  <Pagination
    :current-page="currentPage"
    :page-size="pageSize"
    :total="total"
    :pager-count="7"
    v-on:update:current-page="(c) => (currentPage = c)"
  ></Pagination>
</template>
<script setup lang="ts">
import { ref } from 'vue'
import TableColumnVue from '@/components/table/TableColumn.vue'
import TableVue from '@/components/table/index.vue'
import SimpleSearch from '@/components/table/SimpleSearch.vue'
import Pagination from '@/components/table/Pagination.vue'
const currentPage = ref<number>(0)
const pageSize = ref<number>(0)
const total = ref<number>(0)
const keyword = ref<string>()
</script>
<style lang="scss" scoped></style>
