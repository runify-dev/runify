<template>
  <div class="flex flex-col h-full">
    <!-- 顶部操作栏 -->
    <div class="flex items-center justify-between px-6 py-3 border-b" style="border-color: var(--p-content-border-color);">
      <span class="text-lg font-bold">{{ t('knowledge.setting.title') }}</span>
      <Button :label="t('common.save')" icon="pi pi-save" @click="save" :loading="saving"/>
    </div>

    <!-- 表单内容 -->
    <div class="flex-1 overflow-auto px-6 py-6 flex flex-col gap-8">
      <!-- 基本信息 -->
      <div class="flex flex-col gap-4">
        <h3 class="text-sm font-semibold text-color">{{ t('knowledge.setting.basicInfo') }}</h3>
        <div class="flex flex-col gap-1.5">
          <label class="text-sm font-medium text-surface-700">{{ t('knowledge.setting.name') }}</label>
          <InputText v-model="formData.name" type="text" fluid class="!text-sm"/>
        </div>
        <div class="flex flex-col gap-1.5">
          <label class="text-sm font-medium text-surface-700">{{ t('knowledge.setting.desc') }}</label>
          <Textarea v-model="formData.desc" rows="3" fluid class="!text-sm !resize-none"/>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import {computed, onMounted, ref, watch} from 'vue'
import {useRoute} from 'vue-router'
import {TreeCommonAPI} from '@/api/tree'
import knowledgeApi from '@/api/knowledge'
import bus from '@/bus'
import {t} from '@/locales'

const route = useRoute()
const treeCommonAPI = new TreeCommonAPI('knowledge')
const saving = ref(false)

const resourceId = computed(() => (route.params as any).id)

const formData = ref<Record<string, any>>({
  name: '',
  desc: ''
})

// 加载数据
const get = () => {
  treeCommonAPI.getResource(resourceId.value).then(ok => {
    const data = ok.data
    formData.value = {
      name: data.name || '',
      desc: data.desc || ''
    }
  })
}

// 保存
const save = () => {
  saving.value = true
  knowledgeApi.edit(resourceId.value, {
    name: formData.value.name,
    desc: formData.value.desc
  }).then(() => {
    saving.value = false
    bus.emit('message:success', t('knowledge.setting.saveSuccess'))
  }).catch(() => {
    saving.value = false
  })
}

watch(resourceId, () => get())

onMounted(() => {
  get()
})
</script>
