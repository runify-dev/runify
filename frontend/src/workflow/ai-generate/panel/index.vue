<template>
  <!-- 遮罩：生成期间画布透视可见，但吞掉所有编辑交互 -->
  <div v-if="busy" class="ai-generate-mask"></div>

  <!-- 收起态：一条细状态条，最大限度让出画布 -->
  <div v-if="visible && collapsed" class="ai-generate-bar">
    <i class="pi pi-sparkles bar-icon"></i>
    <span class="bar-text">
      <i v-if="status === 'running'" class="pi pi-spin pi-spinner mr-1"></i>
      <i v-else-if="status === 'error'" class="pi pi-times-circle mr-1 text-red-500"></i>
      <i v-else-if="status === 'done'" class="pi pi-check-circle mr-1 text-green-500"></i>
      {{ barText }}
    </span>
    <Button
      v-if="status === 'running'"
      icon="pi pi-stop"
      text
      rounded
      size="small"
      severity="danger"
      :title="t('workflowAgent.action.stop')"
      @click="stop"
    />
    <Button
      v-else-if="status === 'error'"
      icon="pi pi-refresh"
      text
      rounded
      size="small"
      :title="t('workflowAgent.action.retry')"
      @click="retry"
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
            <label class="mb-1 block text-sm font-medium">{{
              t('workflowAgent.modelLabel')
            }}</label>
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
            <label class="mb-1 block text-sm font-medium">{{
              t('workflowAgent.requirementLabel')
            }}</label>
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
        <div ref="logContainerRef" class="panel-chat" @scroll="onChatScroll">
          <div v-if="!activity.root.items.length" class="chat-empty">
            {{ t('workflowAgent.activity.empty') }}
          </div>
          <!-- 活动树：按 position 层级展开/收起 -->
          <AgentActivity :scope="activity.root" />

          <!-- 生成完成的结果摘要：markdown 渲染 -->
          <div v-if="status === 'done' && summary" class="chat-summary">
            <i class="pi pi-check-circle"></i>
            <div class="chat-summary-md"><PreView :modelValue="summary" /></div>
          </div>
          <!-- 出错信息 -->
          <div v-else-if="status === 'error' && errorMsg" class="chat-error">
            <i class="pi pi-times-circle"></i>
            <span>{{ errorMsg }}</span>
          </div>
        </div>

        <!-- 继续沟通：完成后提修改意见 -->
        <div v-if="status === 'done'" class="panel-followup">
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
              :label="t('workflowAgent.action.restart')"
              icon="pi pi-replay"
              severity="secondary"
              outlined
              size="small"
              @click="reset"
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
import { ref, computed, watch, onMounted, onUnmounted, inject } from 'vue'
import Textarea from 'primevue/textarea'
import { t } from '@/locales'
import bus from '@/bus'
import { TreeCommonAPI } from '@/api/tree'
import { ROOT_FOLDER_ID } from '@/constants/common'
import { WorkflowType } from '@/workflow/common/data'
import { profileFor } from '../index'
import { createAgent } from '../create-agent'
import { clearDebugLog } from '../common'
import type { AgentHandle, AgentEvent, Call } from '../type'
import { createActivity } from './index'
import AgentActivity from './components/AgentActivity.vue'
import PreView from '@/editor/preview/index.vue'

const props = defineProps<{
  getLf: () => any
  validateWorkflow?: (options?: any) => Promise<any>
  relayout?: () => void
}>()

const workflowType = inject<string>('WorkflowType') || WorkflowType.APPLICATION

const visible = ref(false)
const collapsed = ref(false)
const requirement = ref('')
const modelId = ref('')
const modelList = ref<Array<any>>([])
const followUp = ref('')
const logContainerRef = ref<HTMLElement>()

/** 面板状态机：idle 表单 → running 生成中 → done/error/stopped 终态 */
type Status = 'idle' | 'running' | 'done' | 'error' | 'stopped'
const status = ref<Status>('idle')
const busy = computed(() => status.value === 'running')
const summary = ref('')
const errorMsg = ref('')

// 活动树：把带 position 的扁平事件流拼成可展开的树（见 activity.ts）
const activity = createActivity()

let agent: AgentHandle | null = null
let lastRequirement = ''

function resetEventState() {
  activity.reset()
  summary.value = ''
  errorMsg.value = ''
}

/** 事件出口：并入活动树 + 顶层 done/error 单独收尾（总结/报错）+ 贴底跟随 */
function onEvent(ev: AgentEvent) {
  activity.pushEvent(ev)
  if (ev.position.length === 0) {
    // 顶层 agent 的交付(data)另用 markdown 展示；子 agent 的交付由其委派工具卡片结果体现
    if (ev.type === 'done' && ev.data != null) {
      summary.value = typeof ev.data === 'string' ? ev.data : JSON.stringify(ev.data)
    }
    if (ev.type === 'error') errorMsg.value = ev.message
  }
  if (stick) scrollToBottom()
}

