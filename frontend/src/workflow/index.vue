<template>
  <!-- 模板 ref 而非固定 DOM id：同页可挂多个画布实例（如项目级 AI 生成页） -->
  <div ref="wrapperRef" class="layout-content-height w-full relative overflow-hidden">
    <div ref="containerRef" style="width: 100%; height: 100%"></div>
    <CanvasToolbar
      class="workflow-toolbar"
      :zoom-percent="zoomPercent"
      :mode="interactionMode"
      :fullscreen="isFullscreen"
      :show-ai-generate="showAiGenerate"
      @zoom-in="onZoomIn"
      @zoom-out="onZoomOut"
      @fit-view="onFitView"
      @auto-layout="onAutoLayout"
      @set-mode="onSetMode"
      @toggle-fullscreen="toggleFullscreen"
      @ai-generate="aiGeneratePanelRef?.open()"
    />
    <AiGeneratePanel
      v-if="showAiGenerate"
      ref="aiGeneratePanelRef"
      :get-lf="() => lf"
      :validate-workflow="validateWorkflow"
      :relayout="onAutoLayout"
    />
  </div>
  <ReNameDialog ref="reNameDialogRef"></ReNameDialog>
  <AddNodeDialog ref="addNodeDialogRef"></AddNodeDialog>
  <TeleportContainer v-if="flowId" :flow-id="flowId" />
</template>
<script setup lang="ts">
import ReNameDialog from '@/workflow/common/rename-dialog/index.vue'
import AddNodeDialog from '@/workflow/common/add-node-dialog/index.vue'
import CanvasToolbar from '@/workflow/common/CanvasToolbar.vue'
import AiGeneratePanel from '@/workflow/ai-generate/panel/index.vue'
import LogicFlow from '@logicflow/core'
import '@logicflow/core/dist/index.css'
import '@logicflow/extension/lib/style/index.css'
import { layoutCanvas } from '@/workflow/common/auto-layout'

import RunEdge from './common/edge'
import { onMounted, onBeforeUnmount, ref, computed, provide, inject } from 'vue'
import type { ValidationResult } from './common/type'
import { WorkflowType } from './common/data'
import bus from '@/bus'

const reNameDialogRef = ref<InstanceType<typeof ReNameDialog>>()
const addNodeDialogRef = ref<InstanceType<typeof AddNodeDialog>>()
const nodes: any = import.meta.glob('./nodes/*/index.ts', { eager: true })
import { getTeleport } from '@/workflow/common/teleport'
import { WorkflowAPI } from './common/common'

// ── 节点校验器（自动从各节点 validator.ts 收集） ──
type ValidatorFn = (
  nodeData: Record<string, any>,
  validators?: Map<string, ValidatorFn>,
  workflowType?: string
) => ValidationResult

const validatorModules: any = import.meta.glob('./nodes/*/validator.ts', { eager: true })
const nodeValidators = new Map<string, ValidatorFn>()
for (const path in validatorModules) {
  // ./nodes/ai-chat-node/validator.ts → ai-chat-node
  const type = path.split('/')[2]
  const mod = validatorModules[path]
  if (mod?.validate) {
    nodeValidators.set(type, mod.validate)
  }
}

const workflowType = inject<string>('WorkflowType') || WorkflowType.APPLICATION

// ── AI 生成工作流（应用画布 + 处理器画布）；
//    宿主页面自带 agent 时（如项目级 AI 生成页）注入 hideAiGenerate 隐藏内置面板 ──
const aiGeneratePanelRef = ref<InstanceType<typeof AiGeneratePanel>>()
const hideAiGenerate = inject<boolean>('hideAiGenerate', false)
const showAiGenerate = computed(
  () =>
    !hideAiGenerate &&
    (workflowType === WorkflowType.APPLICATION || workflowType === WorkflowType.PROCESSOR)
)

