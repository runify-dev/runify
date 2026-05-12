<template>
  <div
    class="sub-canvas-container"
    ref="containerRef"
    @wheel.stop
    @pointerdown.stop
  >
    <CanvasToolbar
      v-if="false"
      class="sub-canvas-toolbar"
      :zoom-percent="zoomPercent"
      :mode="getEffectiveMode() || 'drag'"
      :show-zoom-out="false"
      @zoom-in="onZoomIn"
      @zoom-out="onZoomOut"
      @fit-view="onFitView"
    />
  </div>
  <TeleportContainer v-if="flowId" :flow-id="flowId"/>
</template>
<script setup lang="ts">
import LogicFlow from '@logicflow/core'
import '@logicflow/core/dist/index.css'
import '@logicflow/extension/lib/style/index.css'
import { Dagre } from '@logicflow/layout'

LogicFlow.use(Dagre)
import RunEdge from './edge'
import CanvasToolbar from './CanvasToolbar.vue'
import {getTeleport} from './teleport'
import {onMounted, onBeforeUnmount, ref, inject, provide, watch} from 'vue'
import type {WorkflowType} from "@/workflow/common/data.ts";

const TeleportContainer = getTeleport()

const props = defineProps<{
  workflowType:WorkflowType
  modelValue?: LogicFlow.GraphConfigData
  showToolbar?: boolean
  defaultData?: LogicFlow.GraphConfigData
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: LogicFlow.GraphConfigData): void
}>()

const containerRef = ref<HTMLElement>()
const lf = ref<LogicFlow>()
const flowId = ref('')

const nodes: any = import.meta.glob('../nodes/*/index.ts', {eager: true})
const getModel = inject('getModel') as () => any
const getMainLf = inject('getMainLf', null) as (() => any) | null
const onSubGraphChange = inject<() => void>('onSubGraphChange', () => {})
const parentNodeFieldOptions = inject('getNodeFieldOptions', null) as ((withSlef?:boolean) => any[]) | null
const parentGetGlobalFieldOptions=inject('getGlobalFieldOptions',null) as (() => any[]) | null
// ── 模式同步 ──
const canvasInteractionMode = inject<ReturnType<typeof ref<'drag' | 'select'>>>('canvasInteractionMode', ref('drag'))
const canvasSpaceHeld = inject<ReturnType<typeof ref<boolean>>>('canvasSpaceHeld', ref(false))
let localSpaceHeld = false // 普通变量，不触发 Vue 重渲染（避免 Teleport 冲突）
let fullscreenActive = false // 普通变量，不触发 Vue 重渲染（避免 Teleport 冲突）
let subCanvasMode: 'drag' | 'select' = 'drag' // 子画布自身模式（工具栏设置）
// 普通函数（不用 computed），因为 fullscreenActive/localSpaceHeld 是非响应式的
function getEffectiveMode() {
  if (fullscreenActive) {
    return localSpaceHeld ? 'select' : subCanvasMode
  }
  return canvasSpaceHeld.value ? 'select' : canvasInteractionMode.value
}

// 直接同步 editConfig，不用 watch（避免触发 Vue Teleport 更新冲突）
function syncEditConfig() {
  if (!lf.value) return
  const mode = getEffectiveMode()
  lf.value.updateEditConfig(
    mode === 'select'
      ? {stopMoveGraph: true, adjustNodePosition: false}
      : {stopMoveGraph: false, adjustNodePosition: true}
  )
}

// 主画布模式变化时同步
watch(canvasInteractionMode, () => syncEditConfig())
watch(canvasSpaceHeld, () => syncEditConfig())

// ── 缩放 ──
const zoomPercent = ref(100)

function onZoomIn() {
  lf.value?.zoom(true)
  zoomPercent.value = Math.round(lf.value!.getTransform().SCALE_X * 100)
}

function onZoomOut() {
  lf.value?.zoom(false)
  zoomPercent.value = Math.round(lf.value!.getTransform().SCALE_X * 100)
}

function onFitView() {
  lf.value?.fitView()
  zoomPercent.value = Math.round(lf.value!.getTransform().SCALE_X * 100)
}

