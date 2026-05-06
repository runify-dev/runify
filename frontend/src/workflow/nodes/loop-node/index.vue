<template>
  <SimpleNodeContainer ref="containerRef" :model="model" :validate="validate" :submit="submit" :menu-items="loopMenuItems">
    <template #header>
      <div class="loop-toggle-btn" @click.stop="toggleFullscreen" title="全屏">
        <i class="pi pi-window-maximize"></i>
      </div>
      <div class="loop-toggle-btn" @click.stop="handleToggle" :title="expanded.value ? '收起循环体' : '展开循环体'">
        <i :class="`pi ${expanded?'pi-chevron-up':'pi-chevron-down'}`" ></i>
      </div>
    </template>
    <Content ref="contentRef" :WorkflowType="WorkflowType" :details="details"></Content>
  </SimpleNodeContainer>

  <div
    v-if="expanded"
    ref="panelRef"
    class="loop-canvas-panel"
    :class="{ 'loop-selected': model.isSelected, 'loop-nested': isNested }"
  >
    <SubCanvas ref="subCanvasRef" :workflow-type="workflowType" :modelValue="initialChildrenData" :default-data="defaultLoopData" :show-toolbar="false" />
  </div>
</template>
<script setup lang="ts">
import SimpleNodeContainer from '@/workflow/common/SimpleNodeContainer.vue'
import type { BaseNodeModel } from '@logicflow/core'
import { Dagre } from '@logicflow/layout'
import LogicFlow from '@logicflow/core'

LogicFlow.use(Dagre)
import Content from './content/index.vue'
import { init } from './content'
import SubCanvas from '@/workflow/common/SubCanvas.vue'
import { computed, inject, provide, ref, nextTick, onMounted, onBeforeUnmount } from 'vue'
import { WorkflowType } from '@/workflow/common/data'
import { useNodeValidator } from '@/workflow/common/useNodeValidator'

const parentWorkflowType = inject<string>('WorkflowType') || WorkflowType.APPLICATION
const workflowType=computed(()=>{
  if (parentWorkflowType.startsWith(WorkflowType.APPLICATION)){
    return WorkflowType.APPLICATION_LOOP
  }
  if (parentWorkflowType.startsWith(WorkflowType.PROCESSOR)){
    return WorkflowType.PROCESSOR_LOOP
  }
  return  WorkflowType.APPLICATION_LOOP
})

const details = (inject('getDetails') as any)()

const getModel = inject('getModel') as () => BaseNodeModel
const model = getModel()

const getMainLf = inject('getMainLf', null) as (() => any) | null
const getParentLf = inject('getParentLf', null) as (() => any) | null
const getNodeFieldOptions = inject('getNodeFieldOptions', null) as (() => any) | null
// 子画布数据变化回调
provide('onSubGraphChange', () => saveSubCanvasData())

// 注入父画布容器（如果存在说明在子画布内，即嵌套循环）
const parentCanvasRef = inject<ReturnType<typeof ref<HTMLElement | undefined>>>('subCanvasContainer', ref<HTMLElement | undefined>(undefined))
const isNested = computed(() => !!parentCanvasRef.value)

const containerRef = ref<InstanceType<typeof SimpleNodeContainer>>()
const contentRef = ref()
const subCanvasRef = ref()
const panelRef = ref<HTMLElement>()
let panelOriginalParent: HTMLElement | null = null

const expanded = ref(model.properties?.expanded ?? false)
let isFullscreen = false

const loopMenuItems = computed(() => [{
  label: expanded.value ? '收起循环体' : '展开循环体',
  value: 'toggle-loop',
  command: () => handleToggle()
}])

const initialChildrenData = model.properties?.nodeData?.children || { nodes: [], edges: [] }

const defaultLoopData = {
  nodes: [{
    id: 'loop-start-node',
    type: 'loop-start-node',
    x: 100,
    y: 150,
    properties: {
      width: 200,
      height: 50,
      name: '循环开始',
      isHovered: false,
      field_list: []
    }
  }],
  edges: []
}

