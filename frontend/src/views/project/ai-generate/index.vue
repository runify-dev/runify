<template>
  <!-- 参照 claude.ai：左侧可折叠会话侧栏 + 右侧对话区 -->
  <div class="workbench layout-content-height">
    <aside class="sidebar" :class="{ collapsed: sidebarCollapsed }">
      <div class="sidebar-inner">
        <button class="new-session-btn" :disabled="busy" @click="onReset">
          <i class="pi pi-plus"></i>
          <span>{{ t('workflowAgent.project.newSession') }}</span>
        </button>
        <div class="session-list">
          <!-- 进行中但尚未落库的新会话临时置顶 -->
          <div v-if="!sessionId && logs.length" class="session-item active">
            <i class="pi pi-comment"></i>
            <span class="session-title">{{ currentTitle }}</span>
          </div>
          <div
            v-for="s in sessions"
            :key="s.id"
            class="session-item"
            :class="{ active: s.id === sessionId }"
            @click="openSession(s.id)"
          >
            <i class="pi pi-comment"></i>
            <span class="session-title">{{ s.title || t('workflowAgent.project.newSession') }}</span>
            <i class="pi pi-trash session-del" :title="t('common.delete')" @click.stop="removeSession(s.id)"></i>
          </div>
        </div>
      </div>
    </aside>

    <div class="chat">
      <div class="chat-header">
        <div class="header-left">
          <Button
            :icon="sidebarCollapsed ? 'pi pi-bars' : 'pi pi-angle-left'"
            variant="text"
            size="small"
            severity="secondary"
            :title="sidebarCollapsed ? t('common.expand') : t('common.collapse')"
            @click="sidebarCollapsed = !sidebarCollapsed"
          />
          <span class="chat-title">
            <i class="pi pi-sparkles mr-2"></i>{{ t('workflowAgent.project.title') }}
            <i v-if="status === 'running'" class="pi pi-spin pi-spinner ml-2 text-primary text-sm"></i>
            <i v-else-if="status === 'awaiting'" class="pi pi-question-circle ml-2 text-amber-500 text-sm"></i>
            <i v-else-if="status === 'error'" class="pi pi-times-circle ml-2 text-red-500 text-sm"></i>
            <i v-else-if="status === 'done'" class="pi pi-check-circle ml-2 text-green-500 text-sm"></i>
          </span>
        </div>
        <Select
          v-model="modelId"
          :options="modelList"
          optionLabel="name"
          optionValue="id"
          :placeholder="t('workflowAgent.modelPlaceholder')"
          size="small"
          class="header-model"
        />
      </div>

    <div ref="feedRef" class="chat-feed" @scroll="onScroll">
      <!-- 恢复提示：进页面检测到未完成会话，让用户选继续或新开 -->
      <div v-if="resumable && !feed.length" class="resume-banner">
        <div class="resume-text">
          <i class="pi pi-history mr-2"></i>
          {{ t('workflowAgent.project.resumeFound') }}「{{ resumable.title || t('workflowAgent.project.newSession') }}」
        </div>
        <div class="resume-actions">
          <Button size="small" :label="t('workflowAgent.action.resume')" @click="openSession(resumable.id)" />
          <Button size="small" text severity="secondary" :label="t('workflowAgent.project.newSession')" @click="resumable = null" />
        </div>
      </div>
      <div v-if="!feed.length" class="feed-empty">
        <i class="pi pi-sparkles"></i>
        <div class="empty-title">{{ t('workflowAgent.project.title') }}</div>
        <div class="empty-sub">{{ t('workflowAgent.project.subtitle') }}</div>
      </div>
      <template v-for="item in feed" :key="item.key">
        <!-- 用户消息 -->
        <div v-if="item.kind === 'user'" class="msg-user">{{ item.text }}</div>

        <!-- AI 叙述 -->
        <div v-else-if="item.kind === 'narration'" class="msg-narration">{{ item.text }}</div>

        <!-- 系统提示 -->
        <div v-else-if="item.kind === 'system'" class="msg-system">{{ item.text }}</div>

        <!-- 错误 -->
        <div v-else-if="item.kind === 'error'" class="msg-error">
          <i class="pi pi-times-circle mr-1.5"></i>{{ item.text }}
        </div>

        <!-- 规划清单（首个 plan 位置，绑定最新状态） -->
        <div v-else-if="item.kind === 'plan'" class="plan-card">
          <div
            v-for="(p, i) in plan"
            :key="i"
            class="plan-item"
            :class="`plan-${p.status}`"
          >
            <i
              :class="p.status === 'done' ? 'pi pi-check-circle' : p.status === 'doing' ? 'pi pi-spin pi-spinner' : 'pi pi-circle'"
            ></i>
            <span>{{ p.text }}</span>
          </div>
        </div>

        <!-- 提问卡片：未回答时高亮 + 选项按钮；已回答则灰显（回答另作 user 气泡出现在下方） -->
        <div
          v-else-if="item.kind === 'ask'"
          class="ask-card"
          :class="{ 'ask-open': isAskOpen(item.callId) }"
        >
          <div class="ask-question">
            <i class="pi pi-question-circle mr-1.5"></i>{{ item.question }}
          </div>
          <div v-if="isAskOpen(item.callId) && item.options.length" class="ask-options">
            <Button
              v-for="(opt, i) in item.options"
              :key="i"
              :label="opt"
              size="small"
              severity="secondary"
              outlined
              @click="answerAsk(opt)"
            />
          </div>
        </div>

        <!-- 内联画布：生成中展开画布，终态收起为结果行 -->
        <div v-else-if="item.kind === 'canvas'" class="canvas-block">
          <template v-if="item.card && (item.card.state === 'queued' || item.card.state === 'running')">
            <div class="canvas-head">
              <span class="canvas-title">
                <i
                  v-if="item.card.state === 'queued'"
                  class="pi pi-clock mr-1.5"
                ></i>
                <i
                  v-else-if="subStatusOf(item.card) === 'paused'"
                  class="pi pi-pause mr-1.5"
                ></i>
                <i v-else class="pi pi-spin pi-spinner mr-1.5 text-primary"></i>
                {{ t('workflowAgent.project.building') }} · {{ item.card.name || item.card.processorId }}
              </span>
              <span class="canvas-progress">{{ cardProgress(item.card) }}</span>
            </div>
            <div class="canvas-body">
              <ProcessorCanvas
                :ref="(el: any) => setCanvasRef(item.card!.id, el)"
                :get-processor="() => item.card?.handle?.processor"
              />
            </div>
          </template>
          <div
            v-else-if="item.card"
            class="result-row"
            :class="`result-${item.card.state}`"
          >
            <i v-if="item.card.state === 'success'" class="pi pi-check-circle"></i>
            <i v-else class="pi pi-times-circle"></i>
            <span class="result-name">{{ item.card.name || item.card.processorId }}</span>
            <span class="result-summary">{{ resultSummary(item.card) }}</span>
          </div>
          <!-- 恢复的会话没有内存卡片（子代理状态已随页面销毁）：结果行可展开，按需重建 mini 画布查看已生成的工作流 -->
          <template v-else>
            <div
              class="result-row restored-head"
              :class="[
                item.canvasState ? `result-${item.canvasState}` : '',
                { 'is-expandable': item.processorId }
              ]"
              @click="toggleRestored(item)"
            >
              <i v-if="item.canvasState === 'success'" class="pi pi-check-circle"></i>
              <i v-else-if="item.canvasState === 'invalid' || item.canvasState === 'error'" class="pi pi-times-circle"></i>
              <i v-else class="pi pi-file"></i>
              <span class="result-name">
                {{ t('workflowAgent.tool.generate_workflow') }}<template v-if="item.name"> · {{ item.name }}</template>
              </span>
              <span v-if="item.summary" class="result-summary">{{ item.summary }}</span>
              <i
                v-if="item.processorId"
                class="pi expand-chevron"
                :class="expandedRestored.has(item.key) ? 'pi-chevron-up' : 'pi-chevron-down'"
              ></i>
            </div>
            <div v-if="expandedRestored.has(item.key)" class="canvas-body restored-canvas">
              <ProcessorCanvas
                :ref="(el: any) => setRestoredCanvasRef(item.key, el)"
                :get-processor="() => restoredProcessors.get(item.processorId)"
              />
            </div>
          </template>
        </div>

        <!-- 其余工具调用：一行摘要 -->
        <div v-else class="msg-tool">
          <span class="tool-status" :class="`tool-status-${item.toolStatus}`">
            <i v-if="item.toolStatus === 'running'" class="pi pi-spin pi-spinner"></i>
            <i v-else-if="item.toolStatus === 'success'" class="pi pi-check-circle"></i>
            <i v-else class="pi pi-times-circle"></i>
          </span>
          <span class="tool-name">{{ toolLabel(item.toolName) }}</span>
          <span class="tool-args">{{ item.text }}</span>
        </div>
      </template>
    </div>

    <div class="chat-input-wrap">
      <div class="composer" :class="{ 'is-disabled': !canInput }">
        <Textarea
          v-model="input"
          :placeholder="inputPlaceholder"
          :disabled="!canInput"
          rows="1"
          autoResize
          class="composer-input"
          @keydown.enter.exact.prevent="onSubmit"
        />
        <div class="composer-bar">
          <template v-if="status === 'running'">
            <Button icon="pi pi-pause" rounded severity="secondary" size="small" :title="t('workflowAgent.action.pause')" @click="onPause" />
            <Button icon="pi pi-stop" rounded severity="danger" size="small" :title="t('workflowAgent.action.stop')" @click="onStop" />
          </template>
          <template v-else-if="status === 'paused'">
            <Button icon="pi pi-play" rounded severity="secondary" size="small" :title="t('workflowAgent.action.resume')" @click="onResume" />
            <Button icon="pi pi-stop" rounded severity="danger" size="small" :title="t('workflowAgent.action.stop')" @click="onStop" />
            <Button icon="pi pi-arrow-up" rounded size="small" :disabled="!input.trim()" :title="t('workflowAgent.action.send')" @click="onSubmit" />
          </template>
          <template v-else-if="status === 'awaiting'">
            <Button icon="pi pi-stop" rounded severity="danger" size="small" :title="t('workflowAgent.action.stop')" @click="onStop" />
            <Button icon="pi pi-arrow-up" rounded size="small" :disabled="!input.trim()" :title="t('workflowAgent.action.send')" @click="onSubmit" />
          </template>
          <template v-else-if="status === 'error'">
            <Button icon="pi pi-stop" rounded severity="danger" size="small" :title="t('workflowAgent.action.stop')" @click="onStop" />
            <Button icon="pi pi-refresh" rounded size="small" :title="t('workflowAgent.action.retry')" @click="retry" />
          </template>
          <Button
            v-else
            icon="pi pi-arrow-up"
            rounded
            size="small"
            :disabled="!input.trim() || (isStart && !modelId)"
            :title="t('workflowAgent.action.send')"
            @click="onSubmit"
          />
        </div>
      </div>
    </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, shallowRef, reactive, triggerRef, computed, watch, nextTick, onMounted, onBeforeUnmount } from 'vue'
