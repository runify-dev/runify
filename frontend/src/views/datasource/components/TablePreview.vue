<template>
  <div class="mt-6">
    <div class="flex items-center justify-between mb-4">
      <h3 class="text-base font-semibold">表信息</h3>
      <Button
        icon="pi pi-refresh"
        severity="secondary"
        variant="text"
        size="small"
        @click="loadTables"
        :loading="loading"
      />
    </div>
    <DataTable :value="tableList" :loading="loading" stripedRows>
      <Column field="name" header="表名" />
      <Column field="engine" header="引擎" />
      <Column field="comment" header="注释">
        <template #body="{ data }">
          {{ data.comment || '-' }}
        </template>
      </Column>
      <Column header="操作" class="min-w-[100px]">
        <template #body="{ data }">
          <Button
            label="查看列"
            variant="text"
            size="small"
            @click="openColumnDrawer(data)"
          />
        </template>
      </Column>
      <template #empty>
        <div class="text-center py-8 text-surface-400">暂无表信息</div>
      </template>
    </DataTable>

    <ColumnDrawer
      ref="columnDrawerRef"
      :datasource-id="datasourceId"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import ColumnDrawer from './ColumnDrawer.vue'
import databaseConnectionPoolAPI from '@/api/database-connection-pool'

const props = defineProps<{
  datasourceId: string
}>()

const loading = ref(false)
const tableList = ref<Array<any>>([])
const columnDrawerRef = ref<InstanceType<typeof ColumnDrawer>>()

const loadTables = () => {
  loading.value = true
  databaseConnectionPoolAPI.getTables(props.datasourceId, loading).then((ok) => {
    tableList.value = ok.data
  })
}

const openColumnDrawer = (table: any) => {
  columnDrawerRef.value?.open(table.name)
}

watch(
  () => props.datasourceId,
  () => {
    if (props.datasourceId) {
      loadTables()
    }
  }
)

onMounted(() => {
  if (props.datasourceId) {
    loadTables()
  }
})
</script>
