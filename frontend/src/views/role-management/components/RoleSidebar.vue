<template>
  <aside class="flex w-[248px] shrink-0 flex-col border-r border-slate-200 bg-slate-50/40 p-3">
    <div class="mb-3">
      <IconField>
        <InputIcon class="pi pi-search text-xs" />
        <InputText v-model="keywordModel" placeholder="搜索角色" class="w-full" size="small" />
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
              selectedRoleId === role.id
                ? 'bg-[#ecfdf5] text-[#10b981] ring-1 ring-[#10b981]'
                : 'text-slate-700 hover:bg-slate-100'
            "
            @click="emit('select', role)"
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
            @click="emit('create')"
          />
        </div>

        <div v-if="customRoles.length" class="space-y-1.5">
          <button
            v-for="role in customRoles"
            :key="role.id"
            type="button"
            class="group flex w-full items-center rounded-lg px-2.5 py-2 text-left transition-colors"
            :class="
              selectedRoleId === role.id
                ? 'bg-[#ecfdf5] text-[#10b981] ring-1 ring-[#10b981]'
                : 'text-slate-700 hover:bg-slate-100'
            "
            @click="emit('select', role)"
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
</template>

<script setup lang="ts">
import { computed } from 'vue'
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
