<template>
  <div
    class="flex h-full min-h-0 overflow-hidden bg-surface-0 text-sm text-surface-800 dark:bg-surface-900 dark:text-surface-100 layout-content-container"
  >
    <RoleSidebar
      v-model:keyword="keyword"
      :builtin-roles="builtinRoles"
      :custom-roles="customRoles"
      :selected-role-id="selectedRoleId"
      :inherited-role-label="inheritedRoleLabel"
      @select="handleSelectRole"
      @create="openCreateDialog"
    />

    <main class="flex min-w-0 flex-1 flex-col p-3">
      <RoleHeaderBar
        v-model:active-tab="activeTab"
        :role-name="selectedRole?.name"
        :role-type="selectedRole?.type"
        :member-total="memberPage.total"
        :show-readonly-tag="!!selectedRole && !canEditCurrentRole"
        :inherited-role-label="inheritedRoleLabel"
      />

      <RolePermissionTable
        v-if="activeTab === 'permission'"
        :permission-groups="permissionGroups"
        :checked-permissions="checkedPermissions"
        :can-edit-current-role="canEditCurrentRole"
        :loading-permissions="loadingPermissions"
        @toggle-permission="togglePermission"
        @toggle-row="toggleRow"
      />

      <RoleMemberTable
        v-else
        v-model:member-search-field="memberSearchField"
        v-model:member-search-keyword="memberSearchKeyword"
        :selected-role-id="selectedRoleId"
        :member-page="memberPage"
        :member-query="memberQuery"
        :loading-members="loadingMembers"
        :member-search-field-options="memberSearchFieldOptions"
        :removing-user-id="removingUserId"
        @search="handleMemberSearch"
        @add="openAddUserDialog"
        @remove="removeUser"
        @page-change="handleMemberPageChange"
      />

      <div v-if="activeTab === 'permission'" class="mt-2 flex justify-end">
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

    <CreateRoleDialog
      v-model:visible="createDialogVisible"
      v-model:name="createForm.name"
      v-model:type="createForm.type"
      :loading="creatingRole"
      :inherit-role-options="inheritRoleOptions"
      @confirm="handleCreateRole"
    />

    <AddUserDialog
      v-model:visible="addUserDialogVisible"
      v-model:search-field="addUserSearchField"
      v-model:search-keyword="addUserSearchKeyword"
      :loading="loadingAvailableUsers"
      :submitting="addingUsers"
      :selected-role-id="selectedRoleId"
      :member-search-field-options="memberSearchFieldOptions"
      :available-users="availableUsers"
      :selected-add-user-ids="selectedAddUserIds"
      :member-user-id-set="memberUserIdSet"
      @search="loadAvailableUsers"
      @toggle-user="toggleAddUser"
      @confirm="handleConfirmAddUsers"
    />
  </div>
</template>

<script setup lang="ts">
import Button from 'primevue/button'
import AddUserDialog from './components/AddUserDialog.vue'
import CreateRoleDialog from './components/CreateRoleDialog.vue'
import RoleHeaderBar from './components/RoleHeaderBar.vue'
import RoleMemberTable from './components/RoleMemberTable.vue'
import RolePermissionTable from './components/RolePermissionTable.vue'
import RoleSidebar from './components/RoleSidebar.vue'
import { useRoleManage } from './composables/useRoleManage'

const {
  activeTab,
  keyword,
  selectedRoleId,
  selectedRole,
  builtinRoles,
  customRoles,
  loadingPermissions,
  loadingMembers,
  loadingAvailableUsers,
  saving,
  creatingRole,
  addingUsers,
  removingUserId,
  permissionGroups,
  checkedPermissions,
  canEditCurrentRole,
  memberQuery,
  memberPage,
  memberSearchField,
  memberSearchKeyword,
  memberSearchFieldOptions,
  memberUserIdSet,
  addUserDialogVisible,
  addUserSearchField,
  addUserSearchKeyword,
  availableUsers,
  selectedAddUserIds,
  createDialogVisible,
  createForm,
  inheritRoleOptions,
  inheritedRoleLabel,
  openCreateDialog,
  openAddUserDialog,
  handleSelectRole,
  handleCreateRole,
  togglePermission,
  toggleRow,
  handleSave,
  handleMemberSearch,
  handleMemberPageChange,
  toggleAddUser,
  handleConfirmAddUsers,
  removeUser,
  loadAvailableUsers
} = useRoleManage()
</script>

<style scoped>
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
