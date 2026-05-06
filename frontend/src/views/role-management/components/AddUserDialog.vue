<template>
  <Dialog
    :visible="visible"
    modal
    header="添加成员"
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
        placeholder="请输入"
        class="flex-1"
        @keydown.enter="emit('search')"
      />
      <Button label="搜索" size="small" @click="emit('search')" />
    </div>

    <div class="max-h-[420px] overflow-auto rounded-lg border border-surface-200 dark:border-surface-700">
      <table class="min-w-full border-collapse text-sm">
        <thead class="sticky top-0 bg-surface-50 dark:bg-surface-800">
          <tr class="text-surface-500">
            <th class="w-[56px] border-b border-surface-200 px-3 py-2 text-center dark:border-surface-700">选择</th>
            <th class="border-b border-surface-200 px-3 py-2 text-left text-xs font-medium dark:border-surface-700">昵称</th>
            <th class="border-b border-surface-200 px-3 py-2 text-left text-xs font-medium dark:border-surface-700">
              用户名
            </th>
            <th class="border-b border-surface-200 px-3 py-2 text-left text-xs font-medium dark:border-surface-700">
              邮箱
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
              {{ loading ? '用户加载中...' : '暂无用户数据' }}
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <div class="mt-3 text-xs text-surface-500">已选 {{ selectedAddUserIds.size }} 人</div>

    <template #footer>
      <div class="flex justify-end gap-2">
        <Button label="取消" text severity="secondary" @click="emit('update:visible', false)" />
        <Button
          label="确认添加"
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
