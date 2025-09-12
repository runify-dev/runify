<template>
  <SimpleSearch v-model="keyword" @search="() => pageUser()"
    ><el-button>创建用户</el-button></SimpleSearch
  >
  <TableVue :data="userList">
    <TableColumnVue prop="username">
      <template #header>用户名</template>
      <template #default="{ row }">{{ row.username }}</template>
    </TableColumnVue>
    <TableColumnVue prop="nickname">
      <template #header>昵称</template>
      <template #default="{ row }">{{ row.nickname }}</template>
    </TableColumnVue>
    <TableColumnVue prop="icon">
      <template #header>用户头像</template>
      <template #default="{ row }">
        <el-avatar shape="square" :size="40" fit="fill" :src="row.icon" />
      </template>
    </TableColumnVue>
    <TableColumnVue prop="phone">
      <template #header>手机号</template>
      <template #default="{ row }">{{ row.phone ? row.phone : '--' }}</template>
    </TableColumnVue>
    <TableColumnVue prop="email">
      <template #header>邮箱</template>
      <template #default="{ row }">{{ row.email }}</template>
    </TableColumnVue>
    <TableColumnVue prop="createTime">
      <template #header>创建时间</template>
      <template #default="{ row }">{{ row.createTime }}</template>
    </TableColumnVue>
    <TableColumnVue prop="updateTime">
      <template #header>修改时间</template>
      <template #default="{ row }">{{ row.updateTime }}</template>
    </TableColumnVue>
  </TableVue>
  <Pagination
    :current-page="currentPage"
    :page-size="pageSize"
    :total="total"
    :pager-count="7"
    v-on:update:current-page="(c) => (currentPage = c)"
  ></Pagination>
</template>
<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import UserAPI from '@/api/user'
import TableColumnVue from '@/components/table/TableColumn.vue'
import TableVue from '@/components/table/index.vue'
import SimpleSearch from '@/components/table/SimpleSearch.vue'
import Pagination from '@/components/table/Pagination.vue'
import type { User } from '@/api/type/user'
const userList = ref<Array<User>>([])

const currentPage = ref<number>(1)
const pageSize = ref<number>(10)
const total = ref<number>(0)
const keyword = ref<string>()

const pageUser = () => {
  UserAPI.page({ mixing: keyword.value }, currentPage.value, pageSize.value).then((ok) => {
    currentPage.value = ok.data.current
    userList.value = ok.data.records
    pageSize.value = ok.data.size
    total.value = ok.data.total
  })
}
watch(currentPage, () => {
  pageUser()
})
onMounted(() => {
  pageUser()
})
</script>
<style lang="scss" scoped></style>