// ── 在整棵节点树里按 id 定位节点（含循环体子画布，逐层递归）──
//    返回命中节点数据 + 其外层循环 id 链（[最外层…最内层]，空数组=就在主画布）。
//    校验器把最深层失败子节点的 id 冒泡为 failedNodeId，但它可能深埋在循环体里，
//    只查主画布顶层会漏 → 名称取不到、也钻不进去。
function locateNode(
  targetId: string
): { node: any; loopIds: string[] } | null {
  function walk(nodes: any[] | undefined, trail: string[]): { node: any; loopIds: string[] } | null {
    for (const n of nodes ?? []) {
      if (n.id === targetId) return { node: n, loopIds: trail }
      const children = n.properties?.nodeData?.children?.nodes as any[] | undefined
      if (children?.length) {
        const found = walk(children, [...trail, n.id])
        if (found) return found
      }
    }
    return null
  }
  return walk(lf.value?.graphModel?.nodes, [])
}

// ── 校验失败时弹出错误提示（取首条错误信息，带上节点名，深埋循环体也能取到）──
function emitValidationError(failedNodeId: string, errors?: Record<string, string>) {
  const located = locateNode(failedNodeId)
  const name = located?.node?.properties?.name
  const keys = errors ? Object.keys(errors) : []
  const message = keys.length ? errors![keys[0]] : '节点配置校验未通过'
  bus.emit('message:error', name ? `「${name}」${message}` : message)
}

// ── 校验整个工作流（短路）；silent 供 AI 生成用：不弹提示、不打开设置抽屉；
//    skipNodeIds 供 AI 生成用：开始节点配置属用户职责，跳过后单独校验其余节点 ──
async function validateWorkflow(
  options?: { silent?: boolean; skipNodeIds?: string[] }
): Promise<{ valid: boolean; nodeId?: string; errors?: Record<string, string> }> {
  const silent = options?.silent === true
  const skipNodeIds = options?.skipNodeIds ?? []
  if (!lf.value) return { valid: true }
  const { nodes } = lf.value.getGraphData()
  for (const node of nodes) {
    if (skipNodeIds.includes(node.id)) continue
    const validateFn = nodeValidators.get(node.type)
    if (!validateFn) continue
    try {
      const result = validateFn(node.properties?.nodeData ?? {}, nodeValidators, workflowType)
      if (!result.valid) {
        const failedNodeId = result.failedNodeId ?? node.id
        if (silent) {
          return { valid: false, nodeId: failedNodeId, errors: result.errors }
        }
        emitValidationError(failedNodeId, result.errors)
        // 按真实节点树定位失败节点：有外层循环链就逐层展开钻进去，否则直接开主画布节点
        const loopIds = locateNode(failedNodeId)?.loopIds ?? []
        if (loopIds.length) {
          await expandAndOpen([...loopIds, failedNodeId])
        } else {
          selectAndOpenNode(failedNodeId)
        }
        return { valid: false, nodeId: failedNodeId, errors: result.errors }
      }
    } catch {
      if (silent) {
        return { valid: false, nodeId: node.id, errors: { node: '节点配置校验异常' } }
      }
      emitValidationError(node.id)
      selectAndOpenNode(node.id)
      return { valid: false, nodeId: node.id }
    }
  }
  return { valid: true }
}

// 展开循环体并逐层钻取到失败节点。
// descentPath = [最外层循环id, …, 最内层循环id, 失败节点id]，只有最外层循环在主画布上。
// 把「进入后还要继续打开的 id 序列」挂到最外层循环的 _pendingOpenPath 上，
// 由 loop-node 渲染完自身循环体后自我递归下钻（详见 loop-node/index.vue onExpandBody）。
async function expandAndOpen(descentPath: string[]) {
  if (!lf.value || descentPath.length < 2) return
  const [outerLoopId, ...rest] = descentPath
  const outerLoop = lf.value.graphModel.nodes.find((n: any) => n.id === outerLoopId)
  if (!outerLoop) return
  outerLoop.properties._pendingOpenPath = rest
  lf.value.graphModel.eventCenter.emit('runify:node:expand-body', outerLoopId)
}

function selectAndOpenNode(nodeId: string) {
  if (!lf.value) return
  lf.value.selectElementById(nodeId)
  lf.value.graphModel.eventCenter.emit('runify:node:open-settings', nodeId)
}

