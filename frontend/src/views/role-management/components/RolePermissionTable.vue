<template>
  <div class="min-h-0 flex-1 overflow-hidden rounded-lg border border-surface-200 bg-surface-0 dark:border-surface-700 dark:bg-surface-900">
    <div class="h-full overflow-auto">
      <table class="min-w-full border-collapse text-sm">
        <thead class="sticky top-0 z-10 bg-surface-50 dark:bg-surface-800">
          <tr class="text-surface-500">
            <th
              class="w-[160px] border-b border-r border-surface-200 px-2.5 py-2 text-left text-xs font-medium dark:border-surface-700"
            >
              模块名称
            </th>
            <th
              class="w-[180px] border-b border-r border-surface-200 px-2.5 py-2 text-left text-xs font-medium dark:border-surface-700"
            >
              操作对象
            </th>
            <th
              class="border-b border-r border-surface-200 px-2.5 py-2 text-left text-xs font-medium dark:border-surface-700"
            >
              权限
            </th>
            <th
              class="w-[56px] border-b border-surface-200 px-2 py-2 text-center text-xs font-medium dark:border-surface-700"
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
                class="border-r border-b border-surface-200 px-2.5 py-3 align-middle text-sm font-medium text-surface-800 dark:border-surface-700 dark:text-surface-200"
              >
                <div class="leading-5">{{ group.groupLabel }}</div>
              </td>

              <td
                class="border-r border-b border-surface-200 px-2.5 py-3 align-middle text-sm text-surface-700 dark:border-surface-700 dark:text-surface-300"
              >
                <div class="leading-5">{{ row.subGroupLabel }}</div>
              </td>

              <td class="border-r border-b border-surface-200 px-2.5 py-2.5 dark:border-surface-700">
                <div class="grid grid-cols-1 gap-x-4 gap-y-1.5 md:grid-cols-2 xl:grid-cols-3">
                  <label
                    v-for="perm in row.permissions"
                    :key="perm.key"
                    class="flex items-center gap-2 text-sm"
                    :class="canEditCurrentRole ? 'text-surface-700 dark:text-surface-300' : 'text-surface-400'"
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
                      class="pi pi-lock text-[11px] text-surface-400"
                    />
                  </label>
                </div>
              </td>

              <td class="border-b border-surface-200 px-2 py-2.5 text-center align-middle dark:border-surface-700">
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
            <td colspan="4" class="px-6 py-14 text-center text-sm text-surface-400">
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
