<template>
  <el-row :gutter="10" class="p-8">
    <el-col :span="8">
      <el-input
        v-model="searchText"
        style="max-width: 600px"
        placeholder="搜索处理器"
        class="input-with-select"
      >
        <template #append>
          <el-button :icon="Search" />
        </template> </el-input
    ></el-col>
    <el-col :span="8">
      <div class="flex items-center justify-center">
        <div>{{ project?.name }}</div>
      </div>
    </el-col>
    <el-col :span="8"
      ><el-button class="float-right" @click="openCreateProcessor">新建处理器</el-button></el-col
    >
  </el-row>
  <CreareProcessor ref="creareProcessorRef"></CreareProcessor>
  <AdaptiveHeight :exclude-height="230">
    <template #default="{ height }">
      <el-table :data="tableData" :height="height">
        <el-table-column type="expand"> </el-table-column>
        <el-table-column label="处理器名称" prop="name" />
        <el-table-column label="处理器描述" prop="desc" />
        <el-table-column label="处理器协议" prop="protocol" />
        <el-table-column label="操作" prop="protocol">
          <template #default="scope">
            <el-button @click="toSetting(scope.row)" link type="primary">
              <el-icon><Setting /></el-icon> </el-button
          ></template>
        </el-table-column>
      </el-table>
    </template>
  </AdaptiveHeight>
  <Pagination
    :current-page="query.currentPage"
    :page-size="query.pageSize"
    :total="total"
    :pager-count="7"
    v-on:update:current-page="(c) => (query.currentPage = c)"
  ></Pagination>
</template>
<script setup lang="ts">
import { ref, computed, inject, onMounted } from 'vue'
import Pagination from '@/components/table/Pagination.vue'
import { useRoute, useRouter } from 'vue-router'
import { Search } from '@element-plus/icons-vue'
import CreareProcessor from '../components/CreareProcessor.vue'
import type { TreeCommonAPI } from '@/api/tree'
import processorAPI from '@/api/processor'
import { type QueryProcessorVO } from '@/api/type/processor'
import AdaptiveHeight from '@/components/adaptive-height/index.vue'
const router = useRouter()
const creareProcessorRef = ref<InstanceType<typeof CreareProcessor>>()
const route = useRoute()
const treeCommonAPI: TreeCommonAPI = inject('treeCommonAPI') as TreeCommonAPI
const searchText = ref<string>()
const toSetting = (row: any) => {
  router.push({
    name: 'processorWorkflow',
    params: { processorId: row.id, id: projectId.value }
  })
}
const projectId = computed(() => {
  const {
    params: { id }
  } = route as any
  return id
})
const openCreateProcessor = () => {
  creareProcessorRef.value?.open(projectId.value)
}
const project = ref()
const total = ref<number>(0)
const query = ref<QueryProcessorVO>({
  currentPage: 1,
  pageSize: 10,
  name: '',
  protocol: '',
  desc: ''
})
const tableData = ref<Array<any>>([])
onMounted(() => {
  treeCommonAPI.getResource(projectId.value).then((ok) => {
    project.value = ok.data
  })
  processorAPI.pageProcessor(projectId.value, query.value).then((ok) => {
    tableData.value = ok.data.records
    total.value = ok.data.total
  })
})
</script>
<style lang="scss"></style>