const TeleportContainer = getTeleport()
const lf = ref()
const flowId = ref('')
const wrapperRef = ref<HTMLElement>()
const containerRef = ref<HTMLElement>()
provide('getMainLf', () => lf.value)
provide('canvasType', 'main')
// 主画布容器：loop-node 展开面板等需要挂载到本实例的容器（多实例下不能 querySelector 全局找）
provide('mainCanvasContainer', wrapperRef)

// ── 缩放 ──
const zoomPercent = ref(100)

function onZoomIn() {
  lf.value?.zoom(true)
  zoomPercent.value = Math.round(lf.value?.getTransform().SCALE_X * 100)
}
function onZoomOut() {
  lf.value?.zoom(false)
  zoomPercent.value = Math.round(lf.value?.getTransform().SCALE_X * 100)
}
function onFitView() {
  lf.value?.fitView(40, 40)
  zoomPercent.value = Math.round(lf.value?.getTransform().SCALE_X * 100)
}
function onAutoLayout() {
  if (!lf.value) return
  layoutCanvas(lf.value)
  zoomPercent.value = Math.round(lf.value.getTransform().SCALE_X * 100)
}

// ── 模式切换 ──
const interactionMode = ref<'drag' | 'select'>('drag')

function applyMode(mode: 'drag' | 'select') {
  if (!lf.value) return
  lf.value.updateEditConfig(
    mode === 'select'
      ? { stopMoveGraph: true, adjustNodePosition: false }
      : { stopMoveGraph: false, adjustNodePosition: true }
  )
}
function onSetMode(mode: 'drag' | 'select') {
  interactionMode.value = mode
  applyMode(mode)
}

// ── 全屏 ──
const isFullscreen = ref(false)
let toolbarOriginalParent: HTMLElement | null = null
let movedToolbar: HTMLElement | null = null

function toggleFullscreen() {
  const el = wrapperRef.value
  if (!el) return
  isFullscreen.value = !isFullscreen.value
  if (isFullscreen.value) {
    Object.assign(el.style, {
      position: 'fixed',
      top: '0',
      left: '0',
      width: `${window.innerWidth}px`,
      height: `${window.innerHeight}px`,
      zIndex: '999',
    })
    // 工具栏移到 body，避免被 canvas 合成层遮挡（移动端 Safari）
    const toolbar = el.querySelector('.canvas-toolbar') as HTMLElement
    if (toolbar) {
      toolbarOriginalParent = toolbar.parentElement
      movedToolbar = toolbar
      toolbar.style.position = 'fixed'
      toolbar.style.zIndex = '1000'
      toolbar.style.bottom = '16px'
      toolbar.style.right = '16px'
      document.body.appendChild(toolbar)
    }
  } else {
    // 清除内联样式，恢复到 CSS 类定义的样式
    el.style.position = ''
    el.style.top = ''
    el.style.left = ''
    el.style.width = ''
    el.style.height = ''
    el.style.zIndex = ''
    // 工具栏移回原位（用实例记录的元素，多画布下 body 里可能有别家的工具栏）
    if (movedToolbar && toolbarOriginalParent) {
      movedToolbar.style.position = ''
      movedToolbar.style.zIndex = ''
      movedToolbar.style.bottom = ''
      movedToolbar.style.right = ''
      toolbarOriginalParent.appendChild(movedToolbar)
      toolbarOriginalParent = null
      movedToolbar = null
    }
  }
}

// ── 空格键 ──
const spaceHeld = ref(false)
provide('canvasInteractionMode', interactionMode)
provide('canvasSpaceHeld', spaceHeld)
function onKeyDown(e: KeyboardEvent) {
  if (e.code !== 'Space' || e.repeat || spaceHeld.value) return
  const tag = (e.target as HTMLElement)?.tagName
  if (tag === 'INPUT' || tag === 'TEXTAREA' || (e.target as HTMLElement)?.isContentEditable) return
  e.preventDefault()
  spaceHeld.value = true
  applyMode('select')
}
function onKeyUp(e: KeyboardEvent) {
  if (e.code !== 'Space' || !spaceHeld.value) return
  e.preventDefault()
  spaceHeld.value = false
  applyMode(interactionMode.value)
}

