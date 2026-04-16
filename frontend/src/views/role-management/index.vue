<template>
  <div
    class="flex h-full min-h-0 overflow-hidden bg-white text-sm text-slate-800 card layout-content-container"
  >
    <!-- 左侧角色列表 -->
    <aside class="flex w-[248px] shrink-0 flex-col border-r border-slate-200 bg-slate-50/40 p-3">
      <div class="mb-3">
        <IconField>
          <InputIcon class="pi pi-search text-xs" />
          <InputText v-model="keyword" placeholder="搜索角色" class="w-full" size="small" />
        </IconField>
      </div>

      <div class="min-h-0 flex-1 overflow-y-auto pr-1">
        <section class="mb-5">
          <div class="mb-2 text-xs font-medium tracking-wide text-slate-500">系统内置角色</div>

          <div v-if="builtinRoles.length" class="space-y-1.5">
            <button
              v-for="role in builtinRoles"
              :key="role.id"
              type="button"
              class="flex w-full items-center rounded-lg px-2.5 py-2 text-left transition-colors"
              :class="
                selectedRole?.id === role.id
                  ? 'bg-blue-50 text-blue-600 ring-1 ring-blue-200'
                  : 'text-slate-700 hover:bg-slate-100'
              "
              @click="handleSelectRole(role)"
            >
              <div class="min-w-0 flex-1">
                <div class="truncate text-sm font-medium leading-5">
                  {{ role.name }}
                </div>
                <div class="mt-0.5 truncate text-xs text-slate-400">
                  {{ inheritedRoleLabel(role.type) }}
                </div>
              </div>
            </button>
          </div>

          <div
            v-else
            class="rounded-lg border border-dashed border-slate-200 px-3 py-4 text-xs text-slate-400"
          >
            暂无内置角色
          </div>
        </section>

        <section>
          <div class="mb-2 flex items-center justify-between">
            <span class="text-xs font-medium tracking-wide text-slate-500">自定义角色</span>
            <Button
              icon="pi pi-plus"
              text
              rounded
              severity="secondary"
              size="small"
              aria-label="新增角色"
              @click="openCreateDialog"
            />
          </div>

          <div v-if="customRoles.length" class="space-y-1.5">
            <button
              v-for="role in customRoles"
              :key="role.id"
              type="button"
              class="group flex w-full items-center rounded-lg px-2.5 py-2 text-left transition-colors"
              :class="
                selectedRole?.id === role.id
                  ? 'bg-blue-50 text-blue-600 ring-1 ring-blue-200'
                  : 'text-slate-700 hover:bg-slate-100'
              "
              @click="handleSelectRole(role)"
            >
              <div class="min-w-0 flex-1">
                <div class="truncate text-sm font-medium leading-5">
                  {{ role.name }}
                </div>
                <div class="mt-0.5 truncate text-xs text-slate-400">
                  {{ inheritedRoleLabel(role.type) }}
                </div>
              </div>

              <Button
                icon="pi pi-ellipsis-h"
                text
                rounded
                severity="secondary"
                size="small"
                class="ml-1 opacity-0 transition-opacity group-hover:opacity-100"
                aria-label="更多"
                @click.stop
              />
            </button>
          </div>

          <div
            v-else
            class="rounded-lg border border-dashed border-slate-200 px-3 py-5 text-center text-xs text-slate-400"
          >
            暂无自定义角色
          </div>
        </section>
      </div>
    </aside>

    <!-- 右侧权限区域 -->
    <main class="flex min-w-0 flex-1 flex-col p-4">
      <div class="mb-4 flex flex-wrap items-center justify-between gap-3">
        <div class="min-w-0">
          <div class="flex flex-wrap items-center gap-2">
            <div class="truncate text-xl font-semibold leading-6 text-slate-900">
              {{ selectedRole?.name || '角色管理' }}
            </div>

            <span class="text-sm font-medium text-slate-400">
              ({{ inheritedRoleLabel(selectedRole?.type) }})
            </span>

            <span class="flex items-center gap-1 text-xs text-slate-400">
              <i class="pi pi-users text-xs" />
              <span>0</span>
            </span>

            <Tag
              v-if="selectedRole && !canEditCurrentRole"
              severity="secondary"
              value="内置角色不可编辑"
              class="ml-1"
            />
          </div>
        </div>

        <Tabs value="permission" class="w-auto">
          <TabList>
            <Tab value="permission">权限配置</Tab>
            <Tab value="member">成员</Tab>
          </TabList>
        </Tabs>
      </div>

      <div class="min-h-0 flex-1 overflow-hidden rounded-xl border border-slate-200 bg-white">
        <div class="h-full overflow-auto">
          <table class="min-w-full border-collapse text-sm">
            <thead class="sticky top-0 z-10 bg-slate-50">
              <tr class="text-slate-500">
                <th
                  class="w-[160px] border-b border-r border-slate-200 px-3 py-2.5 text-left text-xs font-medium"
                >
                  模块名称
                </th>
                <th
                  class="w-[180px] border-b border-r border-slate-200 px-3 py-2.5 text-left text-xs font-medium"
                >
                  操作对象
                </th>
                <th
                  class="border-b border-r border-slate-200 px-3 py-2.5 text-left text-xs font-medium"
                >
                  权限
                </th>
                <th
                  class="w-[56px] border-b border-slate-200 px-2 py-2.5 text-center text-xs font-medium"
                >
                  <Button
                    icon="pi pi-minus"
                    text
                    rounded
                    size="small"
                    aria-label="折叠"
                    :disabled="true"
                  />
                </th>
              </tr>
            </thead>

            <tbody v-if="permissionRows.length">
              <template v-for="group in permissionGroups" :key="group.groupKey">
                <tr v-for="(row, rowIndex) in group.rows" :key="row.rowKey" class="align-top">
                  <td
                    v-if="rowIndex === 0"
                    :rowspan="group.rows.length"
                    class="border-r border-b border-slate-200 px-3 py-4 align-middle text-sm font-medium text-slate-800"
                  >
                    <div class="leading-5">
                      {{ group.groupLabel }}
                    </div>
                  </td>

                  <td
                    class="border-r border-b border-slate-200 px-3 py-4 align-middle text-sm text-slate-700"
                  >
                    <div class="leading-5">
                      {{ row.subGroupLabel }}
                    </div>
                  </td>

                  <td class="border-r border-b border-slate-200 px-3 py-3">
                    <div class="grid grid-cols-1 gap-x-6 gap-y-2 md:grid-cols-2 xl:grid-cols-3">
                      <label
                        v-for="perm in row.permissions"
                        :key="perm.key"
                        class="flex items-center gap-2 text-sm"
                        :class="canEditCurrentRole ? 'text-slate-700' : 'text-slate-400'"
                      >
                        <Checkbox
                          :model-value="checkedPermissions.has(perm.permission)"
                          :binary="true"
                          :disabled="!canEditCurrentRole"
                          @update:model-value="togglePermission(perm.permission, $event)"
                        />
                        <span class="leading-5">{{ perm.label }}</span>
                        <i
                          v-if="!canEditCurrentRole"
                          class="pi pi-lock text-[11px] text-slate-400"
                        />
                      </label>
                    </div>
                  </td>

                  <td class="border-b border-slate-200 px-2 py-3 text-center align-middle">
                    <Checkbox
                      :model-value="isRowChecked(row)"
                      :indeterminate="isRowIndeterminate(row)"
                      :binary="true"
                      :disabled="!canEditCurrentRole"
                      @update:model-value="toggleRow(row, $event)"
                    />
                  </td>
                </tr>
              </template>
            </tbody>

            <tbody v-else>
              <tr>
                <td colspan="4" class="px-6 py-14 text-center text-sm text-slate-400">
                  {{ loadingPermissions ? '权限加载中...' : '暂无权限数据' }}
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <div class="mt-4 flex justify-end">
        <Button
          label="保存"
          size="small"
          class="min-w-[88px]"
          :loading="saving"
          :disabled="!selectedRoleId || !canEditCurrentRole"
          @click="handleSave"
        />
      </div>
    </main>

    <!-- 新增角色弹窗 -->
    <Dialog
      v-model:visible="createDialogVisible"
      modal
      header="新增角色"
      :style="{ width: '28rem' }"
      :draggable="false"
    >
      <div class="space-y-4">
        <div>
          <label class="mb-1.5 block text-sm font-medium text-slate-700"> 角色名称 </label>
          <InputText
            v-model.trim="createForm.name"
            class="w-full"
            maxlength="50"
            placeholder="请输入角色名称"
          />
        </div>

        <div>
          <label class="mb-1.5 block text-sm font-medium text-slate-700"> 继承角色 </label>
          <Select
            v-model="createForm.type"
            :options="inheritRoleOptions"
            option-label="label"
            option-value="value"
            placeholder="请选择继承角色"
            class="w-full"
          />
        </div>
      </div>

      <template #footer>
        <div class="flex justify-end gap-2">
          <Button label="取消" text severity="secondary" @click="createDialogVisible = false" />
          <Button label="创建" :loading="creatingRole" @click="handleCreateRole" />
        </div>
      </template>
    </Dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import Button from 'primevue/button'