function findNodeWrap(): HTMLElement | null {
  const container = document.querySelector(`[data-node-id="${model.id}"]`)
  if (container) {
    const wrap = container.querySelector('.custom-node-wrap')
    if (wrap) return wrap as HTMLElement
  }
  return null
}

function updatePosition() {
  if (!expanded.value || isFullscreen) return
  const el = panelRef.value
  const nodeEl = findNodeWrap()
  if (!el || !nodeEl) return
  const rect = nodeEl.getBoundingClientRect()
  const scale = model.graphModel.transformModel?.SCALE_X || 1
  const overlap = 2 * scale
  const panelHeight = 150 * scale

  // position: absolute，坐标相对于所在容器
  const container = el.parentElement
  const cRect = container ? container.getBoundingClientRect() : { left: 0, top: 0 }

  Object.assign(el.style, {
    position: 'absolute',
    left: `${rect.left - cRect.left}px`,
    top: `${rect.bottom - cRect.top - overlap}px`,
    width: `${rect.width}px`,
    height: `${panelHeight}px`,
  })
}

let raf = 0
function tick() {
  updatePosition()
  raf = requestAnimationFrame(tick)
}

function startTracking() {
  tick()
}

function stopTracking() {
  cancelAnimationFrame(raf)
}


function syncToParentCanvas() {
  const parentLf = getParentLf?.() || getMainLf?.()
  if (!parentLf) return
  // 直接通过 nodeModelMap 拿原始模型，绕过 Vue reactive 代理
  const rawNode = parentLf.graphModel?.nodeModelMap?.get?.(model.id)
    || parentLf.graphModel?.getNodeModelById?.(model.id)
  if (!rawNode) return
  const nodeData = model.properties.nodeData
  if (!rawNode.properties) {
    rawNode.properties = { nodeData }
  } else {
    rawNode.properties.nodeData = { ...rawNode.properties.nodeData, ...nodeData }
  }
}

function saveSubCanvasData() {
  const data = subCanvasRef.value?.getGraphData()
  if (data) {
    syncNestedLoopData(data)
    model.properties.nodeData = {
      ...model.properties.nodeData,
      children: data
    }
    syncToParentCanvas()
  }
}

function syncNestedLoopData(graphData: any) {
  if (!graphData?.nodes) return
  const subLf = subCanvasRef.value?.lf
  if (!subLf) return
  for (const node of graphData.nodes) {
    if (node.type === 'loop-node') {
      const graphNode = subLf.graphModel.nodes.find((n: any) => n.id === node.id)
      console.log('[syncNestedLoopData] node.id:', node.id, 'graphNode found:', !!graphNode, 'children:', graphNode?.properties?.nodeData?.children)
      if (graphNode?.properties?.nodeData?.children) {
        node.properties.nodeData = graphNode.properties.nodeData
        node.properties.nodeData.children = graphNode.properties.nodeData.children
      }
      if (node.properties?.nodeData?.children?.nodes) {
        syncNestedLoopData(node.properties.nodeData.children)
      }
    }
  }
}

function handleToggle() {
  if (expanded.value) {
    saveSubCanvasData()
    expanded.value = false
    isFullscreen = false
    stopTracking()
    movePanelBack()
  } else {
    expanded.value = true
    nextTick(() => {
      movePanelToBody()
      // 用 render() 加载最新保存的数据（绕过 Vue 响应式，避免 Teleport 冲突）
      const data = model.properties?.nodeData?.children
      subCanvasRef.value?.render(data && data.nodes?.length ? data : undefined)
      subCanvasRef.value?.centerContent()
      startTracking()
    })
  }
  model.properties.expanded = expanded.value
}

function onFullscreenResize() {
  const el = panelRef.value
  if (!el || !isFullscreen) return
  el.style.width = `${window.innerWidth}px`
  el.style.height = `${window.innerHeight}px`
  nextTick(() => {
    subCanvasRef.value?.centerContent()
  })
}

let autoExpandedForFullscreen = false