// ── 框选（vanilla DOM，兼容全屏）──
let selBox: HTMLDivElement | null = null
let selStartX = 0
let selStartY = 0

function isOnCanvas(e: MouseEvent) {
  const c = wrapperRef.value
  return c && c.contains(e.target as Node)
}

function effectiveMode() {
  return spaceHeld.value ? 'select' : interactionMode.value
}

function onMouseDown(e: MouseEvent) {
  if (effectiveMode() !== 'select' || e.button !== 0) return
  if (!isOnCanvas(e)) return
  if ((e.target as HTMLElement).closest('.sub-canvas-container')) return
  if ((e.target as HTMLElement).closest('[data-node-id]')) return

  selStartX = e.clientX
  selStartY = e.clientY

  selBox = document.createElement('div')
  Object.assign(selBox.style, {
    position: 'fixed',
    left: e.clientX + 'px',
    top: e.clientY + 'px',
    width: '0px',
    height: '0px',
    background: 'rgba(51, 112, 255, 0.1)',
    border: '1px solid rgba(51, 112, 255, 0.6)',
    pointerEvents: 'none',
    zIndex: '9999',
  })
  document.body.appendChild(selBox)

  window.addEventListener('mousemove', onMouseMove)
  window.addEventListener('mouseup', onMouseUp)
}

function onMouseMove(e: MouseEvent) {
  if (!selBox) return
  const x = Math.min(selStartX, e.clientX)
  const y = Math.min(selStartY, e.clientY)
  const w = Math.abs(e.clientX - selStartX)
  const h = Math.abs(e.clientY - selStartY)
  Object.assign(selBox.style, { left: x + 'px', top: y + 'px', width: w + 'px', height: h + 'px' })
}

function onMouseUp(e: MouseEvent) {
  window.removeEventListener('mousemove', onMouseMove)
  window.removeEventListener('mouseup', onMouseUp)
  if (selBox) { selBox.remove(); selBox = null }

  if (!lf.value || !containerRef.value) return
  const rect = containerRef.value.getBoundingClientRect()
  const t = lf.value.getTransform()

  const sx = Math.min(selStartX, e.clientX)
  const sy = Math.min(selStartY, e.clientY)
  const ex = Math.max(selStartX, e.clientX)
  const ey = Math.max(selStartY, e.clientY)

  const x1 = (sx - rect.left - t.TRANSLATE_X) / t.SCALE_X
  const y1 = (sy - rect.top - t.TRANSLATE_Y) / t.SCALE_Y
  const x2 = (ex - rect.left - t.TRANSLATE_X) / t.SCALE_X
  const y2 = (ey - rect.top - t.TRANSLATE_Y) / t.SCALE_Y

  if (x2 - x1 < 5 && y2 - y1 < 5) return

  let first = true
  for (const node of lf.value.graphModel.nodes) {
    if (node.x >= x1 && node.x <= x2 && node.y >= y1 && node.y <= y2) {
      lf.value.selectElementById(node.id, !first)
      first = false
    }
  }
}

const getThemeVar = (name: string) => getComputedStyle(document.documentElement).getPropertyValue(name).trim()

// 从 JSON 递归算出到目标 loop 的祖先链 [顶层loop, ..., loopId]，找不到返回 null
const findLoopPath = (nodes: any[], loopId: string, trail: string[] = []): string[] | null => {
  for (const n of nodes) {
    if (n.type !== 'loop-node') continue
    const here = [...trail, n.id]
    if (n.id === loopId) return here
    const deep = findLoopPath(n.properties?.nodeData?.children?.nodes ?? [], loopId, here)
    if (deep) return deep
  }
  return null
}