function init(container: HTMLElement) {
  lf.value = new LogicFlow({
    container,
    stopZoomGraph: false,
    snapline: true,
    nodeTextEdit: false,
    edgeTextEdit: false,
    nodeTextDraggable: false,
    edgeTextDraggable: false,
    background: {
      color: '#F8F8F8',
      backgroundColor: '#f5f6f7'
    },
    grid: {
      size: 10,
      visible: true,
      type: 'dot',
      config: {
        color: '#ababab',
        thickness: 1
      }
    },
    keyboard: {
      enabled: true
    }
  })

  // 挂载父级 getNodeFieldOptions 到 graphModel
  if (parentNodeFieldOptions && lf.value.graphModel) {
    lf.value.graphModel.getParentNodeFieldOptions = ()=>parentNodeFieldOptions(true)
    lf.value.graphModel.parentGetGlobalFieldOptions=parentGetGlobalFieldOptions
    lf.value.graphModel.getParentModel=getModel
  }

  lf.value.on('graph:rendered', () => {
    flowId.value = lf.value!.graphModel.flowId || ''
  })

  lf.value.setTheme({
    bezier: {
      stroke: '#afafaf',
      strokeWidth: 1
    }
  })

  lf.value.batchRegister([...Object.keys(nodes).map((key) => nodes[key].default), RunEdge])
  lf.value.setDefaultEdgeType('run-edge')
  syncEditConfig()

  lf.value.on('graph:transform', () => {
    zoomPercent.value = Math.round(lf.value!.getTransform().SCALE_X * 100)
  })

  if (getMainLf) {
    lf.value.on('runify:node:open-add-node-dialog', (setting: any) => {
      const mainLf = getMainLf()
      if (mainLf) {
        setting.workflowType=setting.workflowType?setting.workflowType:props.workflowType
        mainLf.graphModel.eventCenter.emit('runify:node:open-add-node-dialog', setting)
      }
    })
    lf.value.on('runify:node:open-rename-dialog', (call: any) => {
      const mainLf = getMainLf()
      if (mainLf) {
        mainLf.graphModel.eventCenter.emit('runify:node:open-rename-dialog', call)
      }
    })
  }

  // 数据变化时同步通知父组件（loop-node 等），不用 debounce 避免 SubCanvas 销毁后回调丢失数据
  for (const evt of ['node:add', 'node:delete', 'node:drag', 'edge:add', 'edge:delete',
    'node:properties-change', 'edge:properties-change', 'runify:node:data-saved']) {
    lf.value.on(evt, () => onSubGraphChange())
  }
}

function render(data?: LogicFlow.GraphConfigData) {
  if (data && data.nodes && data.nodes.length > 0) {
    lf.value?.render(data)
  } else if (props.defaultData?.nodes?.length) {
    lf.value?.render(props.defaultData)
  } else {
    lf.value?.render({nodes: [], edges: []})
  }
}

function getGraphData() {
  return lf.value?.getGraphData()
}

function centerContent() {
  if (!lf.value) return
  setTimeout(() => {
    if (!lf.value) return
    const nodes = lf.value.graphModel.nodes
    if (nodes.length === 0) return
    console.log('[centerContent] container size:', containerRef.value?.offsetWidth, containerRef.value?.offsetHeight)
    console.log('[centerContent] nodes:', nodes.length, nodes.map((n: any) => ({
      id: n.id,
      x: n.x,
      y: n.y,
      w: n.width,
      h: n.height
    })))
    const transformBefore = lf.value.getTransform()
    if (nodes.length === 1) {
      lf.value.focusOn({id: nodes[0].id})
    } else {
      lf.value.fitView()
    }
  }, 200)
}

onMounted(() => {
  if (containerRef.value) {
    init(containerRef.value)
  }
  window.addEventListener('mousedown', onSubCanvasMouseDown)
  window.addEventListener('keydown', onSpaceKeyDown, true)
  window.addEventListener('keyup', onSpaceKeyUp, true)
})

// ── 框选（纯 vanilla DOM，不用 Vue Teleport）──
let selBox: HTMLDivElement | null = null
let selStartX = 0
let selStartY = 0

function onSubCanvasMouseDown(e: MouseEvent) {
  if (getEffectiveMode() !== 'select' || e.button !== 0 || !lf.value) return
  const container = containerRef.value
  if (!container || !container.contains(e.target as Node)) return
  if ((e.target as HTMLElement).closest('[data-node-id]')) return

  const rect = container.getBoundingClientRect()
  selStartX = e.clientX
  selStartY = e.clientY

  selBox = document.createElement('div')
  selBox.className = 'sub-selection-box'
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

  window.addEventListener('mousemove', onSubCanvasMouseMove)
  window.addEventListener('mouseup', onSubCanvasMouseUp)
}

