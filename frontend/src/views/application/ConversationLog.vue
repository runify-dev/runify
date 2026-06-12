<template>
  <div class="flex flex-col">
    <DataTable
      v-model:filters="filters"
      :value="conversationList"
      dataKey="id"
      filterDisplay="menu"
      :loading="loading"
      stripedRows
      @row-click="openMessage"
      @update:filters="filtersData"
      :row-class="() => 'cursor-pointer'"
      :pt="{
        header: { class: 'border-none' }
      }"
    >
      <template #header>
        <div class="flex items-center justify-between gap-3">
          <span class="text-sm font-medium text-surface-500">{{ t('application.conversationLogData.title') }}</span>
          <InputGroup class="max-w-xs">
            <InputGroupAddon>
              <i class="pi pi-search"/>
            </InputGroupAddon>
            <InputText
              v-model="filters['name'].value"
              :placeholder="t('application.conversationLogData.searchName')"
            />
          </InputGroup>
        </div>
      </template>
      <template #empty>
        <div class="flex flex-col items-center justify-center py-10 text-surface-400">
          <i class="pi pi-inbox text-4xl mb-3 opacity-40" />
          <p class="text-sm">{{ t('application.conversationLogData.noConversations') }}</p>
        </div>
      </template>
      <template #loading>
        <div class="flex items-center justify-center py-6 text-surface-400">
          <i class="pi pi-spin pi-spinner mr-2" />
          <span class="text-sm">{{ t('application.conversationLogData.loading') }}</span>
        </div>
      </template>
      <Column
        field="name"
        :header="t('application.conversationLogData.nameHeader')"
        :showFilterMatchModes="false"
        sortable
        class="min-w-[10rem]"
      >
        <template #body="{ data }">
          {{ data.name }}
        </template>
        <template #filter="{ filterModel }">
          <InputText v-model="filterModel.value" type="text" :placeholder="t('application.conversationLogData.namePlaceholder')" />
        </template>
      </Column>
      <Column
        :header="t('application.conversationLogData.executeTypeHeader')"
        field="executeType"
        :showFilterMatchModes="false"
        sortable
        class="min-w-[10rem]"
      >
        <template #body="{ data }">
          <Tag
            :severity="data.executeType === 'DEBUG' ? 'info' : 'success'"
            :value="data.executeType === 'DEBUG' ? t('application.conversationLogData.debugType') : t('application.conversationLogData.conversationType')"
          />
        </template>
        <template #filter="{ filterModel }">
          <SelectButton
            optionValue="value"
            optionLabel="name"
            v-model="filterModel.value"
            :options="[
              { name: t('application.conversationLogData.debugType'), value: 'DEBUG' },
              { name: t('application.conversationLogData.conversationType'), value: 'CONVERSATION' }
            ]"
          />
        </template>
      </Column>
      <Column
        :header="t('application.conversationLogData.userTypeHeader')"
        field="conversationUserType"
        :filterMenuStyle="{ width: '14rem' }"
        class="min-w-[12rem]"
      >
        <template #body="{ data }">
          <Tag
            :severity="data.conversationUserType === 'ADMIN_USER' ? 'info' : 'success'"
            :value="data.conversationUserType === 'ADMIN_USER' ? t('application.conversationLogData.systemUser') : t('application.conversationLogData.anonymousUser')"
          />
        </template>
      </Column>
      <Column
        field="starNum"
        :header="t('application.conversationLogData.likeCountHeader')"
        :showFilterMatchModes="false"
        class="min-w-[12rem]"
      >
        <template #body="{ data }">
          <span class="flex items-center gap-1.5">
            <i class="pi pi-thumbs-up text-surface-400 text-sm" />
            {{ data.starNum }}
          </span>
        </template>
      </Column>
    </DataTable>
    <div class="flex justify-end pt-2">
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
      />
    </div>
    <ConversationMessageDrawer ref="conversationMessageDrawerRef" />
  </div>
</template>
<script setup lang="ts">
import ApplicationAPI from '@/api/application'
import { ref, onMounted, computed, watch } from 'vue'
import { useRoute } from 'vue-router'
import { FilterMatchMode } from '@primevue/core/api'
import ConversationMessageDrawer from '@/views/application/conversation-message-drawer/index.vue'
import { type DataTableFilterMeta } from 'primevue/datatable'
import { t } from '@/locales'
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
