<template>
  <div class="flex flex-col gap-4 p-4">
    <!-- 顶部工具栏 -->
    <div class="flex items-center justify-between gap-4">
      <InputGroup class="max-w-sm">
        <InputGroupAddon class="!bg-surface-0 !border-surface-200">
          <i class="pi pi-search text-surface-400 text-sm" />
        </InputGroupAddon>
        <InputText
          v-model="searchText"
          placeholder="搜索处理器..."
          class="!border-l-0 !border-surface-200 !bg-surface-0 text-sm focus:!border-primary-400 focus:!ring-2 focus:!ring-primary-100 placeholder:text-surface-400 transition-all duration-200"
        />
      </InputGroup>

      <Button icon="pi pi-plus" label="新建处理器" @click="openCreateProcessor" class="shrink-0" />
    </div>

    <!-- 数据表格 -->
    <div class="card">
      <DataTable
        :value="tableData"
        v-model:expandedRows="expandedRows"
        dataKey="id"
        class="p-datatable-sm"
        :pt="{
          table: { style: { minWidth: '100%' } },
          thead: { class: 'bg-surface-50' },
          headerRow: { class: 'text-sm font-semibold text-surface-700' },
          headerCell: { class: 'py-2' },
          bodyRow: { class: 'hover:bg-surface-50 transition-colors duration-150' },
          bodyCell: { class: 'text-sm text-surface-600 py-3' }
        }"
      >
        <Column expander style="width: 3rem" />
        <Column field="name" header="处理器名称">
          <template #body="scope">
            <span class="font-medium text-surface-900">{{ scope.data.name }}</span>
          </template>
        </Column>
        <Column field="desc" header="处理器描述">
          <template #body="scope">
            <span class="text-surface-500 line-clamp-1">{{ scope.data.desc || '-' }}</span>
          </template>
        </Column>
        <Column field="protocol" header="协议类型">
          <template #body="scope">
            <span
              class="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-primary-50 text-primary-700"
            >
              {{ scope.data.protocol }}
            </span>
          </template>
        </Column>
        <Column header="操作" style="width: 8rem">
          <template #body="scope">
            <div class="flex items-center gap-1">
              <Button
                icon="pi pi-file-edit"
                variant="text"
                severity="secondary"
                size="small"
                v-tooltip.top="'编辑'"
                @click.stop="toSetting(scope.data)"
              />
              <Button
                icon="pi pi-trash"
                variant="text"
                severity="danger"
                size="small"
                v-tooltip.top="'删除'"
                @click="deleteProcessor(scope.data)"
              />
            </div>
          </template>
        </Column>
        <template #expansion="slotProps">
          <Expansion :protocol="slotProps.data.protocol" :meta="slotProps.data.meta" />
        </template>
        <template #empty>
          <div class="flex flex-col items-center justify-center py-12 text-surface-400">
            <i class="pi pi-inbox text-4xl mb-3 opacity-40" />
            <p class="text-sm">暂无处理器数据</p>
          </div>
        </template>
      </DataTable>

      <!-- 分页 -->
      <div class="flex justify-end pt-3 border-t border-surface-100">
        <Paginator
          :rows="query.pageSize"
          :totalRecords="total"
          :rowsPerPageOptions="[10, 20, 50]"
          @page="(e) => (query.currentPage = e.page + 1)"
          template="FirstPageLink PrevPageLink PageLinks NextPageLink LastPageLink RowsPerPageDropdown"
          :pt="{
            root: { class: 'bg-transparent' },
            pages: { class: 'gap-1' },
            pageButton: ({ context }: any) => ({
              class: context.active ? 'bg-primary text-primary-contrast' : 'hover:bg-surface-100'
            })
          }"
        />
      </div>
    </div>

    <CreareProcessor ref="creareProcessorRef" />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, inject, onMounted, provide } from 'vue'
import CreareProcessor from '../components/CreareProcessor.vue'
import type { TreeCommonAPI } from '@/api/tree'
import processorAPI from '@/api/processor'
import { type QueryProcessorVO } from '@/api/type/processor'
import Expansion from './expansion/index.vue'
import { useRouter, useRoute } from 'vue-router'
import Paginator from 'primevue/paginator'

const router = useRouter()
const route = useRoute()
const creareProcessorRef = ref<InstanceType<typeof CreareProcessor>>()
const expandedRows = ref<any>()
const treeCommonAPI: TreeCommonAPI = inject('treeCommonAPI') as TreeCommonAPI
const searchText = ref<string>()

const projectId = computed(() => {
  const { params: { id } } = route as any
  return id
})

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

const toSetting = (row: any) => {
  router.push({
    name: 'processorWorkflow',
    params: { processorId: row.id, id: projectId.value }
  })
}

const deleteProcessor = (processor: any) => {
  // TODO: 实现删除逻辑
}

const openCreateProcessor = () => {
  creareProcessorRef.value?.open(projectId.value)
}

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