import Checkbox from 'primevue/checkbox'
import Dialog from 'primevue/dialog'
import IconField from 'primevue/iconfield'
import InputIcon from 'primevue/inputicon'
import InputText from 'primevue/inputtext'
import Select from 'primevue/select'
import Tabs from 'primevue/tabs'
import TabList from 'primevue/tablist'
import Tab from 'primevue/tab'
import Tag from 'primevue/tag'
import { useToast } from 'primevue/usetoast'

import roleAPI from '@/api/role'

interface RoleItem {
  id: string
  name: string
  internal?: boolean
  type?: 'ADMIN' | 'USER' | string | null
  createTime?: string
  updateTime?: string
}

interface PermissionItem {
  groupLabel: string
  subGroupLabel: string
  permissionGroupLabel?: string | null
  group: string
  subGroup: string
  permission: string
  selected?: boolean
}

interface PermissionOption {
  key: string
  label: string
  permission: string
  selected: boolean
}

interface PermissionRow {
  rowKey: string
  group: string
  groupLabel: string
  subGroup: string
  subGroupLabel: string
  permissions: PermissionOption[]
}

interface PermissionGroupBlock {
  groupKey: string
  groupLabel: string
  rows: PermissionRow[]
}

type InheritRoleType = 'ADMIN' | 'USER'