const init = (container: HTMLElement) => {
  lf.value = new LogicFlow({
    container: container,
    stopZoomGraph: false,
    snapline: true,
    nodeTextEdit: false,
    edgeTextEdit: false,
    nodeTextDraggable: false,
    edgeTextDraggable: false,
    background: {
      color: getThemeVar('--p-content-background') || '#F0F0F0',
      backgroundColor: getThemeVar('--p-content-background') || '#f5f6f7'
    },
    grid: {
      size: 10,
      visible: true,
      type: 'dot',
      config: {
        color: getThemeVar('--p-content-border-color') || '#ababab',
        thickness: 1
      }
    },
    keyboard: {
      enabled: true
    }
  })
  lf.value.on('graph:rendered', () => {
    flowId.value = lf.value.graphModel.flowId
  })
  lf.value.on('graph:transform', () => {
    zoomPercent.value = Math.round(lf.value.getTransform().SCALE_X * 100)
  })
  lf.value.on('runify:node:open-rename-dialog', (call: any) => {
    reNameDialogRef.value?.open(call)
  })
  lf.value.on('runify:node:open-add-node-dialog', (setting: any) => {
    addNodeDialogRef.value?.open(setting)
  })
  lf.value.setTheme({
    bezier: {
      stroke: getThemeVar('--p-surface-400') || '#afafaf',
      strokeWidth: 1
    }
  })
  lf.value.batchRegister([...Object.keys(nodes).map((key) => nodes[key].default), RunEdge])
  lf.value.setDefaultEdgeType('run-edge')

  // 通用：按 loop 节点 id 拿到它循环体的 lf；不传/'main' → 主画布 lf。
  // 目标埋多深、祖先是否折叠都会自动逐层展开并等渲染完成后返回（异步）。
  lf.value.graphModel.getLf = async (loopId?: string): Promise<any> => {
    const main = lf.value
    if (!loopId || loopId === 'main') return main
    const path = findLoopPath(main.graphModel.nodes, loopId)
    if (!path) throw new Error(`lf 不存在: ${loopId}`)

    let parent = main
    for (const id of path) {
      let sub = (parent.graphModel.getNodeModelById(id) as any)?.__subLf
      if (!sub) {
        const p = parent
        sub = await new Promise((resolve, reject) => {
          const onReady = (e: any) => {
            if (e.loopId !== id) return
            main.graphModel.eventCenter.off('runify:subcanvas:rendered', onReady)
            clearTimeout(timer)
            resolve(e.lf)
          }
          const timer = setTimeout(() => {
            main.graphModel.eventCenter.off('runify:subcanvas:rendered', onReady)
            reject(new Error(`展开超时: ${id}`))
          }, 3000)
          main.graphModel.eventCenter.on('runify:subcanvas:rendered', onReady)
          // 在“该 loop 所在那层的 lf”上触发展开（loop-node 按 id 匹配 onExpandBody）
          p.graphModel.eventCenter.emit('runify:node:expand-body', id)
        })
      }
      parent = sub
    }
    return parent
  }
}
const render = (data?: LogicFlow.GraphConfigData) => {
  lf.value.render(data ? data : {})
}
onMounted(() => {
  if (containerRef.value) {
    init(containerRef.value)
  }
  window.addEventListener('keydown', onKeyDown)
  window.addEventListener('keyup', onKeyUp)
  window.addEventListener('mousedown', onMouseDown)
})
onBeforeUnmount(() => {
  window.removeEventListener('keydown', onKeyDown)
  window.removeEventListener('keyup', onKeyUp)
  window.removeEventListener('mousedown', onMouseDown)
  window.removeEventListener('mousemove', onMouseMove)
  window.removeEventListener('mouseup', onMouseUp)
  if (selBox) { selBox.remove(); selBox = null }
})
const getGraphData = () => {
  return lf.value.getGraphData()
}
defineExpose({
  init,
  render,
  getGraphData,
  validateWorkflow,
  // 程序化驱动 agent 的宿主（如项目级 AI 生成页）需要直接访问 lf 与自动布局
  getLf: () => lf.value,
  autoLayout: onAutoLayout
})
</script>
<style lang="scss">
.workflow-toolbar {
  position: absolute;
  right: 16px;
  bottom: 16px;
  // 高于 AI 生成遮罩(20)：生成过程中仍可缩放/适应屏幕
  z-index: 25;
}
// 全屏时 PrimeVue 弹出层需要高于画布 wrapper (z-index: 999)
.p-overlay-mask {
  z-index: 1001 !important;
}
.p-tieredmenu {
  z-index: 1001 !important;
}
</style>