function toggleFullscreen() {
  // 未展开时，临时展开面板直接进入全屏
  if (!expanded.value) {
    expanded.value = true
    autoExpandedForFullscreen = true
    model.properties.expanded = true
    nextTick(() => {
      movePanelToBody()
      const data = model.properties?.nodeData?.children
      subCanvasRef.value?.render(data && data.nodes?.length ? data : undefined)
      isFullscreen = true
      applyFullscreen()
    })
    return
  }
  isFullscreen = !isFullscreen
  applyFullscreen()
}

function applyFullscreen() {
  const el = panelRef.value
  if (!el) return

  if (isFullscreen) {
    stopTracking()
    Object.assign(el.style, {
      position: 'fixed',
      top: '0',
      left: '0',
      width: `${window.innerWidth}px`,
      height: `${window.innerHeight}px`,
      border: 'none',
      zIndex: '1000',
    })
    window.addEventListener('resize', onFullscreenResize)
    subCanvasRef.value?.setFullscreen(true)
    if (!fsToolbar) {
      fsToolbar = createFullscreenToolbar()
      document.body.appendChild(fsToolbar)
    }
  } else {
    window.removeEventListener('resize', onFullscreenResize)
    if (fsToolbar) { fsToolbar.remove(); fsToolbar = null }
    el.style.border = ''
    el.style.borderRadius = ''
    el.style.zIndex = ''
    subCanvasRef.value?.setFullscreen(false)

    // 全屏前未展开 → 退出全屏后收回面板
    if (autoExpandedForFullscreen) {
      autoExpandedForFullscreen = false
      saveSubCanvasData()
      expanded.value = false
      isFullscreen = false
      stopTracking()
      movePanelBack()
      model.properties.expanded = false
      return
    }

    updatePosition()
    startTracking()
  }

  nextTick(() => {
    subCanvasRef.value?.centerContent()
  })
}

const validate = () => {
  if (expanded.value) {
    saveSubCanvasData()
  }
  return contentRef.value ? contentRef.value.validate() : Promise.resolve(true)
}

useNodeValidator(model, containerRef)

const submit = () => {
  if (expanded.value) {
    saveSubCanvasData()
  }
  return contentRef.value
    ? contentRef.value.submit().then((formValues: any) => {
        model.properties.nodeData = {
          ...model.properties.nodeData,
          ...formValues
        }
        syncToParentCanvas()
        return formValues
      })
    : Promise.resolve(true)
}

onMounted(() => {
  init({ model, workflowType: parentWorkflowType, details })

  if (expanded.value) {
    nextTick(() => {
      movePanelToBody()
      const data = model.properties?.nodeData?.children
      subCanvasRef.value?.render(data && data.nodes?.length ? data : undefined)
      subCanvasRef.value?.centerContent()
      startTracking()
    })
  }

  model.graphModel.eventCenter.on('runify:node:expand-body', onExpandBody)
})

function onExpandBody(nodeId: string) {
  if (nodeId !== model.id) return

  function afterRender() {
    const pendingChild = model.properties._pendingOpenChild
    if (!pendingChild) return
    delete model.properties._pendingOpenChild
    const subLf = subCanvasRef.value?.lf
    if (!subLf) return
    const childNode = subLf.graphModel.nodes.find((n: any) => n.id === pendingChild)
    const isChildLoop = childNode?.type === 'loop-node'
    if (isChildLoop) {
      subLf.graphModel.eventCenter.emit('runify:node:expand-body', pendingChild)
    } else {
      setTimeout(() => {
        subLf.graphModel.eventCenter.emit('runify:node:open-settings', pendingChild)
      }, 500)
    }
  }

  if (expanded.value) {
    afterRender()
    return
  }
  expanded.value = true
  model.properties.expanded = true
  nextTick(() => {
    movePanelToBody()
    const data = model.properties?.nodeData?.children
    subCanvasRef.value?.render(data && data.nodes?.length ? data : undefined)
    startTracking()
    afterRender()
  })
}

