<template>
  <!-- 遮罩：生成期间画布透视可见，但吞掉所有编辑交互 -->
  <div v-if="busy" class="ai-generate-mask"></div>

  <!-- 收起态：一条细状态条，最大限度让出画布 -->
  <div v-if="visible && collapsed" class="ai-generate-bar">
    <i class="pi pi-sparkles bar-icon"></i>
    <span class="bar-text">
      <i v-if="status === 'running'" class="pi pi-spin pi-spinner mr-1"></i>
      <i v-else-if="status === 'paused'" class="pi pi-pause mr-1"></i>
      <i v-else-if="status === 'error'" class="pi pi-times-circle mr-1 text-red-500"></i>
      <i v-else-if="status === 'done'" class="pi pi-check-circle mr-1 text-green-500"></i>
      {{ barText }}
    </span>
    <template v-if="status === 'running'">
      <Button icon="pi pi-pause" text rounded size="small" :title="t('workflowAgent.action.pause')" @click="pause" />
    </template>
    <template v-else-if="status === 'paused'">
      <Button icon="pi pi-play" text rounded size="small" :title="t('workflowAgent.action.resume')" @click="resume" />
    </template>
    <template v-else-if="status === 'error'">
      <Button icon="pi pi-refresh" text rounded size="small" :title="t('workflowAgent.action.retry')" @click="retry" />
    </template>
    <Button
      v-if="busy || status === 'error'"
      icon="pi pi-stop"
      text
      rounded
      size="small"
      severity="danger"
      :title="t('workflowAgent.action.stop')"
      @click="stop"
    />
    <Button
      icon="pi pi-chevron-down"
      text
      rounded
      size="small"
      severity="secondary"
      :title="t('workflowAgent.action.expand')"
      @click="collapsed = false"
    />
    <Button
      v-if="!busy"
      icon="pi pi-times"
      text
      rounded
      size="small"
      severity="secondary"
      @click="close"
    />
  </div>

  <!-- 展开态：完整面板 -->
  <div v-if="visible && !collapsed" class="ai-generate-panel">
    <div class="panel-header">
      <span class="panel-title">
        <i class="pi pi-sparkles mr-2"></i>{{ t('workflowAgent.title') }}
      </span>
      <span class="flex items-center">
        <Button
          icon="pi pi-chevron-up"
          text
          rounded
          size="small"
          severity="secondary"
          :title="t('workflowAgent.action.collapse')"
          @click="collapsed = true"
        />
        <Button
          icon="pi pi-times"
          text
          rounded
          size="small"
          severity="secondary"
          :disabled="busy"
          @click="close"
        />
      </span>
    </div>

    <div class="panel-body">
      <!-- 输入表单 -->
      <template v-if="status === 'idle'">
        <div class="flex flex-col gap-3">
          <div>
            <label class="mb-1 block text-sm font-medium">{{ t('workflowAgent.modelLabel') }}</label>
            <Select
              v-model="modelId"
              :options="modelList"
              optionLabel="name"
              optionValue="id"
              :placeholder="t('workflowAgent.modelPlaceholder')"
              class="w-full"
            />
          </div>
          <div>
            <label class="mb-1 block text-sm font-medium">{{ t('workflowAgent.requirementLabel') }}</label>
            <Textarea
              v-model="requirement"
              :placeholder="t('workflowAgent.requirementPlaceholder')"
              rows="4"
              class="w-full"
              autoResize
            />
          </div>
          <Button
            :label="t('workflowAgent.action.generate')"
            icon="pi pi-sparkles"
            :disabled="!modelId || !requirement.trim()"
            @click="onStart"
          />
        </div>
      </template>

      <!-- 运行日志 -->
      <template v-else>
        <div v-if="plan.length" class="panel-plan">
          <div v-for="(item, index) in plan" :key="index" class="plan-item" :class="`plan-${item.status}`">
            <i
              :class="
                item.status === 'done'
                  ? 'pi pi-check-circle'
                  : item.status === 'doing'
                    ? 'pi pi-spin pi-spinner'
                    : 'pi pi-circle'
              "
            ></i>
            <span>{{ item.text }}</span>
          </div>
        </div>
        <div ref="logContainerRef" class="panel-logs">
          <div v-for="log in logs" :key="log.id" class="log-item" :class="`log-${log.kind}`">
            <template v-if="log.kind === 'tool'">
              <span class="log-status" :class="`log-status-${log.status}`">
                <i v-if="log.status === 'running'" class="pi pi-spin pi-spinner"></i>
                <i v-else-if="log.status === 'success'" class="pi pi-check-circle"></i>
                <i v-else class="pi pi-times-circle"></i>
              </span>
              <span class="log-tool-name">{{ toolLabel(log.toolName) }}</span>
              <span class="log-tool-args">{{ log.text }}</span>
            </template>
            <template v-else>{{ log.text }}</template>
          </div>
        </div>

        <!-- 继续沟通：完成后提修改意见 / 暂停中插话 -->
        <div v-if="status === 'done' || status === 'paused'" class="panel-followup">
          <Textarea
            v-model="followUp"
            :placeholder="t('workflowAgent.followUpPlaceholder')"
            rows="2"
            class="flex-1"
            autoResize
            @keydown.enter.exact.prevent="onSend"
          />
          <Button
            icon="pi pi-send"
            :disabled="!followUp.trim()"
            :title="t('workflowAgent.action.send')"
            @click="onSend"
          />
        </div>

        <div class="panel-controls">
          <template v-if="status === 'running'">
            <Button
              :label="t('workflowAgent.action.pause')"
              icon="pi pi-pause"
              severity="secondary"
              size="small"
              @click="pause"
            />
            <Button
              :label="t('workflowAgent.action.stop')"
              icon="pi pi-stop"
              severity="danger"
              outlined
              size="small"
              @click="stop"
            />
          </template>
          <template v-else-if="status === 'paused'">
            <Button
              :label="t('workflowAgent.action.resume')"
              icon="pi pi-play"
              size="small"
              @click="resume"
            />
            <Button
              :label="t('workflowAgent.action.stop')"
              icon="pi pi-stop"
              severity="danger"
              outlined
              size="small"
              @click="stop"
            />
          </template>
          <template v-else-if="status === 'error'">
            <Button
              :label="t('workflowAgent.action.retry')"
              icon="pi pi-refresh"
              size="small"
              @click="retry"
            />
            <Button
              :label="t('workflowAgent.action.stop')"
              icon="pi pi-stop"
              severity="danger"
              outlined
              size="small"
              @click="stop"
            />
          </template>
          <template v-else>
            <Button
              :label="t('workflowAgent.action.restart')"
              icon="pi pi-replay"
              severity="secondary"
              size="small"
              @click="reset"
            />
          </template>
        </div>
      </template>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, inject } from 'vue'
