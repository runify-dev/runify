<template>
  <div style="height: 100%">
    <div class="h-full">
      <Tree
        :filter="true"
        :value="data"
        class="w-full"
        selectionMode="single"
        :pt="{
          root: {
            style: { padding: '16 0' }
          },
          nodeLabel: {
            style: { width: '100%' }
          }
        }"
      >
        <template #nodeicon="scope">
          <i class="pi pi-folder" v-if="scope.node.type == 'folder'"></i>
        </template>
        <template #header>
          <div class="p-tree-node">
            <div class="p-tree-node-content p-tree-node-selectable">
              <div class="p-tree-node-label w-full">
                <div class="flex items-center justify-between w-full group">
                  <span>全部</span>
                  <div class="action-buttons">
                    <Button
                      v-tooltip="'编辑'"
                      icon="pi pi-ellipsis-v"
                      variant="text"
                      aria-label="Filter"
                      severity="secondary"
                      size="small"
                    />
                  </div>
                </div>
              </div>
            </div>
          </div>
        </template>
        <template #default="{ node }">
          <div class="flex items-center justify-between w-full group">
            <span>{{ node.label }}</span>
            <div class="action-buttons">
              <Button
                v-tooltip="'编辑'"
                icon="pi pi-ellipsis-v"
                variant="text"
                aria-label="Filter"
                severity="secondary"
                size="small"
              />
            </div>
          </div>
        </template>
      </Tree>
    </div>
  </div>
</template>
<script setup lang="ts">
import NodeVue from '@/components/tree/node/index.vue'
import { computed, ref } from 'vue'
import { ElTree } from 'element-plus'
import AppIcon from '@/components/icons/AppIcon.vue'
import { Config } from '@/components/tree/index'
import Tree from 'primevue/tree'
import { PrimeIcons } from '@primevue/core/api'
import { type TreeNode } from 'primevue/treenode'
import type { style } from '@logicflow/extension/lib/bpmn-elements/presets/icons'
const treeRef = ref<InstanceType<typeof ElTree>>()
const props = withDefaults(
  defineProps<{
    config: Config
    currentId?: string
    data: Array<any>
  }>(),
  {}
)
const emit = defineEmits(['update:currentId'])
const select = (id: string, node?: any) => {
  emit('update:currentId', id, node)
}
const rootProcessor = computed(() => {
  return props.config.getProcessor('ROOT')
})
</script>
<style lang="scss" scoped>
ui {
  list-style: none;
  display: inline-block;
}

.is_current {
  background-color: var(--el-color-primary-light-9);
}

.knowledge-menu {
  height: var(--el-tree-node-content-height, 26px);

  &:hover {
    cursor: pointer;
    background-color: var(--el-fill-color-light);
  }
}

:deep.el-divider--horizontal {
  margin: 8px 0;
}
</style>