import { useRoute } from 'vue-router'
import { cloneDeep } from 'lodash'
import Textarea from 'primevue/textarea'
import { t } from '@/locales'
import bus from '@/bus'
import { TreeCommonAPI } from '@/api/tree'
import processorAPI from '@/api/processor'
import { baseWorkflow } from '@/workflow/common/data'
import { ROOT_FOLDER_ID } from '@/constants/common'
import ProcessorCanvas from './components/ProcessorCanvas.vue'
import { projectAiApi } from './persistence-api'
import { useProjectAgent } from './useProjectAgent'
import {
  generateProcessorWorkflow,
  type SubAgentHandle,
  type SubAgentResult,
  type WorkflowHost
} from './workflow-subagent'

/** 同时在跑的子代理上限：每路都是独立 SSE 流 + 几十轮模型调用，超出的排队 */
const MAX_CONCURRENT = 3

interface CanvasCard {
  id: number
  callId: string
  processorId: string
  name: string
  state: 'queued' | 'running' | 'success' | 'invalid' | 'error'
  handle: SubAgentHandle | null
  result: SubAgentResult | null
}

const route = useRoute()
// shallowRef：卡片句柄内含子 agent 的一组 ref，深层解包会破坏 .value 访问；变更后 triggerRef
const cards = shallowRef<CanvasCard[]>([])
let cardSeq = 0
/** callId → 卡片：把内联画布锚定到触发它的那次 generate_workflow tool_call */
const cardByCallId = new Map<string, CanvasCard>()

