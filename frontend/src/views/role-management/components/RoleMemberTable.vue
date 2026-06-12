<template>
  <div class="flex min-h-0 flex-1 flex-col overflow-hidden rounded-lg border border-surface-200 bg-surface-0 dark:border-surface-700 dark:bg-surface-900">
    <div class="flex items-center justify-between gap-3 border-b border-surface-200 px-3 py-2 dark:border-surface-700">
      <Button :label="t('role.addMember')" size="small" :disabled="!selectedRoleId" @click="emit('add')" />

      <div class="flex items-center gap-2">
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
          class="w-[220px]"
          @keydown.enter="emit('search')"
        />
        <Button
          icon="pi pi-search"
          text
          rounded
          size="small"
          :aria-label="t('common.search')"
          @click="emit('search')"
        />
      </div>
    </div>

    <div class="min-h-0 flex-1 overflow-auto">
      <table class="min-w-full border-collapse text-sm">
        <thead class="sticky top-0 z-10 bg-surface-50 dark:bg-surface-800">
          <tr class="text-surface-500">
            <th class="border-b border-surface-200 px-3 py-2 text-left text-xs font-medium dark:border-surface-700">{{ t('role.nameHeader') }}</th>
            <th class="border-b border-surface-200 px-3 py-2 text-left text-xs font-medium dark:border-surface-700">
              {{ t('role.usernameHeader') }}
            </th>
            <th
              class="w-[96px] border-b border-surface-200 px-3 py-2 text-center text-xs font-medium dark:border-surface-700"
            >
              {{ t('role.operationHeader') }}
            </th>
          </tr>
        </thead>

        <tbody v-if="memberPage.records.length">
          <tr
            v-for="user in memberPage.records"
            :key="user.id"
            class="border-b border-surface-200 last:border-b-0 dark:border-surface-700"
          >
            <td class="px-3 py-2 text-sm text-surface-700 dark:text-surface-300">
              {{ user.nickname || '-' }}
            </td>
            <td class="px-3 py-2 text-sm text-surface-700 dark:text-surface-300">
              {{ user.username || '-' }}
            </td>
            <td class="px-3 py-2 text-center">
              <Button
                icon="pi pi-user-minus"
                text
                rounded
                size="small"
                severity="secondary"
                :aria-label="t('role.removeMember')"
                :loading="removingUserId === user.id"
                @click="emit('remove', user.id)"
              />
            </td>
          </tr>
        </tbody>

        <tbody v-else>
          <tr>
            <td colspan="3" class="px-6 py-10 text-center text-sm text-surface-400">
              {{ loadingMembers ? t('role.memberLoading') : t('role.noMemberData') }}
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <div class="flex items-center justify-end gap-3 border-t border-surface-200 px-3 py-2 text-sm dark:border-surface-700">
      <span class="text-surface-500">{{ t('role.totalItems', { total: memberPage.total }) }}</span>

      <Paginator
        :rows="memberQuery.pageSize"
        :total-records="memberPage.total"
        :first="(memberQuery.currentPage - 1) * memberQuery.pageSize"
        :rows-per-page-options="[10, 20, 50]"
        template="PrevPageLink PageLinks NextPageLink RowsPerPageDropdown"
        @page="emit('page-change', $event)"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { t } from '@/locales'
import Button from 'primevue/button'
import InputText from 'primevue/inputtext'
import Paginator, { type PageState } from 'primevue/paginator'
import Select from 'primevue/select'
import type { MemberSearchField, PageResult, UserItem } from '../types'

const props = defineProps<{
  selectedRoleId: string
  memberPage: PageResult<UserItem>
  memberQuery: { currentPage: number; pageSize: number }
  loadingMembers: boolean
  memberSearchField: MemberSearchField
  memberSearchKeyword: string
  memberSearchFieldOptions: Array<{ label: string; value: MemberSearchField }>
  removingUserId: string
}>()

const emit = defineEmits<{
  (e: 'update:memberSearchField', value: MemberSearchField): void
  (e: 'update:memberSearchKeyword', value: string): void
  (e: 'search'): void
  (e: 'add'): void
  (e: 'remove', userId: string): void
  (e: 'page-change', event: PageState): void
}>()

const searchFieldModel = computed({
  get: () => props.memberSearchField,
  set: (value: MemberSearchField) => emit('update:memberSearchField', value)
})

const searchKeywordModel = computed({
  get: () => props.memberSearchKeyword,
  set: (value: string) => emit('update:memberSearchKeyword', value)
})
</script>
