<template>
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
              <Button icon="pi pi-minus" text rounded size="small" aria-label="折叠" :disabled="true" />
            </th>
          </tr>
        </thead>

        <tbody v-if="permissionGroups.length">
          <template v-for="group in permissionGroups" :key="group.groupKey">
            <tr v-for="(row, rowIndex) in group.rows" :key="row.rowKey" class="align-top">
              <td
                v-if="rowIndex === 0"
                :rowspan="group.rows.length"
                class="border-r border-b border-slate-200 px-3 py-4 align-middle text-sm font-medium text-slate-800"
              >
                <div class="leading-5">{{ group.groupLabel }}</div>
              </td>

              <td
                class="border-r border-b border-slate-200 px-3 py-4 align-middle text-sm text-slate-700"
              >
                <div class="leading-5">{{ row.subGroupLabel }}</div>
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
                      @update:model-value="emit('toggle-permission', perm.permission, $event)"
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
                  @update:model-value="emit('toggle-row', row, $event)"
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
</template>

<script setup lang="ts">
import Button from 'primevue/button'
import Checkbox from 'primevue/checkbox'
import type { PermissionGroupBlock, PermissionRow } from '../types'

const props = defineProps<{
  permissionGroups: PermissionGroupBlock[]
  checkedPermissions: Set<string>
  canEditCurrentRole: boolean
  loadingPermissions: boolean
}>()

const emit = defineEmits<{
  (e: 'toggle-permission', permission: string, checked: boolean): void
  (e: 'toggle-row', row: PermissionRow, checked: boolean): void
}>()

function isRowChecked(row: PermissionRow): boolean {
  if (!row.permissions.length) return false
  return row.permissions.every((item) => props.checkedPermissions.has(item.permission))
}

function isRowIndeterminate(row: PermissionRow): boolean {
  if (!row.permissions.length) return false

  const checkedCount = row.permissions.filter((item) =>
    props.checkedPermissions.has(item.permission)
  ).length

  return checkedCount > 0 && checkedCount < row.permissions.length
}
</script>