const canvasRefs = new Map<number, InstanceType<typeof ProcessorCanvas>>()
function setCanvasRef(id: number, el: any) {
  if (el) canvasRefs.set(id, el)
  else canvasRefs.delete(id)
}
function hostFor(id: number): WorkflowHost {
  const canvas = () => canvasRefs.get(id)
  return {
    getLf: () => canvas()?.getLf(),
    render: (data) => canvas()?.render(data),
    getGraphData: () => canvas()?.getGraphData(),
    autoLayout: () => canvas()?.autoLayout(),
    validateWorkflow: (options) =>
      canvas()?.validateWorkflow(options) ?? Promise.resolve({ valid: true })
  }
}

// ── 恢复态 mini 画布：已完成的生成行（无内存卡片）可展开，按需拉取处理器已保存的工作流渲染 ──
const expandedRestored = reactive(new Set<string>())
const restoredProcessors = reactive(new Map<string, any>())
const restoredCanvasRefs = new Map<string, InstanceType<typeof ProcessorCanvas>>()
function setRestoredCanvasRef(key: string, el: any) {
  if (el) restoredCanvasRefs.set(key, el)
  else restoredCanvasRefs.delete(key)
}
async function toggleRestored(item: any) {
  if (!item.processorId) return
  if (expandedRestored.has(item.key)) {
    expandedRestored.delete(item.key)
    return
  }
  try {
    if (!restoredProcessors.has(item.processorId)) {
      const res = await processorAPI.getProcessor(route.params.id as string, item.processorId)
      restoredProcessors.set(item.processorId, res.data)
    }
  } catch (e: any) {
    bus.emit('message:error', e?.message ?? String(e))
    return
  }
  expandedRestored.add(item.key)
  await nextTick() // 等 ProcessorCanvas 挂载
  const canvas = restoredCanvasRefs.get(item.key)
  const processor = restoredProcessors.get(item.processorId)
  canvas?.render(processor?.workflow?.nodes ? processor.workflow : cloneDeep(baseWorkflow))
  canvas?.autoLayout()
}