const toast = useToast()

const roles = ref<RoleItem[]>([])
const keyword = ref('')
const selectedRoleId = ref<string>('')
const loadingRoles = ref(false)
const loadingPermissions = ref(false)
const saving = ref(false)
const creatingRole = ref(false)

const permissionRows = ref<PermissionRow[]>([])
const checkedPermissions = ref<Set<string>>(new Set())
const originalPermissions = ref<Set<string>>(new Set())

const createDialogVisible = ref(false)
const createForm = ref<{
  name: string
  type: InheritRoleType | null
}>({
  name: '',
  type: 'USER'
})

const inheritRoleOptions = [
  { label: '系统管理员', value: 'ADMIN' },
  { label: '普通用户', value: 'USER' }
]

const selectedRole = computed(() => {
  return roles.value.find((item) => item.id === selectedRoleId.value) ?? null
})

const canEditCurrentRole = computed(() => {
  return !!selectedRole.value && !isBuiltinRole(selectedRole.value)
})

const filteredRoles = computed(() => {
  const text = keyword.value.trim().toLowerCase()
  if (!text) return roles.value

  return roles.value.filter((role) => {
    const name = role.name?.toLowerCase() ?? ''
    const id = role.id?.toLowerCase() ?? ''
    const type = role.type?.toLowerCase() ?? ''
    return name.includes(text) || id.includes(text) || type.includes(text)
  })
})

