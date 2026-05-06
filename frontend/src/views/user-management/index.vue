<template>
  <div class="layout-content-height">
    <DataTable
      v-model:filters="filters"
      :value="userList"
      dataKey="id"
      filterDisplay="menu"
      :loading="loading"
      @update:filters="filtersData"
    >
      <template #header>
        <div class="flex items-center justify-between gap-2">
          <Button label="创建用户" icon="pi pi-plus" size="small" @click="openCreateUser" />
          <IconField>
            <InputIcon>
              <i class="pi pi-search" />
            </InputIcon>
            <InputText v-model="filters['username'].value" placeholder="关键字搜索" size="small" />
          </IconField>
        </div>
      </template>

      <template #empty>暂无用户数据</template>
      <template #loading>加载中，请稍候...</template>

      <Column
        field="username"
        header="用户名"
        :showFilterMatchModes="false"
        sortable
        class="min-w-[10rem]"
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
        class="min-w-[10rem]"
      >
        <template #body="{ data }">{{ data.nickname }}</template>
        <template #filter="{ filterModel }">
          <InputText v-model="filterModel.value" type="text" placeholder="请输入昵称" />
        </template>
      </Column>

      <Column field="icon" header="头像" :showFilterMatchModes="false" class="min-w-[6rem]">
        <template #body="{ data }">
          <div
            class="relative group cursor-pointer w-9 h-9"
            @click="triggerAvatarInput(data.id)"
          >
            <Avatar v-if="data.icon" :image="resetUrl(data.icon)" shape="circle" size="normal" />
            <Avatar
              v-else
              :label="data.nickname?.charAt(0) ?? data.username?.charAt(0)"
              shape="circle"
              size="normal"
            />
            <div
              class="absolute inset-0 rounded-full bg-black/50 flex items-center justify-center opacity-0 group-hover:opacity-100 transition-opacity"
            >
              <i class="pi pi-pencil text-white text-xs" />
            </div>
          </div>
        </template>
      </Column>

      <Column
        field="phone"
        header="手机号"
        :showFilterMatchModes="false"
        sortable
        class="min-w-[10rem]"
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
        class="min-w-[12rem]"
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
        class="min-w-[12rem]"
      >
        <template #body="{ data }">{{ data.createTime }}</template>
      </Column>

      <Column field="operate" header="操作" class="min-w-[8rem]">
        <template #body="{ data }">
          <div class="flex items-center gap-2">
            <Button
              icon="pi pi-share-alt"
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

    <div class="flex justify-end mt-2">
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
    </div>

    <ConversationMessageDrawer ref="conversationMessageDrawerRef" />
    <input
      ref="avatarInputRef"
      type="file"
      accept="image/*"
      class="hidden"
      @change="handleAvatarChange"
    />
  </div>

  <CreateUser ref="createUserRef" @success="pageUser" />
  <ResourceAuthDrawer ref="resourceAuthDrawerRef"></ResourceAuthDrawer>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import UserAPI from '@/api/user'
import CreateUser from '@/views/user-management/components/CreateUserDialog.vue'
import type { User } from '@/api/type/user'
import { FilterMatchMode } from '@primevue/core/api'
import { getActiveFilters, resetUrl } from '@/utils/common'
import { type DataTableFilterMeta } from 'primevue/datatable'
import bus from '@/bus'
import fileAPI from '@/api/file'
import ResourceAuthDrawer from '@/views/user-management/components/ResourceAuthDrawer.vue'
const loading = ref<boolean>(false)
const userList = ref<Array<User>>([])
const currentPage = ref<number>(1)
const pageSize = ref<number>(10)
const total = ref<number>(0)
const createUserRef = ref<InstanceType<typeof CreateUser>>()
const resourceAuthDrawerRef = ref<InstanceType<typeof ResourceAuthDrawer>>()
const avatarInputRef = ref<HTMLInputElement>()
const editingUserId = ref<string>()
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

const triggerAvatarInput = (userId: string) => {
  editingUserId.value = userId
  avatarInputRef.value?.click()
}

const handleAvatarChange = (event: Event) => {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (file && editingUserId.value) {
    const fd = new FormData()
    fd.append('file', file)
    fileAPI.uploadFile(fd).then((ok) => {
      UserAPI.updateUser(editingUserId.value!, { icon: `./api/storage/file/${ok.data.id}` }).then(
        () => {
          bus.emit('message:success', '头像更新成功')
          pageUser()
        }
      )
    })
    input.value = ''
  }
}

onMounted(() => pageUser())
</script>

<style lang="scss" scoped>
:deep(.p-datatable-header-cell) {
  padding: 0.3rem 0.5rem;
}
</style>
