<template>
  <div>
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
          <InputText v-model="searchText" :placeholder="t('database.search')" />
          <InputGroupAddon>
            <Button icon="pi pi-search" severity="secondary" variant="text" />
          </InputGroupAddon>
        </InputGroup>
      </template>
      <template #end>
        <Button @click="openCreateCollectionPool" :label="t('datasource.create')"></Button
      ></template>
    </Toolbar>
    <DataTable
      :value="tableData"
      v-model:expandedRows="expandedRows"
      dataKey="id"
      tableStyle="min-width: 50rem"
    >
      <Column expander style="width: 5rem"></Column>
      <Column field="name" :header="t('database.datasourceName')"></Column>
      <Column field="desc" :header="t('database.datasourceDesc')"></Column>
      <Column field="dataSourceType" :header="t('datasource.form.type')"></Column>
      <Column field="provider" :header="t('datasource.form.vendor')"></Column>
      <Column field="operate" :header="t('common.operation')">
        <template #body="scope">
          <Button
            icon="pi pi-file-edit"
            variant="text"
            rounded
            aria-label="Cancel"
            size="normal"
            @click.stop="openEditDatabaseCollectionPollDialog(scope.data)"
          />
          <Button
            icon="pi pi-times-circle"
            variant="text"
            rounded
            aria-label="Cancel"
            size="normal"
            @click="deleteDatabaseCollectionPoll(scope.data)"
          />
        </template>
      </Column>
      <template #expansion="slotProps">
        <TableColumnExpand :row="slotProps.data"></TableColumnExpand>
      </template>
    </DataTable>

    <Pagination
      :current-page="query.currentPage"
      :page-size="query.pageSize"
      :total="total"
      :pager-count="7"
      v-on:update:current-page="(c) => (query.currentPage = c)"
    ></Pagination>
    <CollectionPool
      @refresh="page()"
      :project-id="projectId"
      ref="collectionPoolRef"
    ></CollectionPool>
  </div>
</template>
<script setup lang="ts">
import TableColumnExpand from './table-column-expand/index.vue'
import CollectionPool from './collection-pool/index.vue'
import { ref, computed, onMounted } from 'vue'
import { t } from '@/locales'
import Pagination from '@/components/table/Pagination.vue'
import type { QueryPageDatabaseCollectionPoolVO } from '@/api/type/database-connection-pool'
import databaseConnectionPoolAPI from '@/api/database-connection-pool'
import { useRoute } from 'vue-router'
import bus from '@/bus'
const route = useRoute()
const query = ref<QueryPageDatabaseCollectionPoolVO>({
  currentPage: 1,
  pageSize: 10,
  name: '',
  provider: '',
  desc: ''
})
const openEditDatabaseCollectionPollDialog = (data: any) => {
  collectionPoolRef.value?.open(data)
}
const deleteDatabaseCollectionPoll = (data: any) => {
  databaseConnectionPoolAPI.deleteById(data.projectId, data.id).then((ok) => {
    bus.emit('message:success', [t('database.deleteSuccess'), t('datasource.deleteSuccess')])
    page()
  })
}
const total = ref<number>(0)
const projectId = computed(() => {
  return route.params.id as string
})
const expandedRows = ref({})
const tableData = ref<Array<any>>([])
const collectionPoolRef = ref<InstanceType<typeof CollectionPool>>()
const searchText = ref<string>('')
const openCreateCollectionPool = () => {
  collectionPoolRef.value?.open()
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
