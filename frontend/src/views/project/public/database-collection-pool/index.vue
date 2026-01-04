<template>
  <div>
    <el-row :gutter="10" class="p-8">
      <el-col :span="8">
        <el-input
          v-model="searchText"
          style="max-width: 600px"
          placeholder="搜索连接池"
          class="input-with-select"
        >
          <template #append>
            <el-button :icon="Search" />
          </template> </el-input
      ></el-col>
      <el-col :span="8">
        <div class="flex items-center justify-center"></div>
      </el-col>
      <el-col :span="8"
        ><el-button class="float-right" @click="openCreateCollectionPool"
          >新建连接池</el-button
        ></el-col
      >
    </el-row>
    <el-table :data="tableData">
      <el-table-column type="expand">
        <template #default="props">
          <TableColumnExpand :row="props.row"></TableColumnExpand>
        </template>
      </el-table-column>
      <el-table-column label="处理器名称" prop="name" />
      <el-table-column label="处理器描述" prop="desc" />
      <el-table-column label="处理器协议" prop="protocol" />
      <el-table-column label="操作" prop="protocol">
        <template #default="scope">
          <el-button link type="primary">
            <el-icon><Setting /></el-icon> </el-button
        ></template>
      </el-table-column>
    </el-table>
    <Pagination
      :current-page="query.currentPage"
      :page-size="query.pageSize"
      :total="total"
      :pager-count="7"
      v-on:update:current-page="(c) => (query.currentPage = c)"
    ></Pagination>
    <CreateCollectionPool
      :project-id="projectId"
      ref="createCollectionPoolRef"
    ></CreateCollectionPool>
  </div>
</template>
<script setup lang="ts">
import { Search } from '@element-plus/icons-vue'
import TableColumnExpand from './table-column-expand/index.vue'
import CreateCollectionPool from './create-collection-pool/index.vue'
import { ref, computed, onMounted } from 'vue'
import Pagination from '@/components/table/Pagination.vue'
import type { QueryPageDatabaseCollectionPoolVO } from '@/api/type/database-connection-pool'
import databaseConnectionPoolAPI from '@/api/database-connection-pool'
import { useRoute } from 'vue-router'
const route = useRoute()
const query = ref<QueryPageDatabaseCollectionPoolVO>({
  currentPage: 1,
  pageSize: 10,
  name: '',
  protocol: '',
  desc: ''
})
const total = ref<number>(0)
const projectId = computed(() => {
  return route.params.id as string
})
const tableData = ref<Array<any>>([])
const createCollectionPoolRef = ref<InstanceType<typeof CreateCollectionPool>>()
const searchText = ref<string>('')
const openCreateCollectionPool = () => {
  createCollectionPoolRef.value?.open()
}
const page = () => {
  databaseConnectionPoolAPI.page(projectId.value, query.value).then((ok) => {
    tableData.value = ok.data.records
    total.value = ok.data.total
  })
}
onMounted(() => {
  page()
})
</script>
<style lang="scss" scoped></style>
