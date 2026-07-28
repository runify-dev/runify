<template>
  <Dialog v-model:visible="visible" modal :style="{ width: showToolTab ? '46rem' : '32rem' }" header="添加节点">
    <Tabs v-if="showToolTab" value="base">
      <TabList>
        <Tab value="base">基础节点</Tab>
        <Tab value="tool">工具</Tab>
      </TabList>
      <TabPanels>
        <TabPanel value="base">
          <Menu :workflow-type="currentWorkflowType" @selected="selected"></Menu>
        </TabPanel>
        <TabPanel value="tool">
          <ToolPicker @selected="selected"></ToolPicker>
        </TabPanel>
      </TabPanels>
    </Tabs>
    <Menu v-else :workflow-type="currentWorkflowType" @selected="selected"></Menu>
  </Dialog>
</template>
<script setup lang="ts">
import { ref, computed, inject } from 'vue'
import Tabs from 'primevue/tabs'
import TabList from 'primevue/tablist'
import Tab from 'primevue/tab'
import TabPanels from 'primevue/tabpanels'
import TabPanel from 'primevue/tabpanel'
import Menu from '../node-menu/index.vue'
import ToolPicker from './ToolPicker.vue'
import { WorkflowType } from '../data'
const visible = ref<boolean>(false)
// 主画布的锚点事件不带 workflowType(只有 SubCanvas 会补),
// 回退到画布注入的类型,否则处理器画布会错误展示应用菜单
const injectedWorkflowType = inject<string>('WorkflowType')

const setting = ref<any>()
const selected = (node: any) => {
  setting.value.call(node, setting.value.anchorData).then(() => {
    close()
  })
}

const currentWorkflowType = computed(() => {
  return setting.value?.workflowType || injectedWorkflowType || WorkflowType.APPLICATION
})

// 工具 tab 仅在支持 run-tool 的工作流展示(工具工作流自身不调用工具)
const showToolTab = computed(
  () => ![WorkflowType.TOOL, WorkflowType.TOOL_LOOP].includes(currentWorkflowType.value as WorkflowType)
)

const open = (_setting: any) => {
  visible.value = true
  setting.value = _setting
}
const close = () => {
  visible.value = false
  setting.value = undefined
}

defineExpose({
  open,
  close
})
</script>
<style lang="scss"></style>
