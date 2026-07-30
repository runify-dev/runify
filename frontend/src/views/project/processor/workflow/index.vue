<template>
  <div class="relative h-full">
    <!-- 顶部操作栏 -->
    <div
      class="absolute z-10 right-6 top-4 flex items-center gap-1 bg-[var(--p-surface-0)]/80 backdrop-blur-sm border border-[var(--p-content-border-color)] rounded-lg px-1.5 py-1 shadow-sm"
    >
      <Button
        icon="pi pi-save"
        :label="t('common.save')"
        variant="text"
        size="small"
        @click="save"
      />
      <Button
        icon="pi pi-send"
        :label="t('application.publish.publish')"
        variant="text"
        size="small"
        @click="publishFn"
      />
      <Button
        icon="pi pi-history"
        :label="t('application.publish.history')"
        variant="text"
        size="small"
        @click="historyDrawerRef?.open()"
      />
      <div class="w-px h-5 bg-surface-200" />
      <Button
        v-if="processor && processor.isDeploy"
        icon="pi pi-cloud-download"
        :label="t('project.cancelDeploy')"
        variant="text"
        size="small"
        severity="warning"
        @click="unDeploy"
      />
      <Button
        v-if="processor && !processor.isDeploy"
        icon="pi pi-cloud-upload"
        :label="t('project.deploy')"
        variant="text"
        size="small"
        severity="success"
        @click="deploy"
      />
    </div>

    <Dialog
      v-model:visible="publishDialog"
      :header="t('application.publish.publish')"
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

    <Workflow ref="workflowRef" />
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref, provide } from 'vue'
import { t } from '@/locales'
import Workflow from '@/workflow/index.vue'
import PublishHistoryDrawer from '@/components/publish-history-drawer/index.vue'
import processorAPI from '@/api/processor'
import { useRoute } from 'vue-router'
import bus from '@/bus'
import { baseWorkflow, WorkflowType } from '@/workflow/common/data'

const route = useRoute()
const workflowRef = ref<InstanceType<typeof Workflow>>()
const processor = ref<any>()

// ── 发布 ──
const historyDrawerRef = ref<InstanceType<typeof PublishHistoryDrawer>>()
const publishDialog = ref<boolean>(false)
const publishRemark = ref<string>('')
const publishing = ref<boolean>(false)

const publishFn = () => {
  workflowRef.value?.validateWorkflow().then((ok) => {
    if (!ok || !ok.valid) return
    publishRemark.value = ''
    publishDialog.value = true
  })
}

const confirmPublish = () => {
  processorAPI
    .publish(
      route.params.id as string,
      route.params.processorId as string,
      { workflow: workflowRef.value?.getGraphData(), remark: publishRemark.value },
      publishing
    )
    .then(() => {
      publishDialog.value = false
      bus.emit('message:success', t('application.publish.success'))
    })
}

const fetchVersionList = (loading?: any) =>
  processorAPI.listVersions(route.params.id as string, route.params.processorId as string, loading)
const fetchVersionDetail = (versionId: string) =>
  processorAPI.getVersion(route.params.id as string, route.params.processorId as string, versionId)

// 回滚：把某版本 snapshot 的工作流回填画布(不落库),用户确认后再保存/部署
const onRollback = (workflow: any) => {
  workflowRef.value?.render(workflow && workflow.nodes ? workflow : baseWorkflow)
}

provide('getDetails', () => processor.value)
provide('WorkflowType', WorkflowType.PROCESSOR)

const deploy = () => {
  workflowRef.value?.validateWorkflow().then((ok) => {
    if (!ok || !ok.valid) return
    processorAPI
      .deploy(route.params.id as string, route.params.processorId as string)
      .then((res) => {
        processor.value = res.data
        bus.emit('message:success', t('project.deploySuccess'))
      })
  })
}

const unDeploy = () => {
  processorAPI
    .undeploy(route.params.id as string, route.params.processorId as string)
    .then((ok) => {
      processor.value = ok.data
      bus.emit('message:success', t('project.cancelDeploySuccess'))
    })
}

const save = () => {
  workflowRef.value?.validateWorkflow().then((ok) => {
    if (!ok || !ok.valid) return
    processorAPI
      .editProcessor(route.params.id as string, route.params.processorId as string, {
        workflow: workflowRef.value?.getGraphData()
      })
      .then(() => {
        bus.emit('message:success', t('common.saveSuccess'))
      })
  })
}

onMounted(() => {
  processorAPI
    .getProcessor(route.params.id as string, route.params.processorId as string)
    .then((ok) => {
      processor.value = ok.data
      if (ok.data.workflow?.nodes) {
        workflowRef.value?.render(ok.data.workflow)
      } else {
        workflowRef.value?.render(baseWorkflow)
      }
    })
})
</script>