const builtinRoles = computed(() => {
  return filteredRoles.value.filter((role) => isBuiltinRole(role))
})

const customRoles = computed(() => {
  return filteredRoles.value.filter((role) => !isBuiltinRole(role))
})

const permissionGroups = computed<PermissionGroupBlock[]>(() => {
  const map = new Map<string, PermissionGroupBlock>()

  for (const row of permissionRows.value) {
    const groupKey = row.group || row.groupLabel
    if (!map.has(groupKey)) {
      map.set(groupKey, {
        groupKey,
        groupLabel: row.groupLabel,
        rows: []
      })
    }
    map.get(groupKey)!.rows.push(row)
  }

  return [...map.values()]
})

function isBuiltinRole(role: RoleItem): boolean {
  return role.id === 'ADMIN' || role.id === 'USER' || !!role.internal
}

function inheritedRoleLabel(type?: string | null): string {
  if (!type) return '-'
  if (type === 'ADMIN') return '系统管理员'
  if (type === 'USER') return '普通用户'
  return type
}

function normalizePermissionLabel(item: PermissionItem): string {
  return item.permissionGroupLabel?.trim() || item.permission
}

function buildPermissionRows(data: PermissionItem[]): PermissionRow[] {
  const rowMap = new Map<string, PermissionRow>()

  for (const item of data) {
    const rowKey = `${item.group}::${item.subGroup}`

    if (!rowMap.has(rowKey)) {
      rowMap.set(rowKey, {
        rowKey,
        group: item.group,
        groupLabel: item.groupLabel,
        subGroup: item.subGroup,
        subGroupLabel: item.subGroupLabel,
        permissions: []
      })
    }

    const row = rowMap.get(rowKey)!
    row.permissions.push({
      key: `${rowKey}::${item.permission}`,
      label: normalizePermissionLabel(item),
      permission: item.permission,
      selected: item.selected === true
    })
  }

  return [...rowMap.values()]
}

function cloneSet(source: Set<string>): Set<string> {
  return new Set(Array.from(source))
}

function openCreateDialog(): void {
  createForm.value = {
    name: '',
    type: 'USER'
  }
  createDialogVisible.value = true
}

function handleSelectRole(role: RoleItem): void {
  if (selectedRoleId.value === role.id) return
  selectedRoleId.value = role.id
  void loadPermissions(role.id)
}

async function loadRoles(): Promise<void> {
  loadingRoles.value = true
  try {
    const data = await roleAPI.listRoles()
    roles.value = Array.isArray(data.data) ? data.data : []

    if (!selectedRoleId.value && roles.value.length) {
      const defaultRole = roles.value.find((role) => role.id === 'ADMIN') ?? roles.value[0]
      selectedRoleId.value = defaultRole.id
    }
  } finally {
    loadingRoles.value = false
  }
}

async function loadPermissions(roleId: string): Promise<void> {
  loadingPermissions.value = true
  try {
    const data = await roleAPI.listPermission(roleId)
    const list = Array.isArray(data.data) ? data.data : []

    permissionRows.value = buildPermissionRows(list)

    const selected = new Set<string>(
      list.filter((item) => item.selected === true).map((item) => item.permission)
    )

    checkedPermissions.value = selected
    originalPermissions.value = cloneSet(selected)
  } finally {
    loadingPermissions.value = false
  }
}

