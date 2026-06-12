<template>
  <Dialog
    :visible="visible"
    modal
    :header="t('role.addMember')"
    :style="{ width: '42rem' }"
    :draggable="false"
    @update:visible="emit('update:visible', $event)"
  >
    <div class="mb-3 flex items-center gap-2">
      <Select
        v-model="searchFieldModel"
        :options="memberSearchFieldOptions"
        option-label="label"
        option-value="value"
        class="w-[120px]"
      />
      <InputText
        v-model.trim="searchKeywordModel"
        :placeholder="t('role.searchPlaceholder')"
        class="flex-1"
        @keydown.enter="emit('search')"
      />
      <Button :label="t('common.search')" size="small" @click="emit('search')" />
    </div>

    <div class="max-h-[420px] overflow-auto rounded-lg border border-surface-200 dark:border-surface-700">
      <table class="min-w-full border-collapse text-sm">
        <thead class="sticky top-0 bg-surface-50 dark:bg-surface-800">
          <tr class="text-surface-500">
            <th class="w-[56px] border-b border-surface-200 px-3 py-2 text-center dark:border-surface-700">{{ t('role.selectHeader') }}</th>
            <th class="border-b border-surface-200 px-3 py-2 text-left text-xs font-medium dark:border-surface-700">{{ t('role.nicknameHeader') }}</th>
            <th class="border-b border-surface-200 px-3 py-2 text-left text-xs font-medium dark:border-surface-700">
              {{ t('role.usernameHeader') }}
            </th>
            <th class="border-b border-surface-200 px-3 py-2 text-left text-xs font-medium dark:border-surface-700">
              {{ t('role.emailHeader') }}
            </th>
          </tr>
        </thead>

        <tbody v-if="availableUsers.length">
          <tr
            v-for="user in availableUsers"
            :key="user.id"
            class="border-b border-surface-200 last:border-b-0 dark:border-surface-700"
          >
            <td class="px-3 py-2 text-center">
              <Checkbox
                :model-value="selectedAddUserIds.has(user.id)"
                :binary="true"
                :disabled="memberUserIdSet.has(user.id)"
                @update:model-value="emit('toggle-user', user.id, $event)"
              />
            </td>
            <td class="px-3 py-2 text-surface-700 dark:text-surface-300">{{ user.nickname || '-' }}</td>
            <td class="px-3 py-2 text-surface-700 dark:text-surface-300">{{ user.username || '-' }}</td>
            <td class="px-3 py-2 text-surface-700 dark:text-surface-300">{{ user.email || '-' }}</td>
          </tr>
        </tbody>

        <tbody v-else>
          <tr>
            <td colspan="4" class="px-4 py-10 text-center text-sm text-surface-400">
              {{ loading ? t('role.userLoading') : t('role.noUserData') }}
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <div class="mt-3 text-xs text-surface-500">{{ t('role.selectedCount', { count: selectedAddUserIds.size }) }}</div>

    <template #footer>
      <div class="flex justify-end gap-2">
        <Button :label="t('common.cancel')" text severity="secondary" @click="emit('update:visible', false)" />
        <Button
          :label="t('role.confirmAdd')"
          :loading="submitting"
          :disabled="selectedAddUserIds.size === 0 || !selectedRoleId"
          @click="emit('confirm')"
        />
      </div>
    </template>
  </Dialog>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { t } from '@/locales'
import Button from 'primevue/button'
import Checkbox from 'primevue/checkbox'
import Dialog from 'primevue/dialog'
import InputText from 'primevue/inputtext'
import Select from 'primevue/select'
import type { MemberSearchField, UserItem } from '../types'

const props = defineProps<{
  visible: boolean
  loading: boolean
  submitting: boolean
  selectedRoleId: string
  searchField: MemberSearchField
  searchKeyword: string
  memberSearchFieldOptions: Array<{ label: string; value: MemberSearchField }>
  availableUsers: UserItem[]
  selectedAddUserIds: Set<string>
  memberUserIdSet: Set<string>
}>()

const emit = defineEmits<{
  (e: 'update:visible', value: boolean): void
  (e: 'update:searchField', value: MemberSearchField): void
  (e: 'update:searchKeyword', value: string): void
  (e: 'search'): void
  (e: 'toggle-user', userId: string, checked: boolean): void
  (e: 'confirm'): void
}>()

const searchFieldModel = computed({
  get: () => props.searchField,
  set: (value: MemberSearchField) => emit('update:searchField', value)
})

const searchKeywordModel = computed({
  get: () => props.searchKeyword,
  set: (value: string) => emit('update:searchKeyword', value)
})
</script>