// ── 并发信号量：模型一轮想并行发几个 generate_workflow 都行，超出上限的排队 ──
let runningCount = 0
const slotWaiters: Array<() => void> = []
async function acquireSlot() {
  while (runningCount >= MAX_CONCURRENT) {
    await new Promise<void>((resolve) => slotWaiters.push(resolve))
  }
  runningCount++
}
function releaseSlot() {
  runningCount--
  slotWaiters.shift()?.()
}

async function generateWorkflow(args: {
  processorId: string
  requirement: string
  callId?: string
}): Promise<SubAgentResult> {
  const { processorId, requirement: subRequirement, callId } = args
  // 把处理器名/结果摘要/终态写回 generate_workflow 的日志对象——日志随 session.timeline 落库，
  // 刷新恢复后卡片虽已销毁，仍能从日志显示「给谁生成的、结果如何」
  const enrichLog = (patch: Record<string, any>) => {
    if (!callId) return
    const log = logs.value.find(
      (l: any) => l.callId === callId && l.toolName === 'generate_workflow'
    )
    if (log) Object.assign(log, patch)
  }
  const card: CanvasCard = {
    id: ++cardSeq,
    callId: callId ?? `c${cardSeq}`,
    processorId,
    name: '',
    state: 'queued',
    handle: null,
    result: null
  }
  cards.value = [...cards.value, card]
  cardByCallId.set(card.callId, card)
  const projectId = route.params.id as string
  // 台账 taskId：本次 generate_workflow 在 project_ai_task 里的行（best-effort，可能为空）
  let taskId: string | null = null
  await nextTick() // 等卡片画布挂载
  await acquireSlot()
  try {
    if (status.value === 'stopped') {
      card.state = 'error'
      card.result = { valid: false, summary: '', error: '已停止' }
      triggerRef(cards)
      return card.result
    }
    card.state = 'running'
    triggerRef(cards)
    // 台账：登记子任务（best-effort，失败不阻塞生成）
    if (sessionId.value) {
      try {
        const taskRes = await projectAiApi.createTask(projectId, sessionId.value, {
          processorId,
          requirement: subRequirement,
          status: 'running'
        })
        taskId = taskRes.data?.id ?? null
      } catch {
        /* 持久化失败忽略 */
      }
    }
    const result = await generateProcessorWorkflow({
      projectId,
      processorId,
      requirement: subRequirement,
      modelId: modelId.value,
      host: hostFor(card.id),
      onHandle: (handle) => {
        card.handle = handle
        card.name = handle.processorName
        triggerRef(cards)
        enrichLog({ processorName: handle.processorName })
      }
    })
    card.state = result.valid ? 'success' : result.error ? 'error' : 'invalid'
    card.result = result
    triggerRef(cards)
    enrichLog({
      processorId,
      processorName: card.name,
      resultSummary: card.state === 'success' ? result.summary || '' : result.error || '',
      canvasState: card.state
    })
    return result
  } catch (e: any) {
    card.state = 'error'
    card.result = { valid: false, summary: '', error: e?.message ?? String(e) }
    triggerRef(cards)
    enrichLog({ processorId, resultSummary: card.result.error || '', canvasState: 'error' })
    throw e
  } finally {
    releaseSlot()
    // 画布已交付/失败，卸载实例释放（结果行不再需要画布）
    canvasRefs.delete(card.id)
    // 台账终态（best-effort）：状态 + 结果落库，供台账查询与失败增量重来
    if (taskId) {
      projectAiApi
        .updateTask(projectId, taskId, {
          status: card.state === 'queued' || card.state === 'running' ? 'error' : card.state,
          result: card.result ?? undefined
        })
        .catch(() => {})
    }
  }
}

