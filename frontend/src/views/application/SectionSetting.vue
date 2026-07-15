<template>
  <div class="flex flex-col p-4 gap-4">
    <div class="flex items-center justify-between">
      <div>
        <div class="text-base font-medium">便签设置</div>
        <div class="text-xs text-surface-400 mt-1">
          定义便签子区及其归属：跟用户（跨对话记住个人喜好）、跟对话（随本次任务）、跟应用（全应用共享）。
          自定义子区的「抽取说明」决定 AI 抽什么。
        </div>
      </div>
      <div class="flex gap-2">
        <Button label="添加子区" icon="pi pi-plus" size="small" severity="secondary" @click="addRow" />
        <Button label="保存" icon="pi pi-check" size="small" :loading="saving" @click="save" />
      </div>
    </div>

    <div class="border border-surface-200 rounded-lg overflow-hidden">
      <div class="grid grid-cols-[1fr_1fr_2fr_1.2fr_5rem_3rem] gap-2 px-3 py-2 bg-surface-50 text-xs font-medium text-surface-500">
        <div>标识 (value)</div>
        <div>标题 (label)</div>
        <div>抽取说明 (description)</div>
        <div>归属</div>
        <div class="text-center">启用</div>
        <div></div>
      </div>
      <div
        v-for="(row, i) in rows"
        :key="i"
        class="grid grid-cols-[1fr_1fr_2fr_1.2fr_5rem_3rem] gap-2 px-3 py-2 items-center border-t border-surface-100"
      >
        <InputText v-model="row.sectionKey" placeholder="preference" class="w-full" size="small" />
        <InputText v-model="row.label" placeholder="喜好" class="w-full" size="small" />
        <Textarea v-model="row.description" placeholder="用户个人喜好：称呼/语气/风格倾向…" rows="1" autoResize class="w-full text-sm" />
        <Select v-model="row.scope" :options="scopeOptions" optionLabel="label" optionValue="value" class="w-full" size="small" />
        <div class="flex justify-center">
          <ToggleSwitch v-model="row.enabled" />
        </div>
        <div class="flex justify-center">
          <Button icon="pi pi-trash" text rounded severity="danger" size="small" @click="rows.splice(i, 1)" />
        </div>
      </div>
      <div v-if="rows.length === 0" class="px-3 py-8 text-center text-sm text-surface-400">
        暂无便签子区，点「添加子区」新建
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import ApplicationAPI from '@/api/application'
import bus from '@/bus'

const route = useRoute()
const applicationId = ref<string>((route.params as any).id)

const scopeOptions = [
  { label: '跟用户', value: 'user' },
  { label: '跟对话', value: 'conversation' },
  { label: '跟应用', value: 'application' }
]

interface SectionRow {
  sectionKey: string
  label: string
  description: string
  scope: string
  listStyle: boolean
  enabled: boolean
}

const rows = ref<SectionRow[]>([])
const saving = ref(false)

const addRow = () => {
  rows.value.push({
    sectionKey: '',
    label: '',
    description: '',
    scope: 'conversation',
    listStyle: false,
    enabled: true
  })
}

const load = () => {
  ApplicationAPI.getSections(applicationId.value).then((res: any) => {
    rows.value = (res?.data ?? []).map((s: any) => ({
      sectionKey: s.sectionKey ?? '',
      label: s.label ?? '',
      description: s.description ?? '',
      scope: s.scope ?? 'conversation',
      listStyle: !!s.listStyle,
      enabled: s.enabled !== false
    }))
  })
}

const save = () => {
  const payload = rows.value
    .filter((r) => r.sectionKey && r.sectionKey.trim())
    .map((r, i) => ({ ...r, sectionKey: r.sectionKey.trim(), sortOrder: (i + 1) * 10 }))
  saving.value = true
  ApplicationAPI.saveSections(applicationId.value, payload)
    .then(() => {
      bus.emit('message:success', '便签设置已保存')
      load()
    })
    .finally(() => {
      saving.value = false
    })
}

onMounted(load)
</script>
