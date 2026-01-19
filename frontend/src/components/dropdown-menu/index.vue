<template>
  <div>
    <div @click.stop="toggle">
      <slot></slot>
    </div>

    <TieredMenu ref="menu" :model="items" popup @click.stop>
      <template v-if="$slots.item" #item="scope">
        <slot name="item" v-bind="scope"></slot>
      </template>
      <template v-if="$slots.submenuicon" #submenuicon="scope">
        <slot name="submenuicon" v-bind="scope"></slot>
      </template>
      <template v-if="$slots.itemicon" #itemicon="scope">
        <slot name="itemicon" v-bind="scope"></slot>
      </template>
    </TieredMenu>
  </div>
</template>
<script setup lang="ts">
import { ref } from 'vue'

const menu = ref()
defineProps<{
  items: Array<any>
}>()
const toggle = (event: any) => {
  menu.value.toggle(event)
}
</script>
<style lang="scss"></style>