onBeforeUnmount(() => {
  stopTracking()
  window.removeEventListener('resize', onFullscreenResize)
  model.graphModel.eventCenter.off('runify:node:expand-body', onExpandBody)
  // 把面板移回原位，让 Vue 正常卸载，避免 foreignObject 已销毁导致报错
  movePanelBack()
})

let fsToolbar: HTMLDivElement | null = null
function createFullscreenToolbar() {
  const tb = document.createElement('div')
  tb.className = 'loop-panel-toolbar'
  // 工具栏在 body 上，用 fixed 定位，z-index 高于面板(1000)
  tb.style.position = 'fixed'
  tb.style.zIndex = '1001'
  tb.innerHTML = `
    <button class="loop-toolbar-btn" title="放大" data-action="zoom-in">
      <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/><line x1="11" y1="8" x2="11" y2="14"/><line x1="8" y1="11" x2="14" y2="11"/></svg>
    </button>
    <div class="loop-toolbar-zoom">100%</div>
    <button class="loop-toolbar-btn" title="缩小" data-action="zoom-out">
      <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/><line x1="8" y1="11" x2="14" y2="11"/></svg>
    </button>
    <div class="loop-toolbar-divider"></div>
    <button class="loop-toolbar-btn" title="适应屏幕" data-action="fit-view">
      <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M8 3H5a2 2 0 0 0-2 2v3m18 0V5a2 2 0 0 0-2-2h-3m0 18h3a2 2 0 0 0 2-2v-3M3 16v3a2 2 0 0 0 2 2h3"/></svg>
    </button>
    <button class="loop-toolbar-btn" title="自动布局" data-action="auto-layout">
      <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="3" width="7" height="7" rx="1"/><rect x="14" y="3" width="7" height="7" rx="1"/><rect x="3" y="14" width="7" height="7" rx="1"/><rect x="14" y="14" width="7" height="7" rx="1"/></svg>
    </button>
    <div class="loop-toolbar-divider"></div>
    <button class="loop-toolbar-btn" title="拖拽模式" data-action="drag">
      <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M5 9l4-4 4 4"/><path d="M5 15l4 4 4-4"/><path d="M15 5l4 4-4 4"/><path d="M15 15l4-4-4-4"/></svg>
    </button>
    <button class="loop-toolbar-btn" title="框选模式（空格）" data-action="select">
      <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="3" width="18" height="18" rx="2" ry="2" stroke-dasharray="4 2"/></svg>
    </button>
    <div class="loop-toolbar-divider"></div>
    <button class="loop-toolbar-btn" title="退出全屏" data-action="exit-fullscreen">
      <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="4 14 10 14 10 20"/><polyline points="20 10 14 10 14 4"/><line x1="14" y1="10" x2="21" y2="3"/><line x1="3" y1="21" x2="10" y2="14"/></svg>
    </button>
  `
  const getLf = () => {
  const lf = subCanvasRef.value?.lf
  // defineExpose 的 ref 可能被 Vue 自动解包
  return lf?.value ?? lf
}
  const zoomDisplay = tb.querySelector('.loop-toolbar-zoom') as HTMLElement
  const dragBtn = tb.querySelector('[data-action="drag"]') as HTMLElement
  const selectBtn = tb.querySelector('[data-action="select"]') as HTMLElement
  function applySubCanvasMode(mode: 'drag' | 'select') {
    subCanvasRef.value?.setSubCanvasMode(mode)
    dragBtn.classList.toggle('loop-toolbar-btn-active', mode === 'drag')
    selectBtn.classList.toggle('loop-toolbar-btn-active', mode === 'select')
  }
  applySubCanvasMode('drag')
  tb.querySelector('[data-action="zoom-in"]')?.addEventListener('click', () => {
    const lf = getLf()
    if (!lf) return
    lf.zoom(true)
    if (zoomDisplay) zoomDisplay.textContent = Math.round(lf.getTransform().SCALE_X * 100) + '%'
  })
  tb.querySelector('[data-action="zoom-out"]')?.addEventListener('click', () => {
    const lf = getLf()
    if (!lf) return
    lf.zoom(false)
    if (zoomDisplay) zoomDisplay.textContent = Math.round(lf.getTransform().SCALE_X * 100) + '%'
  })
  tb.querySelector('[data-action="fit-view"]')?.addEventListener('click', () => {
    const lf = getLf()
    if (!lf) return
    lf.fitView(40, 40)
    if (zoomDisplay) zoomDisplay.textContent = Math.round(lf.getTransform().SCALE_X * 100) + '%'
  })
  tb.querySelector('[data-action="auto-layout"]')?.addEventListener('click', () => {
    const lf = getLf()
    if (!lf?.extension?.dagre) return
    lf.extension.dagre.layout({ rankdir: 'LR', nodesep: 60, ranksep: 100 })
    lf.fitView(40, 40)
    if (zoomDisplay) zoomDisplay.textContent = Math.round(lf.getTransform().SCALE_X * 100) + '%'
  })
  tb.querySelector('[data-action="drag"]')?.addEventListener('click', () => applySubCanvasMode('drag'))
  tb.querySelector('[data-action="select"]')?.addEventListener('click', () => applySubCanvasMode('select'))
  tb.querySelector('[data-action="exit-fullscreen"]')?.addEventListener('click', () => toggleFullscreen())
  return tb
}

