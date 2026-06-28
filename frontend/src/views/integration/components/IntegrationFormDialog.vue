<template>
  <Dialog v-model:visible="visible" modal :header="t('integration.form.header')" :style="{ width: '36rem' }">
    <div v-loading="loading" class="flex flex-col gap-4">
      <!-- 名称 -->
      <div class="flex flex-col gap-1.5">
        <label class="text-sm font-medium text-surface-700">{{ t('integration.form.name.label') }}</label>
        <InputText
          v-model="formData.name"
          type="text"
          :placeholder="t('integration.form.name.placeholder')"
          fluid
          class="!text-sm"
        />
      </div>

      <!-- 平台类型 -->
      <div class="flex flex-col gap-1.5">
        <label class="text-sm font-medium text-surface-700">{{ t('integration.form.type') }}</label>
        <Select
          v-model="formData.type"
          :options="typeOptions"
          optionLabel="label"
          optionValue="type"
          fluid
          :placeholder="t('integration.form.typePlaceholder')"
          class="!text-sm"
        />
      </div>

      <!-- 绑定应用 -->
      <div class="flex flex-col gap-1.5">
        <label class="text-sm font-medium text-surface-700">{{ t('integration.form.application') }}</label>
        <Select
          v-model="formData.applicationId"
          :options="applicationOptions"
          optionLabel="name"
          optionValue="id"
          filter
          fluid
          :placeholder="t('integration.form.applicationPlaceholder')"
          class="!text-sm"
        />
      </div>

      <!-- 凭证 -->
      <template v-if="fields.length">
        <div v-for="f in fields" :key="f.field" class="flex flex-col gap-1.5">
          <label class="text-sm font-medium text-surface-700">{{ f.label }}</label>
          <Password
            v-if="f.secret"
            v-model="formData.config[f.field]"
            :feedback="false"
            toggleMask
            fluid
            :placeholder="f.placeholder"
            inputClass="!text-sm w-full"
          />
          <InputText
            v-else
            v-model="formData.config[f.field]"
            :placeholder="f.placeholder"
            fluid
            class="!text-sm"
          />
        </div>
      </template>

      <!-- 启用 -->
      <div class="flex items-center justify-between">
        <label class="text-sm font-medium text-surface-700">{{ t('integration.form.enabled') }}</label>
        <ToggleSwitch v-model="formData.enabled"/>
      </div>
    </div>

    <template #footer>
      <div class="flex justify-end gap-2">
        <Button :label="t('common.cancel')" :loading="loading" severity="secondary" variant="outlined"
                @click="close"/>
        <Button :label="t('common.create')" :loading="loading" @click="submit"/>
      </div>
    </template>
  </Dialog>
</template>

<script setup lang="ts">
import {ref, computed} from 'vue'
import {TreeCommonAPI} from '@/api/tree'
import IntegrationAPI, {loadIntegrationTypes, useIntegrationTypes, getTypeMeta} from '@/api/integration'
import type {TreeNode} from 'primevue/treenode'
import bus from '@/bus'
import {ROOT_FOLDER_ID} from "@/constants/common.ts";
import { t } from '@/locales'

const props = defineProps<{ api: TreeCommonAPI }>()
const emit = defineEmits(['create:success'])

const visible = ref(false)
const current = ref<TreeNode>()
const loading = ref<boolean>(false)
const typeOptions = useIntegrationTypes()
const applicationOptions = ref<Array<{id: string; name: string}>>([])

const formData = ref<Record<string, any>>({
  name: '',
  type: '',
  applicationId: '',
  enabled: true,
  config: {}
})

const fields = computed(() => (formData.value.type ? getTypeMeta(formData.value.type)?.fields || [] : []))

const resetForm = () => {
  formData.value = {name: '', type: '', applicationId: '', enabled: true, config: {}}
}

const submit = () => {
  if (!formData.value.name.trim() || !formData.value.type || !formData.value.applicationId) {
    bus.emit('message:error', t('integration.form.required'))
    return
  }
  const payload = {
    name: formData.value.name,
    type: formData.value.type,
    applicationId: formData.value.applicationId,
    enabled: formData.value.enabled,
    config: formData.value.config
  }
  props.api
    .createResource(current.value ? current.value.key : ROOT_FOLDER_ID, payload, loading)
    .then((ok) => {
      emit('create:success', current.value ? current.value.key : undefined, ok.data)
      close()
    })
}

const open = (node?: TreeNode) => {
  current.value = node
  resetForm()
  visible.value = true
  loadIntegrationTypes()
  IntegrationAPI.listAllApplications(ROOT_FOLDER_ID, loading).then((list) => {
    applicationOptions.value = list
  })
}

const close = () => {
  current.value = undefined
  visible.value = false
}

defineExpose({open, close})
</script>
