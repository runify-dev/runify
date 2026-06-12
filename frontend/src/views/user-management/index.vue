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
          <Button :label="t('system.createUser')" icon="pi pi-plus" size="small" @click="openCreateUser" />
          <div class="flex items-center gap-2">
            <InputGroup>
              <InputGroupAddon>
                <Button type="button" icon="pi pi-search" text />
              </InputGroupAddon>
              <InputText v-model="filters['username'].value" :placeholder="t('system.search')" size="small" />
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

      <template #empty>{{ t('system.noData') }}</template>
      <template #loading>{{ t('system.loading') }}</template>

      <Column
        field="username"
        :header="t('system.username')"
        :showFilterMatchModes="false"
        sortable
        class="min-w-[10rem]"
        v-if="visibleColumns.username"
      >
        <template #body="{ data }">{{ data.username }}</template>
        <template #filter="{ filterModel }">
          <InputText v-model="filterModel.value" type="text" :placeholder="t('system.usernamePlaceholder')" />
        </template>
      </Column>

      <Column
        field="nickname"
        :header="t('system.nickname')"
        :showFilterMatchModes="false"
        sortable
        class="min-w-[10rem]"
        v-if="visibleColumns.nickname"
      >
        <template #body="{ data }">{{ data.nickname }}</template>
        <template #filter="{ filterModel }">
          <InputText v-model="filterModel.value" type="text" :placeholder="t('system.nicknamePlaceholder')" />
        </template>
      </Column>

      <Column field="icon" :header="t('system.avatar')" :showFilterMatchModes="false" class="min-w-[6rem]" v-if="visibleColumns.icon">
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
        :header="t('system.phone')"
        :showFilterMatchModes="false"
        sortable
        class="min-w-[10rem]"
        v-if="visibleColumns.phone"
      >
        <template #body="{ data }">{{ data.phone }}</template>
        <template #filter="{ filterModel }">
          <InputText v-model="filterModel.value" type="text" :placeholder="t('system.phonePlaceholder')" />
        </template>
      </Column>

      <Column
        field="email"
        :header="t('system.email')"
        :showFilterMatchModes="false"
        sortable
        class="min-w-[12rem]"
        v-if="visibleColumns.email"
      >
        <template #body="{ data }">{{ data.email }}</template>
        <template #filter="{ filterModel }">
          <InputText v-model="filterModel.value" type="text" :placeholder="t('system.emailPlaceholder')" />
        </template>
      </Column>

      <Column
        field="createTime"
        :header="t('system.createTime')"
        :showFilterMatchModes="false"
        sortable
        class="min-w-[15rem]"
        v-if="visibleColumns.createTime"
      >
        <template #body="{ data }">{{ data.createTime }}</template>
      </Column>

      <Column field="operate" :header="t('common.operation')" frozen alignFrozen="right" class="min-w-[8rem]">
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
  <Dialog v-model:visible="passwordDialogVisible" :header="t('system.changePassword')" modal :style="{ width: '24rem' }">
    <div class="flex flex-col gap-4">
      <div class="flex flex-col gap-2">
        <label for="newPassword">{{ t('system.newPassword') }}</label>
        <Password id="newPassword" v-model="newPassword" :placeholder="t('system.newPasswordPlaceholder')" :feedback="false" toggleMask fluid />
      </div>
    </div>
    <template #footer>
      <Button :label="t('common.cancel')" severity="secondary" @click="passwordDialogVisible = false" />
      <Button :label="t('common.confirm')" @click="confirmPasswordChange" />
    </template>
  </Dialog>
</template>

<script setup lang="ts">
import { onMounted, ref, reactive, computed } from 'vue'
import UserAPI from '@/api/user'
import CreateUser from '@/views/user-management/components/CreateUserDialog.vue'
import { t } from '@/locales'
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
    label: visibleColumns.username ? `${t('system.username')} ✓` : t('system.username'),
    command: () => { visibleColumns.username = !visibleColumns.username }
  },
  {
    label: visibleColumns.nickname ? `${t('system.nickname')} ✓` : t('system.nickname'),
    command: () => { visibleColumns.nickname = !visibleColumns.nickname }
  },
  {
    label: visibleColumns.icon ? `${t('system.avatar')} ✓` : t('system.avatar'),
    command: () => { visibleColumns.icon = !visibleColumns.icon }
  },
  {
    label: visibleColumns.phone ? `${t('system.phone')} ✓` : t('system.phone'),
    command: () => { visibleColumns.phone = !visibleColumns.phone }
  },
  {
    label: visibleColumns.email ? `${t('system.email')} ✓` : t('system.email'),
    command: () => { visibleColumns.email = !visibleColumns.email }
  },
  {
    label: visibleColumns.createTime ? `${t('system.createTime')} ✓` : t('system.createTime'),
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
      label: t('system.changePassword'),
      icon: 'pi pi-lock',
      command: () => openPasswordDialog(user.id)
    },
    {
      label: t('system.authorize'),
      icon: 'pi pi-share-alt',
      command: () => openResourceAuth(user),
      disabled: isAdmin
    },
    {
      label: t('common.delete'),
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
    bus.emit('message:error', t('system.passwordMinLength'))
    return
  }
  UserAPI.updateUser(passwordChangeUserId.value, { password: newPassword.value }).then(() => {
    bus.emit('message:success', t('system.passwordChangeSuccess'))
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
    bus.emit('message:error', t('system.adminDeleteForbidden'))
    return
  }
  UserAPI.deleteUser(userId).then(() => {
    bus.emit('message:success', t('system.deleteSuccess'))
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
          bus.emit('message:success', t('system.avatarUpdateSuccess'))
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
  box-shadow: var(--p-shadow-1);
}

:deep(.p-datatable-scrollable .p-datatable-frozen-right .p-datatable-header-cell) {
  background: var(--p-surface-50) !important;
}

:deep(.p-datatable-scrollable .p-datatable-frozen-right .p-datatable-body-cell) {
  background: var(--p-surface-0);
}

:deep(.p-paginator) {
  font-size: 14px;
}

</style>
