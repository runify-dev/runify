<template>
  <Dialog
    :visible="visible"
    modal
    :header="t('role.createRole')"
    :style="{ width: '28rem' }"
    :draggable="false"
    @update:visible="emit('update:visible', $event)"
  >
    <div class="space-y-4">
      <div>
        <label class="mb-1.5 block text-sm font-medium text-surface-700 dark:text-surface-300">{{ t('role.roleName') }}</label>
        <InputText
          v-model.trim="nameModel"
          class="w-full"
          maxlength="50"
          :placeholder="t('role.roleNamePlaceholder')"
        />
      </div>

      <div>
        <label class="mb-1.5 block text-sm font-medium text-surface-700 dark:text-surface-300">{{ t('role.inheritRole') }}</label>
        <Select
          v-model="typeModel"
          :options="inheritRoleOptions"
          option-label="label"
          option-value="value"
          :placeholder="t('role.inheritRolePlaceholder')"
          class="w-full"
        />
      </div>
    </div>

    <template #footer>
      <div class="flex justify-end gap-2">
        <Button :label="t('common.cancel')" text severity="secondary" @click="emit('update:visible', false)" />
        <Button :label="t('common.create')" :loading="loading" @click="emit('confirm')" />
      </div>
    </template>
  </Dialog>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { t } from '@/locales'
import Button from 'primevue/button'
import Dialog from 'primevue/dialog'
import InputText from 'primevue/inputtext'
import Select from 'primevue/select'
import type { InheritRoleType } from '../types'

const props = defineProps<{
  visible: boolean
  loading: boolean
  name: string
  type: InheritRoleType | null
  inheritRoleOptions: Array<{ label: string; value: InheritRoleType }>
}>()

const emit = defineEmits<{
  (e: 'update:visible', value: boolean): void
  (e: 'update:name', value: string): void
  (e: 'update:type', value: InheritRoleType | null): void
  (e: 'confirm'): void
}>()

const nameModel = computed({
  get: () => props.name,
  set: (value: string) => emit('update:name', value)
})

const typeModel = computed({
  get: () => props.type,
  set: (value: InheritRoleType | null) => emit('update:type', value)
})
</script>