const {
  status,
  logs,
  logsVersion,
  plan,
  pendingAsk,
  busy,
  start,
  send,
  pause,
  resume,
  stop,
  retry,
  reset,
  sessionId,
  resumeSession,
  listSessions,
  deleteSession
} = useProjectAgent({
  getProjectId: () => route.params.id as string,
  generateWorkflow
})

// 侧栏会话列表 + 未完成会话恢复提示
const sessions = ref<Array<any>>([])
const resumable = ref<any | null>(null)
const UNFINISHED = ['running', 'awaiting', 'paused']

async function loadSessions() {
  try {
    const res = await listSessions()
    sessions.value = res.data ?? []
  } catch {
    sessions.value = []
  }
}

// 打开/恢复一个会话：重建循环运行时，回填模型选择器，清掉已随页面销毁的子代理卡片
async function openSession(id: string) {
  resumable.value = null
  cards.value = []
  cardByCallId.clear()
  canvasRefs.clear()
  clearRestored()
  try {
    const { modelId: restoredModel } = await resumeSession(id)
    if (restoredModel) modelId.value = restoredModel
    await loadSessions()
  } catch (e: any) {
    bus.emit('message:error', e?.message ?? String(e))
  }
}

async function removeSession(id: string) {
  try {
    await deleteSession(id)
    if (id === sessionId.value) clearSession()
    if (resumable.value?.id === id) resumable.value = null
    await loadSessions()
  } catch (e: any) {
    bus.emit('message:error', e?.message ?? String(e))
  }
}

const input = ref('')
const modelId = ref('')
const modelList = ref<Array<any>>([])
const feedRef = ref<HTMLElement>()
const sidebarCollapsed = ref(false)

// 当前会话标题（首条用户需求截断）；多会话历史列表待 M3 后端接入
const currentTitle = computed(() => {
  const text = logs.value.find((l) => l.kind === 'user')?.text?.trim()
  if (!text) return t('workflowAgent.project.newSession')
  return text.length > 24 ? text.slice(0, 24) + '…' : text
})

// done/paused 续聊、awaiting 回答提问——均走底部输入框 send
const canSend = computed(
  () => status.value === 'done' || status.value === 'paused' || status.value === 'awaiting'
)
// idle 起手、续聊/回答、stopped 后开新会话时可输入；running/error 时锁输入框
const canInput = computed(
  () => status.value === 'idle' || status.value === 'stopped' || canSend.value
)
// idle/stopped 都是「起手」态：需要模型才能发
const isStart = computed(() => status.value === 'idle' || status.value === 'stopped')
const inputPlaceholder = computed(() => {
  if (isStart.value) return t('workflowAgent.project.requirementPlaceholder')
  if (status.value === 'awaiting') return t('workflowAgent.project.answerPlaceholder')
  if (canSend.value) return t('workflowAgent.followUpPlaceholder')
  return t('workflowAgent.project.inputPlaceholder')
})

/** 该提问是否仍待回答（决定提问卡片是否高亮 + 展示选项按钮） */
function isAskOpen(callId: string): boolean {
  return pendingAsk.value?.callId === callId
}
/** 点选项即作为回答提交（等价于在输入框输入该选项后发送） */
function answerAsk(answer: string) {
  send(answer)
}

/**
 * 对话流 = 外层 agent 的 logs 顺序渲染，加工两处：
 * - plan 工具日志只在首次出现处渲染成规划卡片（绑定最新 plan 状态），后续 plan 日志跳过
 * - generate_workflow 工具日志渲染成内联画布（按 callId 锚定对应卡片）
 */
const feed = computed(() => {
  void logsVersion.value // 依赖版本号，流式追加时重算
  void cards.value // 卡片状态变化时重算
  const items: Array<any> = []
  let planShown = false
  for (const log of logs.value) {
    if (log.kind === 'tool' && log.toolName === 'plan') {
      if (planShown) continue
      planShown = true
      items.push({ key: `plan`, kind: 'plan' })
      continue
    }
    if (log.kind === 'tool' && log.toolName === 'generate_workflow') {
      const callId = (log as any).callId as string
      items.push({
        key: `canvas-${log.id}`,
        kind: 'canvas',
        card: cardByCallId.get(callId) ?? null,
        // 日志上持久化的信息：卡片销毁（刷新恢复）后据此还原「给谁生成、结果如何」
        name: (log as any).processorName ?? '',
        summary: (log as any).resultSummary ?? '',
        canvasState: (log as any).canvasState ?? '',
        processorId: (log as any).processorId ?? ''
      })
      continue
    }
    if (log.kind === 'tool' && log.toolName === 'ask_user') {
      items.push({
        key: `ask-${log.id}`,
        kind: 'ask',
        callId: (log as any).callId,
        question: (log as any).question ?? log.text,
        options: (log as any).options ?? []
      })
      continue
    }
    if (log.kind === 'tool') {
      items.push({
        key: `log-${log.id}`,
        kind: 'tool',
        toolName: log.toolName,
        toolStatus: log.status,
        text: log.text
      })
      continue
    }
    items.push({ key: `log-${log.id}`, kind: log.kind, text: log.text })
  }
  return items
})

