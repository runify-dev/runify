<template>
  <aside class="flex w-[248px] shrink-0 flex-col border-r border-surface-200 bg-surface-50 p-2.5 dark:border-surface-700 dark:bg-surface-900">
    <div class="mb-2">
      <IconField>
        <InputIcon class="pi pi-search text-xs" />
        <InputText v-model="keywordModel" :placeholder="t('role.searchRole')" class="w-full" size="small" />
      </IconField>
    </div>

    <div class="min-h-0 flex-1 overflow-y-auto pr-1">
      <section class="mb-3">
        <div class="mb-1.5 text-xs font-medium tracking-wide text-surface-500">{{ t('role.systemBuiltin') }}</div>

        <div v-if="builtinRoles.length" class="space-y-1">
          <button
            v-for="role in builtinRoles"
            :key="role.id"
            type="button"
            class="flex w-full items-center rounded-lg px-2 py-1.5 text-left transition-colors"
            :class="
              selectedRoleId === role.id
                ? 'bg-primary-50 text-primary-600 ring-1 ring-primary-500 dark:bg-primary-900/30 dark:text-primary-400'
                : 'text-surface-700 hover:bg-surface-100 dark:text-surface-300 dark:hover:bg-surface-800'
            "
            @click="emit('select', role)"
          >
            <div class="min-w-0 flex-1">
              <div class="truncate text-sm font-medium leading-5">
                {{ role.name }}
              </div>
              <div class="mt-0.5 truncate text-xs text-surface-400">
                {{ inheritedRoleLabel(role.type) }}
              </div>
            </div>
          </button>
        </div>

        <div
          v-else
          class="rounded-lg border border-dashed border-surface-200 px-3 py-4 text-xs text-surface-400 dark:border-surface-700"
        >
          {{ t('role.noBuiltin') }}
        </div>
      </section>

      <section>
        <div class="mb-1.5 flex items-center justify-between">
          <span class="text-xs font-medium tracking-wide text-surface-500">{{ t('role.customRole') }}</span>
          <Button
            icon="pi pi-plus"
            text
            rounded
            severity="secondary"
            size="small"
            :aria-label="t('role.addRole')"
            @click="emit('create')"
          />
        </div>

        <div v-if="customRoles.length" class="space-y-1">
          <button
            v-for="role in customRoles"
            :key="role.id"
            type="button"
            class="group flex w-full items-center rounded-lg px-2 py-1.5 text-left transition-colors"
            :class="
              selectedRoleId === role.id
                ? 'bg-primary-50 text-primary-600 ring-1 ring-primary-500 dark:bg-primary-900/30 dark:text-primary-400'
                : 'text-surface-700 hover:bg-surface-100 dark:text-surface-300 dark:hover:bg-surface-800'
            "
            @click="emit('select', role)"
          >
            <div class="min-w-0 flex-1">
              <div class="truncate text-sm font-medium leading-5">
                {{ role.name }}
              </div>
              <div class="mt-0.5 truncate text-xs text-surface-400">
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
              aria-label="more"
              @click.stop
            />
          </button>
        </div>

        <div
          v-else
          class="rounded-lg border border-dashed border-surface-200 px-3 py-5 text-center text-xs text-surface-400 dark:border-surface-700"
        >
          {{ t('role.noCustom') }}
        </div>
      </section>
    </div>
  </aside>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { t } from '@/locales'
import Button from 'primevue/button'
import IconField from 'primevue/iconfield'
import InputIcon from 'primevue/inputicon'
import InputText from 'primevue/inputtext'
import type { RoleItem } from '../types'

const props = defineProps<{
  keyword: string
  builtinRoles: RoleItem[]
  customRoles: RoleItem[]
  selectedRoleId: string
  inheritedRoleLabel: (type?: string | null) => string
}>()

const emit = defineEmits<{
  (e: 'update:keyword', value: string): void
  (e: 'select', role: RoleItem): void
  (e: 'create'): void
}>()

const keywordModel = computed({
  get: () => props.keyword,
  set: (value: string) => emit('update:keyword', value)
})
</script>
