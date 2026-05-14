<template>
  <div class="layout-content-height">
    <DataTable
      v-model:filters="filters"
      :value="userList"
      dataKey="id"
      filterDisplay="menu"
      :loading="loading"
      scrollable
      scrollHeight="calc(100vh - 200px)"
      @update:filters="filtersData"
    >
      <template #header>
        <div class="flex items-center justify-between gap-2">
          <Button label="创建用户" icon="pi pi-plus" size="small" @click="openCreateUser" />
          <div class="flex items-center gap-2">
            <InputGroup>
              <InputGroupAddon>
                <Button type="button" icon="pi pi-search" text />
              </InputGroupAddon>
              <InputText v-model="filters['username'].value" placeholder="关键字搜索" size="small" />
            </InputGroup>
            <Button
              ref="settingsButtonRef"
              icon="pi pi-cog"
              severity="secondary"
              text
              rounded
              size="small"
              @click="toggleSettingsMenu"
            />
            <Menu
              ref="settingsMenuRef"
              :model="columnMenuItems"
              :popup="true"
              :appendTo="'body'"
            />
          </div>
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
        v-if="visibleColumns.username"
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
        v-if="visibleColumns.nickname"
      >
        <template #body="{ data }">{{ data.nickname }}</template>
        <template #filter="{ filterModel }">
          <InputText v-model="filterModel.value" type="text" placeholder="请输入昵称" />
        </template>
      </Column>

      <Column field="icon" header="头像" :showFilterMatchModes="false" class="min-w-[6rem]" v-if="visibleColumns.icon">
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
        v-if="visibleColumns.phone"
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
        v-if="visibleColumns.email"
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
        class="min-w-[15rem]"
        v-if="visibleColumns.createTime"
      >
        <template #body="{ data }">{{ data.createTime }}</template>
      </Column>

      <Column field="operate" header="操作" frozen alignFrozen="right" class="min-w-[8rem]">
        <template #body="{ data }">
          <div class="flex items-center gap-2">
            <Button
              icon="pi pi-pencil"
              severity="secondary"
              text
              rounded
              size="small"
              @click="openEditUser(data)"
            />
            <Button
              icon="pi pi-ellipsis-v"
              severity="secondary"
              text
              rounded
              size="small"
              @click="(event) => toggleMoreMenu(data, event)"
            />

          </div>
        </template>
      </Column>
    </DataTable>

    <Menu
      ref="moreMenuRef"
      :model="getMoreMenuItems(activeMoreUser)"
      :popup="true"
      :appendTo="'body'"
    />

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
  <Dialog v-model:visible="passwordDialogVisible" header="修改密码" modal :style="{ width: '24rem' }">
    <div class="flex flex-col gap-4">
      <div class="flex flex-col gap-2">
        <label for="newPassword">新密码</label>
        <Password id="newPassword" v-model="newPassword" placeholder="请输入新密码" :feedback="false" toggleMask fluid />
      </div>
    </div>
    <template #footer>
      <Button label="取消" severity="secondary" @click="passwordDialogVisible = false" />
      <Button label="确认" @click="confirmPasswordChange" />
    </template>
  </Dialog>
</template>

<script setup lang="ts">
import { onMounted, ref, reactive, computed } from 'vue'
import UserAPI from '@/api/user'
import CreateUser from '@/views/user-management/components/CreateUserDialog.vue'
import type { User } from '@/api/type/user'
import { FilterMatchMode } from '@primevue/core/api'
import { getActiveFilters, resetUrl } from '@/utils/common'
import { type DataTableFilterMeta } from 'primevue/datatable'
import bus from '@/bus'
import fileAPI from '@/api/file'
import ResourceAuthDrawer from '@/views/user-management/components/ResourceAuthDrawer.vue'
import Menu from 'primevue/menu'
import Dialog from 'primevue/dialog'
import Password from 'primevue/password'
import InputGroup from 'primevue/inputgroup'
import InputGroupAddon from 'primevue/inputgroupaddon'

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

const visibleColumns = reactive({
  username: true,
  nickname: true,
  icon: true,
  phone: true,
  email: true,
  createTime: true
})

