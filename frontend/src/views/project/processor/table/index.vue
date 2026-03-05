<template>
  <Toolbar
    :pt="{
      root: {
        style: {
          border: 0
        }
      }
    }"
  >
    <template #start> </template>
    <template #center>
      <InputGroup>
        <InputText v-model="searchText" placeholder="搜索处理器" />
        <InputGroupAddon>
          <Button icon="pi pi-search" severity="secondary" variant="text" />
        </InputGroupAddon>
      </InputGroup>
    </template>
    <template #end> <Button @click="openCreateProcessor" label="新建数处理器"></Button></template>
  </Toolbar>
  <CreareProcessor ref="creareProcessorRef"></CreareProcessor>
  <AdaptiveHeight :exclude-height="230">
    <template #default="{ height }">
      <DataTable
        :value="tableData"
        v-model:expandedRows="expandedRows"
        dataKey="id"
        tableStyle="min-width: 50rem"
      >
        <Column expander style="width: 5rem"> </Column>
        <Column field="name" header="处理器名称"></Column>
        <Column field="desc" header="处理器描述"></Column>
        <Column field="protocol" header="处理器协议"></Column>
        <Column field="operate" header="操作">
          <template #body="scope">
            <Button
              icon="pi pi-file-edit"
              variant="text"
              rounded
              aria-label="Cancel"
              size="normal"
              @click.stop="toSetting(scope.data)"
            />
            <Button
              icon="pi pi-times-circle"
              variant="text"
              rounded
              aria-label="Cancel"
              size="normal"
              @click="deleteProcessor(scope.data)"
            />
          </template>
        </Column>
        <template #expansion="slotProps">
          <Expansion :protocol="slotProps.data.protocol" :meta="slotProps.data.meta"></Expansion>
        </template>
      </DataTable>
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
import { ref, computed, inject, onMounted, provide } from 'vue'
import Pagination from '@/components/table/Pagination.vue'
import { useRoute, useRouter } from 'vue-router'
import CreareProcessor from '../components/CreareProcessor.vue'
import type { TreeCommonAPI } from '@/api/tree'
import processorAPI from '@/api/processor'
import { type QueryProcessorVO } from '@/api/type/processor'
import AdaptiveHeight from '@/components/adaptive-height/index.vue'
import Expansion from './expansion/index.vue'
const router = useRouter()
const creareProcessorRef = ref<InstanceType<typeof CreareProcessor>>()
const route = useRoute()
const expandedRows = ref<any>()
const treeCommonAPI: TreeCommonAPI = inject('treeCommonAPI') as TreeCommonAPI
const searchText = ref<string>()
const toSetting = (row: any) => {
  router.push({
    name: 'processorWorkflow',
    params: { processorId: row.id, id: projectId.value }
  })
}
const deleteProcessor = (processor: any) => {}
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
provide('project', project)
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