import Textarea from 'primevue/textarea'
import { t } from '@/locales'
import bus from '@/bus'
import { TreeCommonAPI } from '@/api/tree'
import { ROOT_FOLDER_ID } from '@/constants/common'
import { WorkflowType } from '@/workflow/common/data'
import { getProfile } from './profiles'
import { useWorkflowAgent } from './useWorkflowAgent'
import type { WorkflowAgentContext } from './types'

const props = defineProps<{
  getLf: () => any
  validateWorkflow: WorkflowAgentContext['validateWorkflow']
  relayout: () => void
}>()

const profile = getProfile(inject<string>('WorkflowType') || WorkflowType.APPLICATION)
// 画布宿主详情（处理器/应用实体）：profile 钩子做联动持久化时用（如处理器保存 HTTP 入参）
const getDetails = inject<() => any>('getDetails', () => undefined)

const visible = ref(false)
const collapsed = ref(false)
const requirement = ref('')
const modelId = ref('')
const modelList = ref<Array<any>>([])
const logContainerRef = ref<HTMLElement>()

const { status, logs, logsVersion, plan, busy, start, send, pause, resume, stop, retry, reset } =
  useWorkflowAgent(
    { getLf: props.getLf, profile, getDetails, validateWorkflow: props.validateWorkflow },
    props.relayout
  )

