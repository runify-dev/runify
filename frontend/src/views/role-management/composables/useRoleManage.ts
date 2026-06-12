import { computed, onMounted, ref, watch } from 'vue'
import { useToast } from 'primevue/usetoast'
import type { PageState } from 'primevue/paginator'
import roleAPI from '@/api/role'
import userAPI from '@/api/user'
import { t } from '@/locales'
import type {
  InheritRoleType,
  MemberSearchField,
  PageResult,
  PermissionGroupBlock,
  PermissionItem,
  PermissionRow,
  RoleItem,
  UserItem
} from '../types'
import {
  buildPermissionGroups,
  buildPermissionRows,
  cloneSet,
  inheritedRoleLabel,
  isBuiltinRole
} from '../utils'

export function useRoleManage() {
  const toast = useToast()

  const activeTab = ref<'permission' | 'member'>('permission')

  const roles = ref<RoleItem[]>([])
  const keyword = ref('')
  const selectedRoleId = ref('')

  const loadingRoles = ref(false)
  const loadingPermissions = ref(false)
  const loadingMembers = ref(false)
  const loadingAvailableUsers = ref(false)
  const saving = ref(false)
  const creatingRole = ref(false)
  const addingUsers = ref(false)
  const removingUserId = ref('')

  const permissionRows = ref<PermissionRow[]>([])
  const checkedPermissions = ref<Set<string>>(new Set())
  const originalPermissions = ref<Set<string>>(new Set())

  const memberQuery = ref({
    currentPage: 1,
    pageSize: 20
  })

  const memberPage = ref<PageResult<UserItem>>({
    records: [],
    current: 1,
    size: 20,
    total: 0
  })

  const memberSearchField = ref<MemberSearchField>('username')
  const memberSearchKeyword = ref('')

  const memberSearchFieldOptions = computed(() => [
    { label: t('system.username'), value: 'username' as const },
    { label: t('system.nickname'), value: 'nickname' as const }
  ])

  const addUserDialogVisible = ref(false)
  const addUserSearchField = ref<MemberSearchField>('username')
  const addUserSearchKeyword = ref('')
  const availableUsers = ref<UserItem[]>([])
  const selectedAddUserIds = ref<Set<string>>(new Set())

  const createDialogVisible = ref(false)
  const createForm = ref<{
    name: string
    type: InheritRoleType | null
  }>({
    name: '',
    type: 'USER'
  })

  const inheritRoleOptions = computed(() => [
    { label: t('role.adminRole'), value: 'ADMIN' as const },
    { label: t('role.userRole'), value: 'USER' as const }
  ])

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
    return buildPermissionGroups(permissionRows.value)
  })

  const memberUserIdSet = computed(() => {
    return new Set(memberPage.value.records.map((item) => item.id))
  })

  function openCreateDialog(): void {
    createForm.value = {
      name: '',
      type: 'USER'
    }
    createDialogVisible.value = true
  }

  function openAddUserDialog(): void {
    if (!selectedRoleId.value) return

    addUserSearchField.value = 'username'
    addUserSearchKeyword.value = ''
    selectedAddUserIds.value = new Set()
    addUserDialogVisible.value = true
    void loadAvailableUsers()
  }

  function handleSelectRole(role: RoleItem): void {
    if (selectedRoleId.value === role.id) return
    memberQuery.value.currentPage = 1
    selectedRoleId.value = role.id
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
      const list = Array.isArray(data.data) ? (data.data as PermissionItem[]) : []

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

  async function loadMembers(): Promise<void> {
    if (!selectedRoleId.value) {
      memberPage.value = {
        records: [],
        current: 1,
        size: memberQuery.value.pageSize,
        total: 0
      }
      return
    }

    loadingMembers.value = true
    try {
      const params: Record<string, unknown> = {
        currentPage: memberQuery.value.currentPage,
        pageSize: memberQuery.value.pageSize
      }

      const keyword = memberSearchKeyword.value.trim()
      if (keyword) {
        if (memberSearchField.value === 'global') {
          params.global = keyword
        } else if (memberSearchField.value === 'username') {
          params.username = keyword
        } else {
          params.nickname = keyword
        }
      }

      const data = await roleAPI.pageUserByRoleId(selectedRoleId.value, { currentPage: Number(params.currentPage), pageSize: Number(params.pageSize) })
      const page = data.data

      memberPage.value = {
        records: page?.records ?? [],
        current: page?.current ?? memberQuery.value.currentPage,
        size: page?.size ?? memberQuery.value.pageSize,
        total: page?.total ?? 0
      }
    } finally {
      loadingMembers.value = false
    }
  }

  async function loadAvailableUsers(): Promise<void> {
    loadingAvailableUsers.value = true
    try {
      const query: { global?: string; username?: string; nickname?: string; nikename?: string } = {}
      const keyword = addUserSearchKeyword.value.trim()

      if (keyword) {
        if (addUserSearchField.value === 'global') {
          query.global = keyword
        } else if (addUserSearchField.value === 'username') {
          query.username = keyword
        } else {
          query.nickname = keyword
          query.nikename = keyword
        }
      }

      const data = await userAPI.listUser(query)
      availableUsers.value = Array.isArray(data.data) ? data.data : []
    } finally {
      loadingAvailableUsers.value = false
    }
  }

  async function handleCreateRole(): Promise<void> {
    const name = createForm.value.name.trim()
    const type = createForm.value.type

    if (!name) {
      toast.add({
        severity: 'warn',
        summary: t('common.tip'),
        detail: t('role.roleNamePlaceholder'),
        life: 2500
      })
      return
    }

    if (!type) {
      toast.add({
        severity: 'warn',
        summary: t('common.tip'),
        detail: t('role.inheritRolePlaceholder'),
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
        summary: t('common.tip'),
        detail: t('role.createRole') + ' ' + t('common.createSuccess'),
        life: 2500
      })

      createDialogVisible.value = false
      await loadRoles()

      if (newRoleId) {
        memberQuery.value.currentPage = 1
        selectedRoleId.value = newRoleId
      }
    } finally {
      creatingRole.value = false
    }
  }

  function togglePermission(permission: string, checked: boolean): void {
    if (!canEditCurrentRole.value) return

    const next = cloneSet(checkedPermissions.value)
    if (checked) next.add(permission)
    else next.delete(permission)

    checkedPermissions.value = next
  }

  function toggleRow(row: PermissionRow, checked: boolean): void {
    if (!canEditCurrentRole.value) return

    const next = cloneSet(checkedPermissions.value)
    for (const item of row.permissions) {
      if (checked) next.add(item.permission)
      else next.delete(item.permission)
    }
    checkedPermissions.value = next
  }

  async function handleSave(): Promise<void> {
    if (!selectedRoleId.value || !canEditCurrentRole.value) return

    saving.value = true
    try {
      await roleAPI.modifyPermissions(selectedRoleId.value, {
        permissions: [...checkedPermissions.value]
      })

      originalPermissions.value = cloneSet(checkedPermissions.value)

      toast.add({
        severity: 'success',
        summary: t('common.tip'),
        detail: t('role.save') + ' ' + t('common.saveSuccess'),
        life: 2500
      })
    } finally {
      saving.value = false
    }
  }

  function handleMemberSearch(): void {
    memberQuery.value.currentPage = 1
    void loadMembers()
  }

  function handleMemberPageChange(event: PageState): void {
    memberQuery.value.currentPage = Math.floor(event.first / event.rows) + 1
    memberQuery.value.pageSize = event.rows
    void loadMembers()
  }

  function toggleAddUser(userId: string, checked: boolean): void {
    const next = cloneSet(selectedAddUserIds.value)
    if (checked) next.add(userId)
    else next.delete(userId)
    selectedAddUserIds.value = next
  }

  async function handleConfirmAddUsers(): Promise<void> {
    if (!selectedRoleId.value || selectedAddUserIds.value.size === 0) return

    addingUsers.value = true
    try {
      await roleAPI.addUser(selectedRoleId.value, {
        userIds: [...selectedAddUserIds.value]
      })

      toast.add({
        severity: 'success',
        summary: t('common.tip'),
        detail: t('role.addMember') + ' ' + t('common.addSuccess'),
        life: 2500
      })

      addUserDialogVisible.value = false
      selectedAddUserIds.value = new Set()

      await loadMembers()
      await loadAvailableUsers()
    } finally {
      addingUsers.value = false
    }
  }

  async function removeUser(userId: string): Promise<void> {
    if (!selectedRoleId.value) return

    removingUserId.value = userId
    try {
      await roleAPI.removeUser(selectedRoleId.value, {
        userIds: [userId]
      })

      const nextRecords = memberPage.value.records.filter((item) => item.id !== userId)
      const nextTotal = Math.max(memberPage.value.total - 1, 0)

      memberPage.value = {
        ...memberPage.value,
        records: nextRecords,
        total: nextTotal
      }

      if (nextRecords.length === 0 && memberQuery.value.currentPage > 1 && nextTotal > 0) {
        memberQuery.value.currentPage -= 1
      }

      await loadMembers()

      if (addUserDialogVisible.value) {
        await loadAvailableUsers()
      }

      toast.add({
        severity: 'success',
        summary: t('common.tip'),
        detail: t('role.removeMember') + ' ' + t('common.deleteSuccess'),
        life: 2500
      })
    } finally {
      removingUserId.value = ''
    }
  }

  watch(selectedRoleId, (roleId) => {
    if (!roleId) return
    void loadPermissions(roleId)
    void loadMembers()
  })

  watch(activeTab, (value) => {
    if (value === 'member') {
      void loadMembers()
    }
  })

  onMounted(() => {
    void loadRoles()
  })

  return {
    activeTab,
    keyword,
    roles,
    selectedRoleId,
    selectedRole,
    builtinRoles,
    customRoles,
    loadingRoles,
    loadingPermissions,
    loadingMembers,
    loadingAvailableUsers,
    saving,
    creatingRole,
    addingUsers,
    removingUserId,

    permissionRows,
    permissionGroups,
    checkedPermissions,
    originalPermissions,
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
  }
}