function movePanelToBody() {
  const el = panelRef.value
  if (!el) return
  // 嵌套循环时移到外层 SubCanvas 容器，否则移到主画布容器
  const target = parentCanvasRef.value || document.querySelector('#canvas-wrapper') || document.body
  if (el.parentElement === target) return
  panelOriginalParent = el.parentElement
  target.appendChild(el)
}

function movePanelBack() {
  const el = panelRef.value
  if (!el || !panelOriginalParent) return
  panelOriginalParent.appendChild(el)
  panelOriginalParent = null
}
</script>

<style>
.loop-toggle-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 20px;
  border-radius: 4px;
  cursor: pointer;
  color: #5d6276;
  flex-shrink: 0;
}
.loop-toggle-btn:hover {
  background: #f0f3f8;
  color: #3370ff;
}

.loop-canvas-panel {
  position: relative;
  background: #fff;
  border: 2px solid #dddfe6;
  border-top: none;
  border-radius: 0 0 8px 8px;
  overflow: hidden;
  box-sizing: border-box;
  pointer-events: none;
}

.loop-canvas-panel.loop-selected {
  border-color: var(--primary-color);
  border-top: none;
}

.loop-canvas-panel .sub-canvas-container {
  border: none;
  border-radius: 0;
  height: 100%;
  margin-bottom: 0;
  pointer-events: auto;
}

.loop-canvas-panel.loop-fullscreen {
  position: fixed !important;
  top: 0 !important;
  left: 0 !important;
  width: 100vw !important;
  border: none !important;
  border-radius: 0 !important;
  z-index: 1000 !important;
}

.loop-panel-toolbar {
  position: absolute;
  bottom: 12px;
  left: 12px;
  display: flex;
  align-items: center;
  gap: 2px;
  background: #fff;
  border: 1px solid #dddfe6;
  border-radius: 8px;
  padding: 4px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  z-index: 10;
  pointer-events: auto;
}

.loop-toolbar-btn {
  width: 32px;
  height: 32px;
  border: none;
  border-radius: 6px;
  background: transparent;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #5d6276;
  transition: background 0.15s, color 0.15s;
}

.loop-toolbar-btn:hover {
  background: #f0f3f8;
  color: #3370ff;
}

.loop-toolbar-btn-active {
  background: #e8f0fe;
  color: #3370ff;
}

.loop-toolbar-zoom {
  min-width: 40px;
  text-align: center;
  font-size: 12px;
  color: #5d6276;
  font-weight: 500;
}

.loop-toolbar-divider {
  width: 1px;
  height: 20px;
  background: #dddfe6;
  margin: 0 2px;
}
</style>
