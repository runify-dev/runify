<template>
  <div style="width: 100%; height: 100%" id="run-workflow-container"></div>
  <TeleportContainer v-if="flowId" :flow-id="flowId" />
</template>
<script setup lang="ts">
import LogicFlow from '@logicflow/core'
import '@logicflow/core/dist/index.css'
import RunEdge from './common/edge'
import { onMounted, ref } from 'vue'
const nodes: any = import.meta.glob('./nodes/**/index.ts', { eager: true })
import { getTeleport } from '@/workflow/common/teleport'
const TeleportContainer = getTeleport()
const lf = ref()
const flowId = ref('')
const init = (container: HTMLElement) => {
  lf.value = new LogicFlow({
    // 容器配置
    container: container,
    stopZoomGraph: false,
    snapline: true,
    nodeTextEdit: false,
    edgeTextEdit: false,
    nodeTextDraggable: false,
    edgeTextDraggable: false,
    background: {
      color: '#F0F0F0',
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
  lf.value.on('graph:rendered', () => {
    flowId.value = lf.value.graphModel.flowId
  })
  lf.value.setTheme({
    bezier: {
      stroke: '#afafaf',
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
})
const getGraphData = () => {
  return lf.value.getGraphData()
}
defineExpose({ init, render, getGraphData })
</script>
<style lang="scss"></style>
