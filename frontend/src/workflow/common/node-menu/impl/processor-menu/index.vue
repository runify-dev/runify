<template>
  <Accordion :value="['0', '1', '2', '3', '4']" multiple>
    <AccordionPanel v-for="group in groups" :key="group.value" :value="group.value">
      <AccordionHeader>
        <div class="flex items-center gap-2">
          <i :class="group.icon" class="text-sm"></i>
          <span class="text-sm font-medium">{{ group.label }}</span>
        </div>
      </AccordionHeader>
      <AccordionContent>
        <div class="grid grid-cols-2 gap-1">
          <div
            v-for="node in group.nodes"
            :key="node.type"
            class="flex items-center gap-2 px-2 py-1.5 rounded cursor-pointer hover:bg-surface-hover transition-colors"
            @click="emit('selected', node)"
          >
            <component :is="iconComponent(node.type)" />
            <span class="text-sm">{{ node.properties.name }}</span>
          </div>
        </div>
      </AccordionContent>
    </AccordionPanel>
  </Accordion>
</template>
<script setup lang="ts">
import { iconComponent } from '@/workflow/icons'
import { WorkflowType } from '@/workflow/common/data'
import { NODE_MENU_GROUPS } from '@/workflow/common/node-group'

const groups = NODE_MENU_GROUPS[WorkflowType.PROCESSOR]

const emit = defineEmits(['selected'])
</script>
<style lang="scss"></style>