function onSubCanvasMouseMove(e: MouseEvent) {
  if (!selBox) return
  const x = Math.min(selStartX, e.clientX)
  const y = Math.min(selStartY, e.clientY)
  const w = Math.abs(e.clientX - selStartX)
  const h = Math.abs(e.clientY - selStartY)
  Object.assign(selBox.style, {left: x + 'px', top: y + 'px', width: w + 'px', height: h + 'px'})
}

function onSubCanvasMouseUp(e: MouseEvent) {
  window.removeEventListener('mousemove', onSubCanvasMouseMove)
  window.removeEventListener('mouseup', onSubCanvasMouseUp)
  if (selBox) {
    selBox.remove();
    selBox = null
  }

  const localLf = lf.value
  const container = containerRef.value
  if (!localLf || !container) return

  const rect = container.getBoundingClientRect()
  const t = localLf.getTransform()

  const sx = Math.min(selStartX, e.clientX) - rect.left
  const sy = Math.min(selStartY, e.clientY) - rect.top
  const ex = Math.max(selStartX, e.clientX) - rect.left
  const ey = Math.max(selStartY, e.clientY) - rect.top

  const x1 = (sx - t.TRANSLATE_X) / t.SCALE_X
  const y1 = (sy - t.TRANSLATE_Y) / t.SCALE_Y
  const x2 = (ex - t.TRANSLATE_X) / t.SCALE_X
  const y2 = (ey - t.TRANSLATE_Y) / t.SCALE_Y

  if (x2 - x1 < 5 && y2 - y1 < 5) return

  let first = true
  for (const node of localLf.graphModel.nodes) {
    if (node.x >= x1 && node.x <= x2 && node.y >= y1 && node.y <= y2) {
      localLf.selectElementById(node.id, !first)
      first = false
    }
  }
}

provide('subCanvasContainer', containerRef)
provide('canvasType', 'loop')
provide('getParentLf', () => lf.value)

// ── 全屏模式空格键框选 ──
function isPointerInContainer(e: Event) {
  const c = containerRef.value
  return c && c.contains(e.target as Node)
}

function onSpaceKeyDown(e: KeyboardEvent) {
  if (!fullscreenActive) return
  if (e.code !== 'Space' || e.repeat || localSpaceHeld) return
  if (!isPointerInContainer(e)) return
  const tag = (e.target as HTMLElement)?.tagName
  if (tag === 'INPUT' || tag === 'TEXTAREA' || (e.target as HTMLElement)?.isContentEditable) return
  e.preventDefault()
  e.stopImmediatePropagation()
  localSpaceHeld = true
  syncEditConfig()
}

function onSpaceKeyUp(e: KeyboardEvent) {
  if (!fullscreenActive || !localSpaceHeld) return
  if (e.code !== 'Space') return
  e.preventDefault()
  e.stopImmediatePropagation()
  localSpaceHeld = false
  syncEditConfig()
}

onBeforeUnmount(() => {
  window.removeEventListener('mousedown', onSubCanvasMouseDown)
  window.removeEventListener('mousemove', onSubCanvasMouseMove)
  window.removeEventListener('mouseup', onSubCanvasMouseUp)
  window.removeEventListener('keydown', onSpaceKeyDown, true)
  window.removeEventListener('keyup', onSpaceKeyUp, true)
  if (selBox) {
    selBox.remove();
    selBox = null
  }
})

function setFullscreen(active: boolean) {
  console.log('[SubCanvas] setFullscreen:', active, 'container size:', containerRef.value?.offsetWidth, containerRef.value?.offsetHeight)
  fullscreenActive = active
  syncEditConfig()
}

function setSubCanvasMode(mode: 'drag' | 'select') {
  subCanvasMode = mode
  syncEditConfig()
}

defineExpose({getGraphData, render, centerContent, setFullscreen, setSubCanvasMode, lf})
</script>
<style scoped>
.sub-canvas-container {
  position: relative;
  width: 100%;
  height: 300px;
  overflow: hidden;
  background: #f5f6f7;
  margin-bottom: 8px;
}

.sub-canvas-toolbar {
  position: absolute;
  bottom: 8px;
  right: 8px;
  z-index: 10;
}
</style>
