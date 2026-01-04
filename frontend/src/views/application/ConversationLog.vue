<template>
  <div class="relative overflow-x-auto shadow-md sm:rounded-lg">
    <!-- 搜索栏 -->
    <SimpleSearch v-model="keyword"></SimpleSearch>
    <TableVue :data="conversationList">
      <TableColumnVue prop="name">
        <template #header>姓名</template>
        <template #default="{ row }">{{ row.name }}</template>
      </TableColumnVue>
      <TableColumnVue prop="conversationRecordCount">
        <template #header>对话提问次数</template>
        <template #default="{ row }">{{ row.conversationRecordCount }}</template>
      </TableColumnVue>
      <TableColumnVue prop="conversationUserType">
        <template #header>对话用户</template>
        <template #default="{ row }">{{ row.conversationUserType }}</template>
      </TableColumnVue>
      <TableColumnVue prop="starNum">
        <template #header>反馈</template>
        <template #default="{ row }">{{ row.starNum }} / {{ row.trampleNum }}</template>
      </TableColumnVue>
      <TableColumnVue prop="createTime">
        <template #header>时间</template>
        <template #default="{ row }">{{ row.createTime }}</template>
      </TableColumnVue>
    </TableVue>
    <Pagination
      :current-page="currentPage"
      :page-size="pageSize"
      :total="total"
      :pager-count="7"
      v-on:update:current-page="(c) => (currentPage = c)"
    ></Pagination>
  </div>
</template>
<script setup lang="ts">
import ApplicationAPI from '@/api/application'
import { ref, onMounted, computed, watch } from 'vue'
import TableColumnVue from '@/components/table/TableColumn.vue'
import TableVue from '@/components/table/index.vue'
import { useRoute } from 'vue-router'
import Pagination from '@/components/table/Pagination.vue'
import SimpleSearch from '@/components/table/SimpleSearch.vue'
const route = useRoute()
const conversationList = ref<Array<any>>([])
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

const listConversationLog = () => {
  ApplicationAPI.pageConversation(
    folderId.value,
    applicationId.value,
    currentPage.value,
    pageSize.value
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
const pageSize = ref(1)
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
