<template>
  <SimpleSearch v-model="keyword" @search="() => pageUser()"
    ><el-button @click="openCreateUser">创建用户</el-button></SimpleSearch
  >
  <AdaptiveHeight :exclude-height="230">
    <template #default="{ height }">
      <el-table :data="userList" class="w-full" :maxHeight="height">
        <el-table-column prop="username">
          <template #header>用户名</template>
          <template #default="{ row }">{{ row.username }}</template>
        </el-table-column>
        <el-table-column prop="nickname">
          <template #header>昵称</template>
          <template #default="{ row }">{{ row.nickname }}</template>
        </el-table-column>
        <el-table-column prop="icon">
          <template #header>用户头像</template>
          <template #default="{ row }">
            <el-avatar shape="square" :size="40" fit="fill" :src="row.icon" />
          </template>
        </el-table-column>
        <el-table-column prop="phone">
          <template #header>手机号</template>
          <template #default="{ row }">{{ row.phone ? row.phone : '--' }} </template>
        </el-table-column>
        <el-table-column prop="email">
          <template #header>邮箱</template>
          <template #default="{ row }">{{ row.email }}</template>
        </el-table-column>
        <el-table-column prop="createTime">
          <template #header>创建时间</template>
          <template #default="{ row }">{{ row.createTime }}</template>
        </el-table-column>
        <el-table-column prop="operate">
          <template #header>操作</template>
          <template #default="{ row }">
            <el-button @click="deleteUser(row.id)" style="margin: 1px" link type="primary">
              <el-icon> <Delete /> </el-icon
            ></el-button>
          </template>
        </el-table-column>
      </el-table>
    </template>
  </AdaptiveHeight>

  <Pagination
    :current-page="currentPage"
    :page-size="pageSize"
    :total="total"
    :pager-count="7"
    v-on:update:current-page="(c) => (currentPage = c)"
  ></Pagination>
  <CreateUser ref="createUserRef"></CreateUser>
</template>
<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import UserAPI from '@/api/user'
import SimpleSearch from '@/components/table/SimpleSearch.vue'
import Pagination from '@/components/table/Pagination.vue'
import CreateUser from '@/views/user-management/components/CreateUserDialog.vue'
import type { User } from '@/api/type/user'
import AdaptiveHeight from '@/components/adaptive-height/index.vue'
import { ElMessage } from 'element-plus'
const userList = ref<Array<User>>([])
const currentPage = ref<number>(1)
const pageSize = ref<number>(10)
const total = ref<number>(0)
const keyword = ref<string>()
const createUserRef = ref<InstanceType<typeof CreateUser>>()
const openCreateUser = () => {
  createUserRef.value?.open()
}
const pageUser = () => {
  UserAPI.page({ mixing: keyword.value }, currentPage.value, pageSize.value).then((ok) => {
    currentPage.value = ok.data.current
    userList.value = ok.data.records
    pageSize.value = ok.data.size
    total.value = ok.data.total
  })
}
const deleteUser = (userId: string) => {
  UserAPI.deleteUser(userId).then(() => {
    ElMessage.success('删除成功')
    pageUser()
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