const followUp = ref('')

function onSend() {
  const content = followUp.value.trim()
  if (!content) return
  followUp.value = ''
  send(content)
}


/** 收起态状态条文案：计划进度 + 最新一条日志 */
const barText = computed(() => {
  if (status.value === 'paused') return t('workflowAgent.status.paused')
  const progress = plan.value.length
    ? `[${plan.value.filter((i) => i.status === 'done').length}/${plan.value.length}] `
    : ''
  const last = logs.value[logs.value.length - 1]
  if (!last) return progress + t('workflowAgent.status.running')
  if (last.kind === 'tool') return progress + toolLabel(last.toolName)
  const text = last.text.trim()
  return progress + (text.length > 40 ? '…' + text.slice(-40) : text || t('workflowAgent.status.running'))
})

const toolLabelMap: Record<string, string> = {
  plan: t('workflowAgent.tool.plan'),
  clear_workflow: t('workflowAgent.tool.clear_workflow'),
  get_node_schema: t('workflowAgent.tool.get_node_schema'),
  add_node: t('workflowAgent.tool.add_node'),
  update_node: t('workflowAgent.tool.update_node'),
  add_edge: t('workflowAgent.tool.add_edge'),
  delete_edge: t('workflowAgent.tool.delete_edge'),
  delete_node: t('workflowAgent.tool.delete_node'),
  get_workflow: t('workflowAgent.tool.get_workflow'),
  get_node_detail: t('workflowAgent.tool.get_node_detail'),
  get_models: t('workflowAgent.tool.get_models'),
  get_knowledge_bases: t('workflowAgent.tool.get_knowledge_bases'),
  get_database_pools: t('workflowAgent.tool.get_database_pools'),
  get_database_tables: t('workflowAgent.tool.get_database_tables'),
  get_database_columns: t('workflowAgent.tool.get_database_columns'),
  validate_workflow: t('workflowAgent.tool.validate_workflow'),
  locate_node: t('workflowAgent.tool.locate_node'),
  finish: t('workflowAgent.tool.finish')
}

function toolLabel(name?: string) {
  return (name && toolLabelMap[name]) || name || ''
}

function open() {
  visible.value = true
  collapsed.value = false
}

function close() {
  if (busy.value) return
  visible.value = false
}

function onStart() {
  start(requirement.value.trim(), modelId.value)
  // 生成期间自动收起，把画布让出来
  collapsed.value = true
}

watch(status, (value) => {
  if (value === 'done') {
    bus.emit('message:success', t('workflowAgent.doneToast'))
    // 展开面板，露出总结与继续沟通输入框
    collapsed.value = false
  }
})

// 日志自动滚底：浅监听版本号 + rAF 节流（流式期间每帧至多滚一次，避免逐 chunk 重排）
let scrollScheduled = false
watch(logsVersion, () => {
  if (scrollScheduled) return
  scrollScheduled = true
  requestAnimationFrame(() => {
    scrollScheduled = false
    const el = logContainerRef.value
    if (el) el.scrollTop = el.scrollHeight
  })
})

onMounted(() => {
  new TreeCommonAPI('model').listResource(ROOT_FOLDER_ID).then((ok) => {
    modelList.value = ok.data ?? []
  })
})

defineExpose({ open })
</script>

<style lang="scss" scoped>
.ai-generate-mask {
  position: absolute;
  inset: 0;
  z-index: 20;
  background: color-mix(in srgb, var(--p-content-background) 25%, transparent);
  pointer-events: auto;
}

