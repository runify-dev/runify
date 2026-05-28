<template>
  <div class="layout-content-height w-full relative overflow-hidden" id="canvas-wrapper">
    <div style="width: 100%; height: 100%" id="run-workflow-container"></div>
    <CanvasToolbar
      class="workflow-toolbar"
      :zoom-percent="zoomPercent"
      :mode="interactionMode"
      :fullscreen="isFullscreen"
      @zoom-in="onZoomIn"
      @zoom-out="onZoomOut"
      @fit-view="onFitView"
      @auto-layout="onAutoLayout"
      @set-mode="onSetMode"
      @toggle-fullscreen="toggleFullscreen"
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
import LogicFlow from '@logicflow/core'
import '@logicflow/core/dist/index.css'
import '@logicflow/extension/lib/style/index.css'
import dagre from 'dagre'

import RunEdge from './common/edge'
import { onMounted, onBeforeUnmount, ref, provide, inject, nextTick } from 'vue'
import type { ValidationResult } from './common/type'
import { WorkflowType } from './common/data'

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

// ── 校验整个工作流（短路） ──
async function validateWorkflow(): Promise<{ valid: boolean; nodeId?: string }> {
  if (!lf.value) return { valid: true }
  const { nodes } = lf.value.getGraphData()
  for (const node of nodes) {
    const validateFn = nodeValidators.get(node.type)
    if (!validateFn) continue
    try {
      const result = validateFn(node.properties?.nodeData ?? {}, nodeValidators, workflowType)
      if (!result.valid) {
        const failedNodeId = result.failedNodeId ?? node.id
        const path = result.failedPath ?? []
        // failedNodeId !== node.id 说明是子节点失败，需要展开父循环
        if (failedNodeId !== node.id && path.length === 0) {
          path.push(node.id)
        }
        if (path.length) {
          await expandAndOpen(path, failedNodeId)
        } else {
          selectAndOpenNode(node.id)
        }
        return { valid: false, nodeId: failedNodeId }
      }
    } catch {
      selectAndOpenNode(node.id)
      return { valid: false, nodeId: node.id }
    }
  }
  return { valid: true }
}

async function expandAndOpen(path: string[], failedNodeId: string) {
  if (!lf.value) return
  const graphNodes = lf.value.graphModel.nodes

  // 逐层展开循环体，设置 _pendingOpenChild 指向下一层
  for (let i = 0; i < path.length; i++) {
    const loopNodeId = path[i]
    const loopNode = graphNodes.find((n: any) => n.id === loopNodeId)
    if (!loopNode) continue

    // 最内层的 _pendingOpenChild 指向失败节点，其余指向下一层循环
    loopNode.properties._pendingOpenChild = i === 0 ? failedNodeId : path[i - 1]
    lf.value.graphModel.eventCenter.emit('runify:node:expand-body', loopNodeId)
    await nextTick()
    await new Promise((r) => setTimeout(r, 300))
  }
}

function selectAndOpenNode(nodeId: string) {
  if (!lf.value) return
  lf.value.selectElementById(nodeId)
  lf.value.graphModel.eventCenter.emit('runify:node:open-settings', nodeId)
}

const TeleportContainer = getTeleport()
const lf = ref()
const flowId = ref('')
provide('getMainLf', () => lf.value)
provide('canvasType', 'main')

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

  const { graphModel } = lf.value
  const nodeMap = new Map<string, any>(graphModel.nodes.map((n: any) => [n.id, n]))

  // 按源节点分组，对每个节点的出边分别按锚点 Y 坐标排序
  const edgesBySource = new Map<string, any[]>()

  graphModel.edges.forEach((edge: any) => {
    if (!edgesBySource.has(edge.sourceNodeId)) {
      edgesBySource.set(edge.sourceNodeId, [])
    }
    edgesBySource.get(edge.sourceNodeId)!.push(edge)
  })

  const sortedEdges: any[] = []
  edgesBySource.forEach((edges, nodeId) => {
    const sourceNode = nodeMap.get(nodeId)
    if (sourceNode) {
      edges.sort((a, b) => {
        const anchorA = sourceNode.anchors.find((an: any) => an.id === a.sourceAnchorId)
        const anchorB = sourceNode.anchors.find((an: any) => an.id === b.sourceAnchorId)
        return (anchorA?.y ?? 0) - (anchorB?.y ?? 0)
      })
    }
    sortedEdges.push(...edges)
  })

  // 使用 Dagre 布局
  const g = new dagre.graphlib.Graph()
  g.setGraph({ rankdir: 'LR', align: '', nodesep: 60, ranksep: 100 })
  g.setDefaultEdgeLabel(() => ({}))

  graphModel.nodes.forEach((node: any) => {
    g.setNode(node.id, { width: node.width || 150, height: node.height || 50 })
  })

  sortedEdges.forEach((edge: any) => {
    g.setEdge(edge.sourceNodeId, edge.targetNodeId)
  })

  dagre.layout(g)

  // 更新节点坐标
  graphModel.nodes.forEach((node: any) => {
    const pos = g.node(node.id)
    if (pos) {
      node.x = pos.x
      node.y = pos.y
    }
  })

  // 刷新边的位置
  graphModel.edges.forEach((edge: any) => edge.updatePathByAnchor?.())

  lf.value.fitView(40, 40)
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

function toggleFullscreen() {
  const el = document.querySelector('#canvas-wrapper') as HTMLElement
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
    // 工具栏移回原位
    const toolbar = document.body.querySelector('.canvas-toolbar') as HTMLElement
    if (toolbar && toolbarOriginalParent) {
      toolbar.style.position = ''
      toolbar.style.zIndex = ''
      toolbar.style.bottom = ''
      toolbar.style.right = ''
      toolbarOriginalParent.appendChild(toolbar)
      toolbarOriginalParent = null
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
  const c = document.querySelector('#canvas-wrapper')
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

  if (!lf.value) return
  const rect = document.querySelector('#run-workflow-container')!.getBoundingClientRect()
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
}
const render = (data?: LogicFlow.GraphConfigData) => {
  lf.value.render(data ? data : {})
}
onMounted(() => {
  const container = document.querySelector('#run-workflow-container')
  if (container) {
    init(container as HTMLElement)
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
defineExpose({ init, render, getGraphData, validateWorkflow })
</script>
<style lang="scss">
.workflow-toolbar {
  position: absolute;
  right: 16px;
  bottom: 16px;
  z-index: 10;
}
// 全屏时 PrimeVue 弹出层需要高于画布 wrapper (z-index: 999)
.p-overlay-mask {
  z-index: 1001 !important;
}
.p-tieredmenu {
  z-index: 1001 !important;
}
</style>
