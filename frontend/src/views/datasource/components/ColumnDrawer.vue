<template>
  <Drawer v-model:visible="visible" :header="`${tableName} - 列信息`" class="!w-[500px]">
    <DataTable :value="columnList" :loading="loading" stripedRows>
      <Column field="name" header="列名" />
      <Column field="type" header="类型" />
      <Column field="nullable" header="可空">
        <template #body="{ data }">
          <Tag
            :value="data.nullable ? 'YES' : 'NO'"
            :severity="data.nullable ? 'secondary' : 'info'"
          />
        </template>
      </Column>
      <Column field="primaryKey" header="主键">
        <template #body="{ data }">
          <i v-if="data.primaryKey" class="pi pi-key text-yellow-500" />
          <span v-else>-</span>
        </template>
      </Column>
      <Column field="comment" header="注释">
        <template #body="{ data }">
          {{ data.comment || '-' }}
        </template>
      </Column>
      <template #empty>
        <div class="text-center py-8 text-surface-400">暂无列信息</div>
      </template>
    </DataTable>
  </Drawer>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import databaseConnectionPoolAPI from '@/api/database-connection-pool'

const props = defineProps<{
  datasourceId: string
}>()

const visible = ref(false)
const loading = ref(false)
const tableName = ref('')
const columnList = ref<Array<any>>([])

const loadColumns = (name: string) => {
  loading.value = true
  tableName.value = name
  databaseConnectionPoolAPI.getColumns(props.datasourceId, name, loading).then((ok) => {
    columnList.value = ok.data
  })
}

const open = (name: string) => {
  visible.value = true
  loadColumns(name)
}

defineExpose({ open })
</script>
