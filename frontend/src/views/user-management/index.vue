<template>
  <div class="card layout-content-container">
    <DataTable
      v-model:filters="filters"
      :value="userList"
      dataKey="id"
      filterDisplay="menu"
      :loading="loading"
      @update:filters="filtersData"
    >
      <template #header>
        <div class="flex items-center justify-between gap-3">
          <!-- 左侧：创建用户按钮 -->
          <Button label="创建用户" icon="pi pi-plus" @click="openCreateUser" />
          <!-- 右侧：关键字搜索 -->
          <IconField>
            <InputIcon>
              <i class="pi pi-search" />
            </InputIcon>
            <InputText v-model="filters['username'].value" placeholder="关键字搜索" />
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
              pageUser()
            }
          "
        />
      </template>

      <template #empty>暂无用户数据</template>
      <template #loading>加载中，请稍候...</template>

      <Column
        field="username"
        header="用户名"
        :showFilterMatchModes="false"
        sortable
        style="min-width: 10rem"
      >
        <template #body="{ data }">{{ data.username }}</template>
        <template #filter="{ filterModel }">
          <InputText v-model="filterModel.value" type="text" placeholder="请输入用户名" />
        </template>
      </Column>

      <Column
        field="nickname"
        header="昵称"
        :showFilterMatchModes="false"
        sortable
        style="min-width: 10rem"
      >
        <template #body="{ data }">{{ data.nickname }}</template>
        <template #filter="{ filterModel }">
          <InputText v-model="filterModel.value" type="text" placeholder="请输入昵称" />
        </template>
      </Column>

      <Column field="icon" header="头像" :showFilterMatchModes="false" style="min-width: 6rem">
        <template #body="{ data }">
          <Avatar v-if="data.icon" :image="resetUrl(data.icon)" shape="circle" size="normal" />
          <Avatar
            v-else
            :label="data.nickname?.charAt(0) ?? data.username?.charAt(0)"
            shape="circle"
            size="normal"
          />
        </template>
      </Column>

      <Column
        field="phone"
        header="手机号"
        :showFilterMatchModes="false"
        sortable
        style="min-width: 10rem"
      >
        <template #body="{ data }">{{ data.phone }}</template>
        <template #filter="{ filterModel }">
          <InputText v-model="filterModel.value" type="text" placeholder="请输入手机号" />
        </template>
      </Column>

      <Column
        field="email"
        header="邮箱"
        :showFilterMatchModes="false"
        sortable
        style="min-width: 12rem"
      >
        <template #body="{ data }">{{ data.email }}</template>
        <template #filter="{ filterModel }">
          <InputText v-model="filterModel.value" type="text" placeholder="请输入邮箱" />
        </template>
      </Column>

      <Column
        field="createTime"
        header="创建时间"
        :showFilterMatchModes="false"
        sortable
        style="min-width: 12rem"
      >
        <template #body="{ data }">{{ data.createTime }}</template
        ><!-- 修复了原来错误写成 data.email -->
      </Column>

      <Column field="operate" header="操作" style="min-width: 8rem">
        <template #body="{ data }">
          <div class="flex items-center gap-2">
            <Button
              icon="pi pi-pencil"
              severity="secondary"
              text
              rounded
              size="small"
              v-tooltip.top="'授权'"
              @click="openEditUser(data)"
            />
            <Button
              icon="pi pi-trash"
              severity="danger"
              text
              rounded
              size="small"
              v-tooltip.top="'删除'"
              @click="deleteUser(data.id)"
            />
          </div>
        </template>
      </Column>
    </DataTable>

    <ConversationMessageDrawer ref="conversationMessageDrawerRef" />
  </div>

  <CreateUser ref="createUserRef" @success="pageUser" />
  <ResourceAuthDrawer ref="resourceAuthDrawerRef"></ResourceAuthDrawer>
</template>

<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import UserAPI from '@/api/user'
import CreateUser from '@/views/user-management/components/CreateUserDialog.vue'
import type { User } from '@/api/type/user'
import { FilterMatchMode } from '@primevue/core/api'
import { getActiveFilters, resetUrl } from '@/utils/common'
import { type DataTableFilterMeta } from 'primevue/datatable'
import bus from '@/bus'
import ResourceAuthDrawer from '@/views/user-management/components/ResourceAuthDrawer.vue'
const loading = ref<boolean>(false)
const userList = ref<Array<User>>([])
const currentPage = ref<number>(1)
const pageSize = ref<number>(10)
const total = ref<number>(0)
const createUserRef = ref<InstanceType<typeof CreateUser>>()
const resourceAuthDrawerRef = ref<InstanceType<typeof ResourceAuthDrawer>>()
const filters = ref<any>({
  username: { value: null, matchMode: FilterMatchMode.CONTAINS },
  nickname: { value: null, matchMode: FilterMatchMode.CONTAINS }
})

const filtersData = (event: DataTableFilterMeta) => {
  const query = getActiveFilters(event)
  pageUser(query)
}

const openCreateUser = () => createUserRef.value?.open()
const openEditUser = (user: User) => resourceAuthDrawerRef.value?.open(user.id)

const pageUser = (query?: any) => {
  if (!query) {
    query = {}
  }
  loading.value = true
  UserAPI.page(query, currentPage.value, pageSize.value)
    .then((ok) => {
      currentPage.value = ok.data.current
      userList.value = ok.data.records
      pageSize.value = ok.data.size
      total.value = ok.data.total
    })
    .finally(() => {
      loading.value = false
    })
}

const deleteUser = (userId: string) => {
  UserAPI.deleteUser(userId).then(() => {
    bus.emit('message:success', '删除成功')
    pageUser()
  })
}

watch(currentPage, () => pageUser())
onMounted(() => pageUser())
</script>