const columnMenuItems = computed(() => [
  {
    label: visibleColumns.username ? '用户名 ✓' : '用户名',
    command: () => { visibleColumns.username = !visibleColumns.username }
  },
  {
    label: visibleColumns.nickname ? '昵称 ✓' : '昵称',
    command: () => { visibleColumns.nickname = !visibleColumns.nickname }
  },
  {
    label: visibleColumns.icon ? '头像 ✓' : '头像',
    command: () => { visibleColumns.icon = !visibleColumns.icon }
  },
  {
    label: visibleColumns.phone ? '手机号 ✓' : '手机号',
    command: () => { visibleColumns.phone = !visibleColumns.phone }
  },
  {
    label: visibleColumns.email ? '邮箱 ✓' : '邮箱',
    command: () => { visibleColumns.email = !visibleColumns.email }
  },
  {
    label: visibleColumns.createTime ? '创建时间 ✓' : '创建时间',
    command: () => { visibleColumns.createTime = !visibleColumns.createTime }
  }
])

const settingsButtonRef = ref()
const settingsMenuRef = ref<InstanceType<typeof Menu>>()

const toggleSettingsMenu = (event: Event) => {
  settingsMenuRef.value?.toggle(event)
}

const moreMenuRef = ref<InstanceType<typeof Menu>>()
const activeMoreUser = ref<User | null>(null)

const toggleMoreMenu = (user: User, event: Event) => {
  activeMoreUser.value = user
  moreMenuRef.value?.toggle(event)
}

const filtersData = (event: DataTableFilterMeta) => {
  const query = getActiveFilters(event)
  pageUser(query)
}

const openCreateUser = () => createUserRef.value?.open()
const openEditUser = (user: User) => createUserRef.value?.open(user)
const openResourceAuth = (user: User) => resourceAuthDrawerRef.value?.open(user.id)
const passwordDialogVisible = ref<boolean>(false)
const newPassword = ref<string>('')
const passwordChangeUserId = ref<string>('')

const getMoreMenuItems = (user: User | null) => {
  if (!user) return []
  const isAdmin = user.username === 'admin'
  return [
    {
      label: '修改密码',
      icon: 'pi pi-lock',
      command: () => openPasswordDialog(user.id)
    },
    {
      label: '授权',
      icon: 'pi pi-share-alt',
      command: () => openResourceAuth(user)
    },
    {
      label: '删除',
      icon: 'pi pi-trash',
      command: () => deleteUser(user.id),
      disabled: isAdmin
    }
  ]
}

const openPasswordDialog = (userId: string) => {
  passwordChangeUserId.value = userId
  newPassword.value = ''
  passwordDialogVisible.value = true
}

const confirmPasswordChange = () => {
  if (!newPassword.value || newPassword.value.length < 6) {
    bus.emit('message:error', '密码长度至少为6位')
    return
  }
  UserAPI.updateUser(passwordChangeUserId.value, { password: newPassword.value }).then(() => {
    bus.emit('message:success', '密码修改成功')
    passwordDialogVisible.value = false
  })
}

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
  const user = userList.value.find(u => u.id === userId)
  if (user?.username === 'admin') {
    bus.emit('message:error', 'admin账号不允许删除')
    return
  }
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
  font-size: 14px;
}

:deep(.p-button) {
  font-size: 14px;
}

:deep(.p-inputtext) {
  font-size: 14px;
}

:deep(.p-datatable-column-filter) {
  .p-button {
    padding: 0.25rem;
    .p-icon {
      width: 12.25px;
      height: 12.25px;
    }
  }
}

:deep(.p-sortable-column-icon) {
  width: 12.25px;
  height: 12.25px;
}

:deep(.p-datatable-scrollable .p-datatable-frozen-right) {
  box-shadow: -2px 0 8px rgba(0, 0, 0, 0.05);
}

:deep(.p-datatable-scrollable .p-datatable-frozen-right .p-datatable-header-cell) {
  background: #f8f9fa !important;
}

:deep(.p-datatable-scrollable .p-datatable-frozen-right .p-datatable-body-cell) {
  background: white;
}

:deep(.p-paginator) {
  font-size: 14px;
}

</style>