function subStatusOf(card: CanvasCard) {
  return card.handle?.agent.status.value
}

function cardProgress(card: CanvasCard): string {
  const items = card.handle?.agent.plan.value ?? []
  if (!items.length) return ''
  return `[${items.filter((i) => i.status === 'done').length}/${items.length}]`
}

function resultSummary(card: CanvasCard): string {
  if (card.state === 'success') return card.result?.summary || t('workflowAgent.project.succeeded')
  if (card.state === 'invalid') return t('workflowAgent.project.invalid')
  return card.result?.error || t('workflowAgent.project.failed')
}

function clearSession() {
  reset()
  cards.value = []
  cardByCallId.clear()
  canvasRefs.clear()
  clearRestored()
}

// 清掉恢复态 mini 画布的缓存与展开状态（切换/新建会话时）
function clearRestored() {
  expandedRestored.clear()
  restoredProcessors.clear()
  restoredCanvasRefs.clear()
}

// 底部输入框统一提交：idle/stopped 起手即 start（stopped 先清掉已中断的会话），done/paused 即续聊
function onSubmit() {
  const content = input.value.trim()
  if (!content) return
  if (isStart.value) {
    if (!modelId.value) return
    // stopped 的会话已中断、可能存在未应答 tool_calls，不能续聊——输入即开启新会话
    if (status.value === 'stopped') clearSession()
    input.value = ''
    start(content, modelId.value)
  } else if (canSend.value) {
    input.value = ''
    send(content)
  }
}

function onReset() {
  if (busy.value) return
  clearSession()
  resumable.value = null
  input.value = ''
}

// 暂停/继续/停止级联到全部在途子代理（外层状态翻转只在工具批次间生效）
function activeAgents() {
  return cards.value
    .map((card) => card.handle?.agent)
    .filter((agent) => agent && (agent.status.value === 'running' || agent.status.value === 'paused'))
}
function onPause() {
  activeAgents().forEach((agent) => agent!.pause())
  pause()
}
function onResume() {
  activeAgents().forEach((agent) => agent!.resume())
  resume()
}
function onStop() {
  activeAgents().forEach((agent) => agent!.stop())
  stop()
}

const toolLabelKeys = [
  'plan', 'ask_user', 'clear_workflow', 'get_node_schema', 'add_node', 'update_node', 'add_edge',
  'delete_edge', 'delete_node', 'get_workflow', 'get_node_detail', 'get_models',
  'get_knowledge_bases', 'get_database_pools', 'get_database_tables', 'get_database_columns',
  'edit_node_text', 'validate_workflow', 'locate_node', 'update_blueprint', 'list_processors',
  'create_processor', 'update_processor', 'generate_workflow', 'deploy_processor',
  'undeploy_processor', 'execute_processor', 'delete_processor', 'finish'
]
function toolLabel(name?: string) {
  if (!name) return ''
  return toolLabelKeys.includes(name) ? t(`workflowAgent.tool.${name}`) : name
}

watch(status, (value) => {
  if (value === 'done') bus.emit('message:success', t('workflowAgent.doneToast'))
  // 会话落定/新建后刷新侧栏列表与标题
  if (['done', 'error', 'stopped', 'awaiting', 'paused'].includes(value)) loadSessions()
})
// 首次落库后（sessionId 从无到有）刷新侧栏，让新会话进入列表
watch(sessionId, (id) => {
  if (id) loadSessions()
})

// autoscroll 仅在贴底时生效：用户上翻查看历史时不打扰
let stickToBottom = true
function onScroll() {
  const el = feedRef.value
  if (!el) return
  stickToBottom = el.scrollHeight - el.scrollTop - el.clientHeight < 60
}
let scrollScheduled = false
function scheduleScroll() {
  if (!stickToBottom || scrollScheduled) return
  scrollScheduled = true
  requestAnimationFrame(() => {
    scrollScheduled = false
    const el = feedRef.value
    if (el) el.scrollTop = el.scrollHeight
  })
}
watch(logsVersion, scheduleScroll)

onMounted(async () => {
  new TreeCommonAPI('model').listResource(ROOT_FOLDER_ID).then((ok) => {
    modelList.value = ok.data ?? []
  })
  await loadSessions()
  // 进页面检测未完成会话（最近一个 running/awaiting/paused），提示继续或新开
  resumable.value = sessions.value.find((s) => UNFINISHED.includes(s.status)) ?? null
})