/** 装配顶层父 agent 并跑一轮；agent 复用同一实例以保留多轮上下文 */
async function runOnce(text: string) {
  if (!agent) {
    const call: Call<AgentEvent> = { onNext: onEvent }
    agent = createAgent(
      profileFor(workflowType).useAgent({
        lf: props.getLf(),
        modelId: modelId.value,
        call,
        workflowType,
        position: [],
        // 父 agent 现在亲手建整张图（主画布 + 所有循环体，经 lf_id 跨画布），轮次远多于只建主画布时——
        // 放宽到 200（逐节点配置仍由 configure_node 子 agent 各自 60 轮承担，不占这里）。
        maxIterations: 200,
        // 整体目标：用户需求作为背景，经 bindings.context 逐层透传给各子配置 agent（见 useAgentConfig / configure_node）
        context: text
      })
    )
  }
  status.value = 'running'
  const result = await agent.run(text)
  if (result.status === 'done') {
    status.value = 'done'
    if (!summary.value) {
      summary.value = typeof result.data === 'string' ? result.data : JSON.stringify(result.data ?? '')
    }
    props.relayout?.()
  } else if (result.status === 'error') {
    status.value = 'error'
    if (!errorMsg.value) errorMsg.value = typeof result.data === 'string' ? result.data : String(result.data ?? '')
  } else {
    status.value = 'stopped'
  }
}

function onStart() {
  stick = true
  resetEventState()
  clearDebugLog() // 每次新生成清空调试日志，避免与上一次混在一起（localStorage: ai-generate-debug-log）
  agent = null
  lastRequirement = requirement.value.trim()
  runOnce(lastRequirement)
  // 生成期间自动收起，把画布让出来
  collapsed.value = true
}

function onSend() {
  const content = followUp.value.trim()
  if (!content) return
  followUp.value = ''
  stick = true
  runOnce(content)
}

function stop() {
  agent?.stop()
  status.value = 'stopped'
}

function retry() {
  // 复用同一 agent 续跑（保留上下文），把上次需求再喂一遍
  stick = true
  runOnce(lastRequirement || requirement.value.trim())
}

function reset() {
  agent?.stop()
  agent = null
  resetEventState()
  status.value = 'idle'
}

/** 收起态状态条文案：最近一条活动（活动树维护） */
const barText = computed(() => {
  if (status.value === 'error') return errorMsg.value || t('workflowAgent.status.running')
  return activity.lastActivity.value || t('workflowAgent.status.running')
})

function open() {
  visible.value = true
  collapsed.value = false
}

function close() {
  if (busy.value) return
  visible.value = false
}

watch(status, (value) => {
  if (value === 'done') {
    bus.emit('message:success', t('workflowAgent.doneToast'))
    // 展开面板，露出总结与继续沟通输入框
    collapsed.value = false
  }
})

// 自动滚底：默认贴底跟随；用户上滚离底则暂停跟随，回到底部再恢复（stick）
let stick = true
let scrollScheduled = false

function scrollToBottom() {
  if (scrollScheduled) return
  scrollScheduled = true
  requestAnimationFrame(() => {
    scrollScheduled = false
    const el = logContainerRef.value
    if (el) el.scrollTop = el.scrollHeight
  })
}

function onChatScroll() {
  const el = logContainerRef.value
  if (!el) return
  stick = el.scrollHeight - el.scrollTop - el.clientHeight < 48
}

onMounted(() => {
  new TreeCommonAPI('model').listResource(ROOT_FOLDER_ID).then((ok) => {
    modelList.value = ok.data ?? []
  })
})

onUnmounted(() => agent?.stop())

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
  // 固定宽度，不随文案变长；超出的状态文字由 bar-text 省略号截断
  width: 20rem;
  max-width: calc(100% - 32px);
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
    flex: 1;
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
  width: 34rem;
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

  // 活动树滚动区：由 AgentActivity 组件按 position 层级渲染可展开子树
  .panel-chat {
    flex: 1;
    min-height: 14rem;
    max-height: 60vh;
    overflow-y: auto;
    display: flex;
    flex-direction: column;
    gap: 8px;

    .chat-empty {
      color: var(--p-text-muted-color);
      font-size: 13px;
      text-align: center;
      padding: 12px 0;
    }
  }

  // 生成完成的结果摘要，钉在对话流末尾
  .chat-summary {
    flex-shrink: 0;
    display: flex;
    align-items: flex-start;
    gap: 8px;
    padding: 8px 10px;
    border-radius: 8px;
    background: color-mix(in srgb, var(--p-green-500) 12%, transparent);
    font-size: 13px;
    line-height: 1.55;
    color: var(--p-text-color);

    i {
      color: var(--p-green-500);
      flex-shrink: 0;
      margin-top: 3px;
    }

    .chat-summary-md {
      flex: 1;
      min-width: 0;

      :deep(.simple-editor-content) {
        p:first-child {
          margin-top: 0;
        }
        p:last-child {
          margin-bottom: 0;
        }
      }
    }
  }

  .chat-error {
    flex-shrink: 0;
    display: flex;
    align-items: flex-start;
    gap: 8px;
    padding: 8px 10px;
    border-radius: 8px;
    background: color-mix(in srgb, var(--p-red-500) 12%, transparent);
    font-size: 13px;
    line-height: 1.55;
    color: var(--p-text-color);

    i {
      color: var(--p-red-500);
      flex-shrink: 0;
      margin-top: 3px;
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