async function handleCreateRole(): Promise<void> {
  const name = createForm.value.name.trim()
  const type = createForm.value.type

  if (!name) {
    toast.add({
      severity: 'warn',
      summary: '提示',
      detail: '请输入角色名称',
      life: 2500
    })
    return
  }

  if (!type) {
    toast.add({
      severity: 'warn',
      summary: '提示',
      detail: '请选择继承角色',
      life: 2500
    })
    return
  }

  creatingRole.value = true
  try {
    const res = await roleAPI.createRole({
      name,
      type
    })

    const newRoleId = res?.data?.id || res?.data?.roleId

    toast.add({
      severity: 'success',
      summary: '成功',
      detail: '角色创建成功',
      life: 2500
    })

    createDialogVisible.value = false
    await loadRoles()

    if (newRoleId) {
      selectedRoleId.value = newRoleId
      await loadPermissions(newRoleId)
    } else {
      const matched = roles.value.find((item) => item.name === name && item.type === type)
      if (matched) {
        selectedRoleId.value = matched.id
        await loadPermissions(matched.id)
      }
    }
  } finally {
    creatingRole.value = false
  }
}

function togglePermission(permission: string, checked: boolean): void {
  if (!canEditCurrentRole.value) return

  const next = cloneSet(checkedPermissions.value)
  if (checked) {
    next.add(permission)
  } else {
    next.delete(permission)
  }
  checkedPermissions.value = next
}

function isRowChecked(row: PermissionRow): boolean {
  if (!row.permissions.length) return false
  return row.permissions.every((item) => checkedPermissions.value.has(item.permission))
}

function isRowIndeterminate(row: PermissionRow): boolean {
  if (!row.permissions.length) return false

  const checkedCount = row.permissions.filter((item) =>
    checkedPermissions.value.has(item.permission)
  ).length

  return checkedCount > 0 && checkedCount < row.permissions.length
}

function toggleRow(row: PermissionRow, checked: boolean): void {
  if (!canEditCurrentRole.value) return

  const next = cloneSet(checkedPermissions.value)
  for (const item of row.permissions) {
    if (checked) {
      next.add(item.permission)
    } else {
      next.delete(item.permission)
    }
  }
  checkedPermissions.value = next
}

async function handleSave(): Promise<void> {
  if (!selectedRoleId.value || !canEditCurrentRole.value) return

  saving.value = true
  try {
    const permissions = [...checkedPermissions.value]

    await roleAPI.modifyPermissions(selectedRoleId.value, {
      permissions
    })

    originalPermissions.value = cloneSet(checkedPermissions.value)

    toast.add({
      severity: 'success',
      summary: '成功',
      detail: '权限保存成功',
      life: 2500
    })
  } finally {
    saving.value = false
  }
}

onMounted(async () => {
  await loadRoles()
  if (selectedRoleId.value) {
    await loadPermissions(selectedRoleId.value)
  }
})
</script>

<style scoped>
:deep(.p-inputtext),
:deep(.p-button),
:deep(.p-tab),
:deep(.p-select),
:deep(.p-tag) {
  border-radius: 0.5rem;
}

:deep(.p-inputtext) {
  padding: 0.5rem 0.75rem;
  font-size: 0.875rem;
}

:deep(.p-select) {
  min-height: 2.5rem;
}

:deep(.p-button) {
  padding: 0.45rem 0.7rem;
}

:deep(.p-button.p-button-icon-only) {
  width: 2rem;
  height: 2rem;
}

:deep(.p-tabs-tablist) {
  border: 1px solid rgb(226 232 240);
  border-radius: 0.625rem;
  padding: 0.125rem;
  background: white;
}

:deep(.p-tab) {
  padding: 0.45rem 0.75rem;
  font-size: 0.875rem;
}

:deep(.p-tab-active) {
  background: rgb(239 246 255);
  color: rgb(37 99 235);
}

:deep(.p-checkbox) {
  transform: scale(0.92);
}

:deep(.p-dialog-header) {
  padding-bottom: 0.75rem;
}

:deep(.p-dialog-content) {
  padding-top: 0.25rem;
}
</style>
