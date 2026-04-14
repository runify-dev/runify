<template>
  <div>
    <DataTable
      v-model:filters="filters"
      :value="conversationList"
      dataKey="id"
      filterDisplay="menu"
      :loading="loading"
      @row-click="openMessage"
      @update:filters="filtersData"
    >
      <template #header>
        <div class="flex justify-end">
          <IconField>
            <InputIcon>
              <i class="pi pi-search" />
            </InputIcon>
            <InputText v-model="filters['name'].value" placeholder="关键字搜索" />
          </IconField>
        </div>
      </template>
      <template #footer>
        <Paginator
          :rows="pageSize"
          :totalRecords="total"
          :rowsPerPageOptions="[5, 10, 20, 30]"
          @page="
            (v: any) => {
              currentPage = v.page
              pageSize = v.rows
              listConversationLog()
            }
          "
        ></Paginator>
      </template>
      <template #empty> No customers found. </template>
      <template #loading> Loading customers data. Please wait. </template>
      <Column
        field="name"
        header="名称"
        :showFilterMatchModes="false"
        sortable
        style="min-width: 10rem"
      >
        <template #body="{ data }">
          {{ data.name }}
        </template>
        <template #filter="{ filterModel }">
          <InputText v-model="filterModel.value" type="text" placeholder="请输入名称" />
        </template>
      </Column>
      <Column
        header="执行类型"
        field="executeType"
        :showFilterMatchModes="false"
        sortable
        style="min-width: 10rem"
      >
        <template #body="{ data }">
          <Tag severity="info" value="调试" v-if="data.executeType == 'DEBUG'"></Tag>
          <Tag severity="success" value="对话" v-else></Tag>
        </template>
        <template #filter="{ filterModel }">
          <SelectButton
            optionValue="value"
            optionLabel="name"
            v-model="filterModel.value"
            :options="[
              { name: '调试', value: 'DEBUG' },
              { name: '对话', value: 'CONVERSATION' }
            ]"
          />
        </template>
      </Column>
      <Column
        header="对话用户类型"
        field="conversationUserType"
        :filterMenuStyle="{ width: '14rem' }"
        style="min-width: 12rem"
      >
        <template #body="{ data }">
          <Tag
            severity="info"
            value="系统用户"
            v-if="data.conversationUserType == 'ADMIN_USER'"
          ></Tag>
          <Tag
            severity="success"
            value="匿名用户"
            v-if="data.conversationUserType == 'ANONYMOUS'"
          ></Tag>
        </template>
      </Column>
      <Column
        field="starNum"
        header="点赞数量"
        :showFilterMatchModes="false"
        style="min-width: 12rem"
      >
        <template #body="{ data }">
          {{ data.starNum }}
        </template>
      </Column>
    </DataTable>
    <ConversationMessageDrawer ref="conversationMessageDrawerRef"></ConversationMessageDrawer>
  </div>
</template>
<script setup lang="ts">
import ApplicationAPI from '@/api/application'
import { ref, onMounted, computed, watch } from 'vue'
import { useRoute } from 'vue-router'
import { FilterMatchMode } from '@primevue/core/api'
import ConversationMessageDrawer from '@/views/application/conversation-message-drawer/index.vue'
import { type DataTableFilterMeta } from 'primevue/datatable'
const loading = ref<boolean>(false)
const route = useRoute()
const conversationList = ref<Array<any>>([])
const conversationMessageDrawerRef = ref<InstanceType<typeof ConversationMessageDrawer>>()
const openMessage = (row: any) => {
  conversationMessageDrawerRef.value?.open(row.data.applicationId, row.data.id)
}
const getActiveFilters = (f: DataTableFilterMeta) => {
  const result: Record<string, any> = {}

  Object.keys(f).forEach((key) => {
    const field = f[key]

    if (!field || typeof field === 'string') return // 如果是空值或 string，跳过

    // 单值过滤
    if ('value' in field) {
      if (field.value != null) result[key] = field.value
    }
    // 复合过滤
    else if ('operator' in field && Array.isArray(field.constraints)) {
      const val = field.constraints.map((c) => c.value).filter((v) => v != null)
      if (val.length > 0) result[key] = val.length === 1 ? val[0] : val
    }
  })

  return result
}
const filtersData = (f: DataTableFilterMeta) => {
  const query = getActiveFilters(f)
  console.log(query)
  listConversationLog(query)
}
const folderId = computed(() => {
  const {
    params: { folderId }
  } = route as any
  return folderId
})
const applicationId = computed(() => {
  const {
    params: { id }
  } = route as any
  return id
})
const filters = ref<any>({
  name: {
    value: null,
    matchMode: FilterMatchMode.EQUALS
  },
  executeType: {
    value: null,
    matchMode: FilterMatchMode.EQUALS
  }
})

const listConversationLog = (query?: any) => {
  if (!query) {
    query = {}
  }
  ApplicationAPI.pageConversation(
    applicationId.value,
    currentPage.value,
    pageSize.value,
    query,
    loading
  ).then((ok) => {
    conversationList.value = ok.data.records
    total.value = ok.data.total
    currentPage.value = ok.data.current
    pageSize.value = ok.data.size
  })
}

/* ------------- 响应式状态 ------------- */
const keyword = ref('')
const currentPage = ref(1)
const pageSize = ref<number>(10)
// 总条数
const total = ref(0)
watch(currentPage, () => {
  listConversationLog()
})

onMounted(() => {
  listConversationLog()
})
</script>
<style scoped></style>
