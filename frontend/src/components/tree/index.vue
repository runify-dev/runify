<template>
  <div style="height: calc(100vh - 16px)">
    <div>
      <div
        class="knowledge-menu"
        @click="config.go('star')"
        :class="currentId == 'star' ? 'is_current' : ''"
      >
        <AppIcon name="app-collect"></AppIcon>
        收藏
      </div>
      <div
        class="knowledge-menu"
        @click="config.go('share')"
        :class="currentId == 'share' ? 'is_current' : ''"
      >
        <AppIcon name="app-share"></AppIcon>
        分享
      </div>
    </div>
    <el-divider />
    <div
      class="group knowledge-menu flex items-center"
      :class="currentId == 'root' ? 'is_current' : ''"
      @click="config.go('root')"
    >
      <div>全部</div>
      <div class="flex-auto"></div>
      <div class="group-hover:block hidden">
        <div class="grid place-items-center">
          <el-dropdown trigger="click">
            <el-icon>
              <More />
            </el-icon>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item
                  v-for="p in rootProcessor"
                  command="a"
                  @click="p.execute({ data: { id: 'root' } })"
                  :key="p.label"
                  >{{ p.label }}</el-dropdown-item
                >
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>
    </div>
    <div style="overflow-y: auto; height: calc(100vh - 140px)">
      <el-tree
        ref="treeRef"
        :highlight-current="true"
        @node-click="(node: any) => select(node.id, node)"
        :current-node-key="
          ['root', 'shar', 'share'].includes(currentId ? currentId : '') ? undefined : currentId
        "
        :default-expanded-keys="currentId ? [currentId] : []"
        :data="data"
        node-key="id"
        :props="propsConf"
      >
        <template v-slot="node">
          <NodeVue :data="node.data" :node="node.node" :config="config"> </NodeVue>
        </template>
      </el-tree>
    </div>
  </div>
</template>
<script setup lang="ts">
import { type Tree } from '@/api/type/node'
import NodeVue from '@/components/tree/node/index.vue'
import { computed, ref } from 'vue'
import { ElTree } from 'element-plus'
import AppIcon from '@/components/icons/AppIcon.vue'
import { Config } from '@/components/tree/index'
const treeRef = ref<InstanceType<typeof ElTree>>()
const props = withDefaults(
  defineProps<{
    config: Config
    currentId?: string
    data: Array<Tree>
    propsConf?: any
  }>(),
  {
    propsConf: {
      value: 'id',
      label: 'name',
      source: 'source',
      children: 'children',
      type: 'type',
      parent_id: 'parentId',
      meta: 'meta'
    }
  }
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