onBeforeUnmount(() => {
  // 离开页面终止生成：已创建的处理器与已保存的工作流不受影响
  onStop()
})
</script>

<style lang="scss" scoped>
.workbench {
  display: flex;
  min-height: 0;
  overflow: hidden;
}

.sidebar {
  width: 15rem;
  flex-shrink: 0;
  border-right: 1px solid var(--p-content-border-color);
  overflow: hidden;
  transition: width 0.18s ease;

  &.collapsed {
    width: 0;
    border-right: none;
  }

  // 折叠时内容不 reflow：内层固定宽，由外层 overflow 裁剪
  .sidebar-inner {
    width: 15rem;
    height: 100%;
    min-height: 0;
    display: flex;
    flex-direction: column;
    gap: 8px;
    padding: 10px;
  }

  .new-session-btn {
    flex-shrink: 0;
    display: flex;
    align-items: center;
    gap: 8px;
    width: 100%;
    padding: 9px 12px;
    border: 1px solid var(--p-content-border-color);
    border-radius: 8px;
    background: var(--p-surface-0);
    font-size: 13px;
    font-weight: 500;
    color: var(--p-text-color);
    cursor: pointer;
    transition: background 0.15s;

    i {
      color: var(--p-primary-color);
      font-size: 14px;
    }
    &:hover:not(:disabled) {
      background: color-mix(in srgb, var(--p-text-color) 5%, transparent);
    }
    &:disabled {
      opacity: 0.5;
      cursor: not-allowed;
    }
  }

  .session-list {
    flex: 1;
    min-height: 0;
    overflow-y: auto;
    display: flex;
    flex-direction: column;
    gap: 4px;
  }

  .session-item {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 8px 10px;
    border-radius: 8px;
    font-size: 13px;
    cursor: pointer;
    color: var(--p-text-color);

    i {
      font-size: 13px;
      color: var(--p-text-muted-color);
      flex-shrink: 0;
    }
    .session-title {
      flex: 1;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }

    .session-del {
      opacity: 0;
      font-size: 12px;
      color: var(--p-text-muted-color);
      &:hover { color: var(--p-red-500); }
    }

    &:hover {
      background: color-mix(in srgb, var(--p-text-color) 6%, transparent);
      .session-del { opacity: 1; }
    }
    &.active {
      background: color-mix(in srgb, var(--p-primary-color) 12%, transparent);
    }
  }
}

.resume-banner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 10px 14px;
  border: 1px solid color-mix(in srgb, var(--p-primary-color) 40%, var(--p-content-border-color));
  border-radius: 12px;
  background: color-mix(in srgb, var(--p-primary-color) 7%, transparent);
  font-size: 13px;

  .resume-text {
    display: flex;
    align-items: center;
    color: var(--p-text-color);
    > i { color: var(--p-primary-color); }
  }
  .resume-actions {
    display: flex;
    gap: 6px;
    flex-shrink: 0;
  }
}

.header-left {
  display: flex;
  align-items: center;
  gap: 4px;
  min-width: 0;
}

.header-model {
  min-width: 10rem;
}

