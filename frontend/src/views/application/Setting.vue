<template>
  <div class="relative h-full">
    <div
      class="absolute z-10 right-6 top-4 items-center space-x-0 bg-white border border-gray-300 rounded-full p-px inline-flex"
    >
      <button @click="save" class="px-2 py-0.5 text-sm text-gray-600 hover:bg-gray-200/30 rounded-full">
        {{ t('application.save') }}
      </button>
      <button
        @click="debugFn"
        class="px-2 py-0.5 text-sm text-gray-600 hover:bg-gray-200/30 rounded-full"
      >
        {{ t('application.debug') }}
      </button>
      <button
        @click="historyDrawerRef?.open()"
        class="px-2 py-0.5 text-sm text-gray-600 hover:bg-gray-200/30 rounded-full"
      >
        {{ t('application.publish.history') }}
      </button>
      <button
        @click="publishFn"
        class="px-2 py-0.5 text-sm text-primary-600 font-medium hover:bg-gray-200/30 rounded-full"
      >
        {{ t('application.publish.publish') }}
      </button>
    </div>

    <Dialog
      v-model:visible="publishDialog"
      :header="t('application.publish.title')"
      :modal="true"
      :style="{ width: '420px' }"
    >
      <div class="flex flex-col gap-3">
        <p class="text-xs" style="color: var(--p-text-muted-color)">{{ t('application.publish.tip') }}</p>
        <div class="flex flex-col gap-1">
          <label class="text-sm font-semibold">{{ t('application.publish.remark') }}</label>
          <Textarea
            v-model="publishRemark"
            rows="3"
            auto-resize
            fluid
            :placeholder="t('application.publish.remarkPlaceholder')"
          />
        </div>
      </div>
      <template #footer>
        <Button text @click="publishDialog = false">{{ t('common.cancel') }}</Button>
        <Button :loading="publishing" @click="confirmPublish">{{ t('application.publish.confirm') }}</Button>
      </template>
    </Dialog>

    <PublishHistoryDrawer
      ref="historyDrawerRef"
      :fetch-list="fetchVersionList"
      :fetch-version="fetchVersionDetail"
      @rollback="onRollback"
    />

    <DebugConversation
      v-if="debug"
      @close="debug = false"
      :forder-id="folderId"
      :application="application"
    ></DebugConversation>

    <Workflow ref="workflowRef"></Workflow>
  </div>
</template>
<script setup lang="ts">
import { onMounted, ref, computed, inject, provide } from 'vue'
import Workflow from '@/workflow/index.vue'
import ApplicationAPI from '@/api/application'
import { useRoute } from 'vue-router'
import bus from '@/bus'
import { baseWorkflow, WorkflowType } from '@/workflow/common/data'
import DebugConversation from './DebugConversation.vue'
import PublishHistoryDrawer from '@/components/publish-history-drawer/index.vue'
import { t } from '@/locales'
const debug = ref<boolean>(false)
const route = useRoute()
// 注入父组件提供的方法
const getApplication = inject('getApplication') as any
provide('WorkflowType', WorkflowType.APPLICATION)
provide('getDetails', () => application.value)
const workflowRef = ref<InstanceType<typeof Workflow>>()
const application = ref<any>()
const debugFn=()=>{
  workflowRef.value?.validateWorkflow().then(ok=>{
    console.log(ok)
    if(ok.valid){

      debug.value=true
    }
  })
}
const folderId = computed(() => {
  const {
    params: { folderId }
  } = route as any
  return folderId
})
const resourceId = computed(() => {
  const {
    params: { id }
  } = route as any
  return id
})
const save = () => {
  workflowRef.value?.validateWorkflow().then((ok) => {
    if (!ok.valid) return
    ApplicationAPI.edit(resourceId.value, { workflow: workflowRef.value?.getGraphData() }).then(() => {
      bus.emit('message:success', t('application.saveSuccess'))
    })
  })
}

// ── 发布 ──
const historyDrawerRef = ref<InstanceType<typeof PublishHistoryDrawer>>()
const publishDialog = ref<boolean>(false)
const publishRemark = ref<string>('')
const publishing = ref<boolean>(false)

const publishFn = () => {
  workflowRef.value?.validateWorkflow().then((ok) => {
    if (!ok.valid) return
    publishRemark.value = ''
    publishDialog.value = true
  })
}

const confirmPublish = () => {
  ApplicationAPI.publish(
    resourceId.value,
    { workflow: workflowRef.value?.getGraphData(), remark: publishRemark.value },
    publishing
  ).then(() => {
    publishDialog.value = false
    bus.emit('message:success', t('application.publish.success'))
  })
}

const fetchVersionList = (loading?: any) => ApplicationAPI.listVersions(resourceId.value, loading)
const fetchVersionDetail = (versionId: string) => ApplicationAPI.getVersion(resourceId.value, versionId)

// 回滚：把某版本 snapshot 的工作流回填画布(不落库),用户确认后再保存/发布
const onRollback = (workflow: any) => {
  workflowRef.value?.render(workflow && workflow.nodes ? workflow : baseWorkflow)
}

onMounted(() => {
  getApplication().then((app: any) => {
    application.value = app
    workflowRef.value?.render(app.workflow.nodes ? app.workflow : baseWorkflow)
  })
})
</script>
<style lang="scss">
.layout-main {
  padding: 0 !important;
  border: 1px solid var(--surface-border);
  border-radius: 12px;
  overflow: hidden;
}

.layout-content-height {
  height: calc(100dvh - var(--app-content-height-offset) + 3rem - 2px);
}
</style>