.ai-generate-bar {
  position: absolute;
  top: 12px;
  left: 50%;
  transform: translateX(-50%);
  z-index: 30;
  display: flex;
  align-items: center;
  gap: 4px;
  max-width: min(560px, calc(100% - 32px));
  padding: 4px 10px;
  background: var(--p-content-background);
  border: 1px solid var(--p-content-border-color);
  border-radius: 999px;
  box-shadow: var(--p-shadow-2);

  .bar-icon {
    color: var(--p-primary-color);
    flex-shrink: 0;
  }

  .bar-text {
    display: flex;
    align-items: center;
    font-size: 13px;
    color: var(--p-text-color);
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
    min-width: 0;
  }
}

.ai-generate-panel {
  position: absolute;
  top: 12px;
  left: 50%;
  transform: translateX(-50%);
  z-index: 30;
  width: 26rem;
  max-width: calc(100% - 32px);
  // 任务/日志再多也不超出画布：整体限高，内部各区滚动
  max-height: calc(100% - 24px);
  display: flex;
  flex-direction: column;
  background: var(--p-content-background);
  border: 1px solid var(--p-content-border-color);
  border-radius: 8px;
  box-shadow: var(--p-shadow-2);

  .panel-header {
    flex-shrink: 0;
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 8px 8px 8px 12px;
    border-bottom: 1px solid var(--p-content-border-color);

    .panel-title {
      font-size: 14px;
      font-weight: 600;
    }
  }

  .panel-body {
    padding: 12px;
    display: flex;
    flex-direction: column;
    gap: 10px;
    flex: 1;
    min-height: 0;
    overflow-y: auto; // idle 表单态整体滚动兜底
  }

  .panel-plan {
    display: flex;
    flex-direction: column;
    gap: 4px;
    padding: 8px;
    border: 1px solid var(--p-content-border-color);
    border-radius: 6px;
    font-size: 13px;
    flex-shrink: 0;
    max-height: 9rem;
    overflow-y: auto;

    .plan-item {
      display: flex;
      align-items: baseline;
      gap: 6px;
      line-height: 1.5;

      i {
        font-size: 12px;
        flex-shrink: 0;
      }
    }

    .plan-done {
      color: var(--p-text-muted-color);
      text-decoration: line-through;

      i {
        color: var(--p-green-500);
      }
    }

    .plan-doing i {
      color: var(--p-primary-color);
    }

    .plan-pending i {
      color: var(--p-text-muted-color);
    }
  }

  .panel-logs {
    flex: 1;
    min-height: 5rem;
    max-height: 16rem;
    overflow-y: auto;
    display: flex;
    flex-direction: column;
    gap: 6px;
    font-size: 13px;

    .log-item {
      line-height: 1.5;
      word-break: break-word;
      white-space: pre-wrap;
    }

    .log-narration {
      color: var(--p-text-color);
    }

    .log-system {
      color: var(--p-text-muted-color);
      font-size: 12px;
    }

    .log-error {
      color: var(--p-red-500);
    }

    .log-user {
      align-self: flex-end;
      max-width: 90%;
      padding: 4px 10px;
      border-radius: 8px;
      background: color-mix(in srgb, var(--p-primary-color) 12%, transparent);
      color: var(--p-text-color);
    }

    .log-tool {
      display: flex;
      align-items: baseline;
      gap: 6px;
      white-space: nowrap;

      .log-status-running {
        color: var(--p-primary-color);
      }
      .log-status-success {
        color: var(--p-green-500);
      }
      .log-status-error {
        color: var(--p-red-500);
      }

      .log-tool-name {
        font-weight: 500;
        flex-shrink: 0;
      }

      .log-tool-args {
        color: var(--p-text-muted-color);
        font-size: 12px;
        overflow: hidden;
        text-overflow: ellipsis;
      }
    }
  }

  .panel-followup {
    display: flex;
    align-items: flex-end;
    gap: 8px;
    flex-shrink: 0;
  }

  .panel-controls {
    display: flex;
    gap: 8px;
    flex-shrink: 0;
  }
}
</style>