.chat {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.chat-header {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 16px;
  border-bottom: 1px solid var(--p-content-border-color);

  .chat-title {
    display: flex;
    align-items: center;
    font-size: 14px;
    font-weight: 600;

    > .pi-sparkles {
      color: var(--p-primary-color);
    }
  }
}

.chat-feed {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.feed-empty {
  flex: 1;
  min-height: 14rem;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  text-align: center;
  color: var(--p-text-muted-color);

  > i {
    font-size: 28px;
    color: var(--p-primary-color);
  }
  .empty-title {
    font-size: 15px;
    font-weight: 600;
    color: var(--p-text-color);
  }
  .empty-sub {
    font-size: 13px;
    max-width: 22rem;
    line-height: 1.6;
  }
}

.msg-user {
  align-self: flex-end;
  max-width: 80%;
  padding: 8px 12px;
  border-radius: 12px;
  background: color-mix(in srgb, var(--p-primary-color) 12%, transparent);
  color: var(--p-text-color);
  font-size: 13px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
}

.msg-narration {
  font-size: 13px;
  line-height: 1.7;
  color: var(--p-text-color);
  white-space: pre-wrap;
  word-break: break-word;
}

.msg-system {
  font-size: 12px;
  color: var(--p-text-muted-color);
}

.msg-error {
  font-size: 13px;
  color: var(--p-red-500);
}

.plan-card {
  display: flex;
  flex-direction: column;
  gap: 5px;
  padding: 10px 12px;
  border: 1px solid var(--p-content-border-color);
  border-radius: 12px;
  background: var(--p-surface-0);
  font-size: 13px;

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
    i { color: var(--p-green-500); }
  }
  .plan-doing i { color: var(--p-primary-color); }
  .plan-pending i { color: var(--p-text-muted-color); }
}

.ask-card {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 12px 14px;
  border: 1px solid var(--p-content-border-color);
  border-radius: 12px;
  background: var(--p-surface-0);

  // 待回答：琥珀高亮，区别于普通信息卡
  &.ask-open {
    border-color: color-mix(in srgb, var(--p-amber-500) 55%, var(--p-content-border-color));
    background: color-mix(in srgb, var(--p-amber-500) 7%, transparent);
  }

  .ask-question {
    display: flex;
    align-items: baseline;
    font-size: 13px;
    line-height: 1.6;
    color: var(--p-text-color);

    > i {
      color: var(--p-amber-500);
      flex-shrink: 0;
    }
  }

  .ask-options {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
  }
}

.msg-tool {
  display: flex;
  align-items: baseline;
  gap: 6px;
  font-size: 12px;
  color: var(--p-text-muted-color);

  .tool-status-running { color: var(--p-primary-color); }
  .tool-status-success { color: var(--p-green-500); }
  .tool-status-error { color: var(--p-red-500); }

  .tool-name {
    font-weight: 500;
    flex-shrink: 0;
    color: var(--p-text-color);
  }
  .tool-args {
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}

.canvas-block {
  .canvas-head {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 8px 12px;
    border: 1px solid var(--p-content-border-color);
    border-bottom: none;
    border-radius: 12px 12px 0 0;
    background: color-mix(in srgb, var(--p-primary-color) 6%, transparent);
    font-size: 12px;
    font-weight: 500;

    .canvas-progress { color: var(--p-text-muted-color); }
  }

  .canvas-body {
    height: 22rem;
    border: 1px solid var(--p-content-border-color);
    border-radius: 0 0 12px 12px;
    overflow: hidden;
  }
}

.result-row {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  border: 1px solid var(--p-content-border-color);
  border-radius: 12px;
  font-size: 13px;

  > i { flex-shrink: 0; }

  &.result-success {
    border-color: color-mix(in srgb, var(--p-green-500) 45%, var(--p-content-border-color));
    > i { color: var(--p-green-500); }
  }
  &.result-invalid,
  &.result-error {
    border-color: color-mix(in srgb, var(--p-red-500) 45%, var(--p-content-border-color));
    > i { color: var(--p-red-500); }
  }

  .result-name { font-weight: 500; flex-shrink: 0; }
  .result-summary {
    color: var(--p-text-muted-color);
    font-size: 12px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    min-width: 0;
  }
}

// 恢复态：可展开的生成结果行 + 按需重建的 mini 画布
.restored-head {
  &.is-expandable {
    cursor: pointer;
    &:hover {
      background: color-mix(in srgb, var(--p-text-color) 4%, transparent);
    }
  }
  .expand-chevron {
    margin-left: auto;
    font-size: 12px;
    color: var(--p-text-muted-color);
    flex-shrink: 0;
  }
}

.restored-canvas {
  border-radius: 12px;
  margin-top: 8px;
}

.chat-input-wrap {
  flex-shrink: 0;
  padding: 8px 16px 16px;
}

.composer {
  display: flex;
  align-items: flex-end;
  gap: 8px;
  border: 1px solid var(--p-content-border-color);
  border-radius: 14px;
  background: var(--p-surface-0);
  padding: 5px 6px 5px 14px;
  transition: border-color 0.15s, box-shadow 0.15s;

  &:focus-within {
    border-color: var(--p-primary-color);
    box-shadow: 0 0 0 3px color-mix(in srgb, var(--p-primary-color) 14%, transparent);
  }

  &.is-disabled {
    opacity: 0.7;
  }

  // 去掉 PrimeVue Textarea 自带边框/底色，融进容器
  :deep(.composer-input) {
    flex: 1;
    min-width: 0;
    border: none;
    outline: none;
    background: transparent;
    box-shadow: none;
    padding: 6px 0;
    font-size: 14px;
    line-height: 1.5;
    resize: none;
    max-height: 12rem;
    color: var(--p-text-color);

    &:hover,
    &:focus,
    &:enabled:hover,
    &:enabled:focus {
      border: none;
      outline: none;
      box-shadow: none;
    }
  }
}

// 按钮与输入框同一行、贴右下，整体保持单行高度
.composer-bar {
  display: flex;
  gap: 6px;
  flex-shrink: 0;
  padding-bottom: 1px;
}
</style>
