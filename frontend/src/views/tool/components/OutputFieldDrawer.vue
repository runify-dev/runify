<template>
  <Drawer
    v-model:visible="drawer"
    header="返回字段"
    position="right"
    :pt="{ root: { style: { '--drawer-width-desktop': '460px' }, class: 'responsive-drawer' } }"
  >
    <div class="flex flex-col gap-4">
      <div class="flex flex-col gap-1">
        <label class="text-sm font-semibold">字段名 (field)</label>
        <InputText v-model="local.field" fluid />
      </div>
      <div class="flex flex-col gap-1">
        <label class="text-sm font-semibold">显示名 (label)</label>
        <InputText v-model="local.label" fluid />
      </div>
      <div class="flex flex-col gap-1">
        <label class="text-sm font-semibold">类型 (type)</label>
        <Select v-model="local.type" :options="typeOptions" option-label="label" option-value="value" fluid />
      </div>
      <div class="flex flex-col gap-1">
        <label class="text-sm font-semibold">描述 (description)</label>
        <Textarea v-model="local.description" rows="2" auto-resize fluid />
      </div>
    </div>
    <template #footer>
      <Button text @click="close">{{ t('common.cancel') }}</Button>
      <Button @click="submit">{{ edit ? t('common.save') : t('common.add') }}</Button>
    </template>
  </Drawer>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { t } from '@/locales'
import bus from '@/bus'

const props = defineProps<{ addParams: (data: any, index?: number) => boolean }>()

const typeOptions = [
  { label: 'string', value: 'string' },
  { label: 'number', value: 'number' },
  { label: 'boolean', value: 'boolean' },
  { label: 'object', value: 'object' },
  { label: 'array', value: 'array' }
]

const drawer = ref(false)
const edit = ref(false)
const currentIndex = ref<number>()
const local = reactive({ field: '', label: '', type: 'string', description: '' })

const close = () => {
  drawer.value = false
  edit.value = false
  currentIndex.value = undefined
}

const open = (data?: any, index?: number) => {
  Object.assign(local, { field: '', label: '', type: 'string', description: '' })
  if (data) {
    Object.assign(local, {
      field: data.field || '',
      label: data.label || '',
      type: data.type || 'string',
      description: data.description || ''
    })
  }
  edit.value = !!data
  currentIndex.value = index
  drawer.value = true
}

const submit = () => {
  if (!local.field) {
    bus.emit('message:error', '请填写字段名')
    return
  }
  const ok = props.addParams({ ...local }, currentIndex.value)
  if (ok) close()
}

defineExpose({ open, close })
</script>

<style lang="scss">
.responsive-drawer {
  width: 90% !important;
}
@media (min-width: 1024px) {
  .responsive-drawer {
    width: var(--drawer-width-desktop) !important;
  }
}
</style>
