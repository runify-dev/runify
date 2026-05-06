<template>
  <div
    class="custom-node-wrap custom-node-wrap-start"
    :class="model.isSelected ? 'p-node-selected' : ''"
  >
    <div class="title-box">
      <div class="title-left">
        <component :is="iconComponent(model.type)"></component>
        <div class="node-label pl-2">{{ model.properties.name }}</div>
      </div>
      <slot name="header"></slot>
      <DropdownMenu
        :items="[
          {
            label: '设置',
            value: 'setting',
            command: () => {
              openContent()
            }
          },
          {
            label: '重命名',
            value: 'rename',
            command: () => {
              model.graphModel.eventCenter.emit('runify:node:open-rename-dialog', reName)
            }
          },
          {
            label: '删除',
            value: 'delete',
            command: () => {
              model.graphModel.deleteNode(model.id)
            }
          }
        ]"
      >
        <template #default>
          <i class="pi pi-ellipsis-h"></i>
        </template>
      </DropdownMenu>
    </div>
    <NodeContentContainer :validate="validate" :submit="submit" ref="nodeContentContainerRef">
      <template #content v-if="$slots.default">
        <slot></slot>
      </template>
    </NodeContentContainer>
    <slot name="content"></slot>
  </div>
</template>
<script setup lang="ts">
import { BaseNodeModel } from '@logicflow/core'
import { inject, ref } from 'vue'
import NodeContentContainer from '@/workflow/common/NodeContentContainer.vue'
import DropdownMenu from '@/components/dropdown-menu/index.vue'
import { iconComponent } from '../icons'
const getModel = inject('getModel') as () => BaseNodeModel
const model = getModel()
const reName = (name: string) => {
  const nodes = model.graphModel.nodes
  if (
    nodes
      .filter((node: any) => node.id !== model.id)
      .some((node: any) => node.properties.name.trim() === name.trim())
  ) {
    return Promise.reject({ message: '节点名称已存在' })
  }
  model.properties.name = name
  return Promise.resolve({ data: '修改成功' })
}
defineProps<{
  validate: () => Promise<any>
  submit: () => Promise<any>
}>()

const nodeContentContainerRef = ref<InstanceType<typeof NodeContentContainer>>()
const openContent = () => {
  nodeContentContainerRef.value?.open(model)
}
defineExpose({ openContent })
</script>
<style lang="scss" scope>
// 定义颜色变量
$border-color: #dddfe6;
$text-primary: var(--p-content-color);
$text-secondary: #7e8393;
$text-icon: #5d6276;
$bg-hover: #f0f3f8;
$bg-light: #f5f7fb;
$accent-color: #f9a11d;
$accent-bg: rgba(249, 161, 29, 0.1);
$white: #fff;
.custom-node-wrap {
  box-sizing: border-box;
  width: 190px;
  min-height: 84px;
  padding: 0 12px;
  background: $white;
  border: 1px solid $border-color;
  border-radius: 8px;
  cursor: pointer;
  opacity: 1;

  &-start {
    width: 100%;
    height: 100%;
    min-height: 44px;
    background-image: linear-gradient(180deg, #e3ffef 0%, $white 50%);
  }
}

.title-box {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 10px 0 8px;
  color: $text-primary;
  font-weight: 500;
  font-size: 14px;
  line-height: 20px;
  letter-spacing: 0;

  .title-left {
    display: flex;
    align-items: center;
    min-width: 0;
    flex: 1;

    .node-label {
      flex: 1;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }
  }

  .node-icon {
    box-sizing: border-box;
    width: 20px;
    height: 20px;
    margin-right: 8px;
    padding: 3px;
    border-radius: 4px;
  }

  .more-box {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 16px;
    height: 16px;
    border-radius: 2px;
    cursor: pointer;

    &:hover {
      background: $bg-hover;
    }
  }

  .node-more-icon {
    color: $text-icon;
    font-size: 12px;
  }
}

.lf-node-selected {
  .custom-node-wrap {
    border: 1px solid var(--primary-color);
  }
}
</style>
