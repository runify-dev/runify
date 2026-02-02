<template>
  <Panel header="基本节点">
    <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
      <div class="space-y-1" v-for="node in baseNodes" :key="node.type">
        <Card @click="emit('selected', node)">
          <template #content>
            <div class="flex items-center">
              <component :is="iconComponent(node.type)"></component>
              <p class="m-0 pl-4">
                {{ node.properties.name }}
              </p>
            </div>
          </template>
        </Card>
      </div>
    </div>
  </Panel>
</template>
<script setup lang="ts">
import { iconComponent } from '@/workflow/icons'
import {
  aiChatNode,
  javaScriptNode,
  databaseSearchNode,
  jsonResponseNode
} from '@/workflow/common/data'
const baseNodes = [aiChatNode, javaScriptNode, databaseSearchNode, jsonResponseNode]
const emit = defineEmits(['selected'])
</script>
<style lang="scss"></style>
